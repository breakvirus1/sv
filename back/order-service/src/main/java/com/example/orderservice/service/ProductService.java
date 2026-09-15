package com.example.orderservice.service;

import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.product.Product;
import com.example.orderservice.product.ProductMaterial;
import com.example.orderservice.product.ProductOperation;
import com.example.orderservice.product.repository.ProductRepository;
import com.example.orderservice.product.dto.ProductDTO;
import com.example.orderservice.product.dto.ProductMaterialDTO;
import com.example.orderservice.product.dto.ProductOperationDTO;
import com.example.orderservice.product.mapper.ProductMapper;
import com.example.materialservice.entity.Material;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public ProductDTO create(ProductDTO dto) {
        Product product = new Product();
        applyDtoToProduct(product, dto);
        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    public ProductDTO update(Long id, ProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        applyDtoToProduct(product, dto);
        return productMapper.toDto(product);
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
        return productMapper.toDto(product);
    }

    public List<ProductDTO> getAll() {
        return productRepository.findAll().stream()
                .peek(p -> { if (p.getMaterials() != null) p.getMaterials().size(); if (p.getOperations() != null) p.getOperations().size(); })
                .map(productMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public ProductDTO calculateEstimate(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        BigDecimal materialTotal = BigDecimal.ZERO;
        if (product.getMaterials() != null) {
            for (ProductMaterial pm : product.getMaterials()) {
                Material material = pm.getMaterial();
                if (material == null) continue;
                BigDecimal qty = pm.getQuantity() != null ? pm.getQuantity() : BigDecimal.ONE;
                BigDecimal waste = pm.getWasteCoefficient() != null ? pm.getWasteCoefficient() : BigDecimal.ONE;
                BigDecimal total = qty.multiply(waste).multiply(material.getPrice());
                materialTotal = materialTotal.add(total);
            }
        }

        BigDecimal operationTotal = BigDecimal.ZERO;
        if (product.getOperations() != null) {
            for (ProductOperation po : product.getOperations()) {
                BigDecimal qty = po.getQuantity() != null ? po.getQuantity() : BigDecimal.ONE;
                BigDecimal coeff = po.getCoefficient() != null ? po.getCoefficient() : BigDecimal.ONE;
                BigDecimal total = po.getPricePerUnit().multiply(qty).multiply(coeff);
                operationTotal = operationTotal.add(total);
            }
        }

        BigDecimal grandTotal = materialTotal.add(operationTotal);

        ProductDTO dto = productMapper.toDto(product);
        dto.setBasePrice(grandTotal);
        return dto;
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

        if (product.getOperations() != null) {
            product.getOperations().clear();
        } else {
            product.setOperations(new ArrayList<>());
        }
        if (dto.getOperations() != null) {
            for (ProductOperationDTO opDto : dto.getOperations()) {
                ProductOperation op = new ProductOperation();
                op.setProduct(product);
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
                product.getOperations().add(op);
            }
        }
    }
}
