package com.example.orderservice.service;

import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.product.Product;
import com.example.orderservice.product.ProductMaterial;
import com.example.orderservice.product.ProductOperation;
import com.example.orderservice.product.repository.ProductRepository;
import com.example.orderservice.product.dto.ProductDTO;
import com.example.orderservice.product.dto.ProductMaterialDTO;
import com.example.orderservice.product.dto.ProductOperationDTO;
import com.example.orderservice.product.dto.ProductPositionDTO;
import com.example.orderservice.product.mapper.ProductMapper;
import com.example.materialservice.entity.Material;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductDTO create(ProductDTO dto, Long companyId) {
        Product product = new Product();
        applyDtoToProduct(product, dto);
        product.setCompanyId(companyId);
        product = productRepository.save(product);
        return toDtoWithPositions(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        applyDtoToProduct(product, dto);
        return toDtoWithPositions(product);
    }

    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
    }

    public ProductDTO getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return toDtoWithPositions(product);
    }

    public List<ProductDTO> getAll(Long companyId) {
        return productRepository.findAll().stream()
                .peek(p -> { if (p.getMaterials() != null) p.getMaterials().size(); if (p.getOperations() != null) p.getOperations().size(); })
                .filter(p -> companyId == null || p.getCompanyId() == null
                        || companyId.equals(p.getCompanyId()))
                .map(this::toDtoWithPositions)
                .collect(java.util.stream.Collectors.toList());
    }

    public ProductDTO calculateEstimate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        ProductDTO dto = toDtoWithPositions(product);
        dto.setBasePrice(dto.getTotalPrice());
        return dto;
    }

    private ProductDTO toDtoWithPositions(Product product) {
        ProductDTO dto = productMapper.toDto(product);
        return enrichWithPositions(product, dto);
    }

    /**
     * Строит позиции изделия (материал + его операции) и единственную сумму каждой позиции.
     * Общая стоимость изделия — сумма всех позиций.
     */
    private ProductDTO enrichWithPositions(Product product, ProductDTO dto) {
        List<ProductPositionDTO> positions = new ArrayList<>();
        BigDecimal productTotal = BigDecimal.ZERO;
        List<ProductOperation> unlinkedOperations = new ArrayList<>();

        if (product.getOperations() != null) {
            for (ProductOperation po : product.getOperations()) {
                if (po.getMaterialPosition() == null) {
                    unlinkedOperations.add(po);
                }
            }
        }

        if (product.getMaterials() != null) {
            for (ProductMaterial pm : product.getMaterials()) {
                Material material = pm.getMaterial();
                ProductPositionDTO position = new ProductPositionDTO();
                position.setId(pm.getId());
                if (material != null) {
                    position.setMaterialId(material.getId());
                    position.setMaterialName(material.getName());
                    position.setUnit(material.getUnit());
                    position.setPrice(material.getPrice());
                }
                position.setQuantity(pm.getQuantity());
                position.setWasteCoefficient(pm.getWasteCoefficient());
                position.setQuantityFormula(pm.getQuantityFormula());
                position.setSortOrder(pm.getSortOrder());

                BigDecimal positionSum = materialCost(pm);
                List<ProductOperationDTO> operations = new ArrayList<>();
                if (product.getOperations() != null) {
                    for (ProductOperation po : product.getOperations()) {
                        if (belongsToPosition(po, pm)) {
                            operations.add(productMapper.toOperationDto(po));
                            positionSum = positionSum.add(operationCost(po));
                        }
                    }
                }
                position.setOperations(operations);
                position.setPositionSum(positionSum.setScale(2, RoundingMode.HALF_UP));
                positions.add(position);
                productTotal = productTotal.add(positionSum);
            }
        }

        // Совместимость со старыми изделиями: операции без привязки к позиции
        // относим к первой позиции, чтобы общая сумма осталась прежней.
        if (!unlinkedOperations.isEmpty()) {
            if (positions.isEmpty()) {
                ProductPositionDTO legacy = new ProductPositionDTO();
                legacy.setMaterialName("Работы");
                legacy.setOperations(new ArrayList<>());
                positions.add(legacy);
            }
            ProductPositionDTO target = positions.get(0);
            BigDecimal positionSum = target.getPositionSum() != null ? target.getPositionSum() : BigDecimal.ZERO;
            if (target.getOperations() == null) {
                target.setOperations(new ArrayList<>());
            }
            for (ProductOperation po : unlinkedOperations) {
                target.getOperations().add(productMapper.toOperationDto(po));
                positionSum = positionSum.add(operationCost(po));
                productTotal = productTotal.add(operationCost(po));
            }
            target.setPositionSum(positionSum.setScale(2, RoundingMode.HALF_UP));
        }

        dto.setPositions(positions);
        dto.setTotalPrice(productTotal.setScale(2, RoundingMode.HALF_UP));
        return dto;
    }

    private boolean belongsToPosition(ProductOperation operation, ProductMaterial position) {
        return operation.getMaterialPosition() != null
                && position.getId() != null
                && position.getId().equals(operation.getMaterialPosition().getId());
    }

    private BigDecimal materialCost(ProductMaterial pm) {
        BigDecimal price = pm.getMaterial() != null && pm.getMaterial().getPrice() != null
                ? pm.getMaterial().getPrice() : BigDecimal.ZERO;
        BigDecimal quantity = pm.getQuantity() != null ? pm.getQuantity() : BigDecimal.ONE;
        BigDecimal coefficient = pm.getWasteCoefficient() != null ? pm.getWasteCoefficient() : BigDecimal.ONE;
        return price.multiply(quantity).multiply(coefficient);
    }

    private BigDecimal operationCost(ProductOperation po) {
        BigDecimal price = po.getPricePerUnit() != null ? po.getPricePerUnit() : BigDecimal.ZERO;
        BigDecimal quantity = po.getQuantity() != null ? po.getQuantity() : BigDecimal.ONE;
        BigDecimal coefficient = po.getCoefficient() != null ? po.getCoefficient() : BigDecimal.ONE;
        return price.multiply(quantity).multiply(coefficient);
    }

    private void applyDtoToProduct(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setArticle(dto.getArticle());
        product.setDescription(dto.getDescription());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setUnit(dto.getUnit() != null ? dto.getUnit() : "шт");
        product.setBasePrice(dto.getBasePrice());
        product.setCategory(dto.getCategory());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        product.setFormulaJson(dto.getFormulaJson());

        if (product.getMaterials() != null) {
            product.getMaterials().clear();
        } else {
            product.setMaterials(new ArrayList<>());
        }
        if (product.getOperations() != null) {
            product.getOperations().clear();
        } else {
            product.setOperations(new ArrayList<>());
        }

        if (dto.getPositions() != null) {
            applyPositions(product, dto.getPositions());
        } else {
            applyFlatMaterialsAndOperations(product, dto);
        }
    }

    private void applyPositions(Product product, List<ProductPositionDTO> positions) {
        int positionIndex = 0;
        for (ProductPositionDTO positionDto : positions) {
            if (positionDto.getMaterialId() == null) {
                continue;
            }
            Material material = entityManager.find(Material.class, positionDto.getMaterialId());
            if (material == null) {
                throw new NotFoundException("Material not found: " + positionDto.getMaterialId());
            }
            ProductMaterial pm = new ProductMaterial();
            pm.setProduct(product);
            pm.setMaterial(material);
            pm.setQuantity(positionDto.getQuantity() != null ? positionDto.getQuantity() : BigDecimal.ONE);
            pm.setWasteCoefficient(positionDto.getWasteCoefficient() != null ? positionDto.getWasteCoefficient() : BigDecimal.ONE);
            pm.setSortOrder(positionDto.getSortOrder() != null ? positionDto.getSortOrder() : positionIndex);
            pm.setQuantityFormula(positionDto.getQuantityFormula());
            product.getMaterials().add(pm);

            if (positionDto.getOperations() != null) {
                int operationIndex = 0;
                for (ProductOperationDTO opDto : positionDto.getOperations()) {
                    ProductOperation op = buildOperation(opDto);
                    op.setProduct(product);
                    op.setMaterialPosition(pm);
                    op.setSortOrder(opDto.getSortOrder() != null ? opDto.getSortOrder() : operationIndex);
                    product.getOperations().add(op);
                    operationIndex++;
                }
            }
            positionIndex++;
        }
    }

    private void applyFlatMaterialsAndOperations(Product product, ProductDTO dto) {
        if (dto.getMaterials() != null) {
            for (ProductMaterialDTO pmDto : dto.getMaterials()) {
                Material material = entityManager.find(Material.class, pmDto.getMaterialId());
                if (material == null) {
                    throw new NotFoundException("Material not found: " + pmDto.getMaterialId());
                }
                ProductMaterial pm = new ProductMaterial();
                pm.setProduct(product);
                pm.setMaterial(material);
                pm.setQuantity(pmDto.getQuantity());
                pm.setWasteCoefficient(pmDto.getWasteCoefficient() != null ? pmDto.getWasteCoefficient() : BigDecimal.ONE);
                pm.setSortOrder(pmDto.getSortOrder());
                pm.setQuantityFormula(pmDto.getQuantityFormula());
                product.getMaterials().add(pm);
            }
        }

        if (dto.getOperations() != null) {
            for (ProductOperationDTO opDto : dto.getOperations()) {
                ProductOperation op = buildOperation(opDto);
                op.setProduct(product);
                product.getOperations().add(op);
            }
        }
    }

    private ProductOperation buildOperation(ProductOperationDTO opDto) {
        ProductOperation op = new ProductOperation();
        op.setName(opDto.getName());
        op.setPricePerUnit(opDto.getPricePerUnit());
        if (opDto.getNormTime() != null && !opDto.getNormTime().isEmpty()) {
            try {
                op.setNormTime(Duration.parse(opDto.getNormTime()));
            } catch (Exception e) {
                op.setNormTime(null);
            }
        }
        op.setUnit(opDto.getUnit() != null ? opDto.getUnit() : "шт");
        op.setSortOrder(opDto.getSortOrder());
        op.setQuantityFormula(opDto.getQuantityFormula());
        op.setQuantity(opDto.getQuantity() != null ? opDto.getQuantity() : BigDecimal.ONE);
        op.setCoefficient(opDto.getCoefficient() != null ? opDto.getCoefficient() : BigDecimal.ONE);
        return op;
    }
}
