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
        product.setName(dto.getName());
        product.setArticle(dto.getArticle());
        product.setDescription(dto.getDescription());
        product.setWidth(dto.getWidth());
        product.setHeight(dto.getHeight());
        product.setUnit(dto.getUnit() != null ? dto.getUnit() : "шт");
        product.setBasePrice(dto.getBasePrice());

        // Materials
        if (dto.getMaterials() != null) {
            List<ProductMaterial> materialEntities = new ArrayList<>();
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
                materialEntities.add(pm);
            }
            product.setMaterials(materialEntities);
        }

        // Operations
        if (dto.getOperations() != null) {
            List<ProductOperation> opEntities = new ArrayList<>();
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
                opEntities.add(op);
            }
            product.setOperations(opEntities);
        }

        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    public ProductDTO getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return productMapper.toDto(product);
    }

    public List<ProductDTO> getAll() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .collect(java.util.stream.Collectors.toList());
    }
}
