package com.example.clientservice.service;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.entity.Company;
import com.example.clientservice.exception.ConflictException;
import com.example.clientservice.exception.NotFoundException;
import com.example.clientservice.mapper.CompanyMapper;
import com.example.clientservice.repository.CompanyRepository;
import com.example.printsv.common.tenant.DatabaseProvisioningService;
import com.example.printsv.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final CompanyDefaultsSeeder companyDefaultsSeeder;
    private final DatabaseProvisioningService databaseProvisioningService;

    @Cacheable(value = "companies", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#spec?.toString() ?: 'all') + '_' + (#companyId ?: 'all')")
    public Page<CompanyResponse> getAllCompanies(Specification<Company> spec, Pageable pageable, Long companyId) {
        Specification<Company> finalSpec = spec;
        if (companyId != null) {
            finalSpec = finalSpec == null ? Specification.where(null) : finalSpec;
            finalSpec = finalSpec.and((root, query, cb) -> cb.equal(root.get("id"), companyId));
        }
        return companyRepository.findAll(finalSpec, pageable)
                .map(companyMapper::toDto);
    }

    @Cacheable(value = "companies", key = "'id_' + #id")
    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id, Long requesterCompanyId) {
        verifyCompanyAccess(id, requesterCompanyId);
        Company company = getCompanyEntity(id);
        return companyMapper.toDto(company);
    }

    @CacheEvict(value = "companies", allEntries = true)
    public CompanyResponse createCompany(CompanyCreateRequest request) {
        if (companyRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("Компания с таким названием уже существует");
        }
        Company company = companyMapper.createFromRequest(request);
        Company saved = companyRepository.save(company);
        Long companyId = saved.getId();
        // Фиксируем запись о компании в мастер-БД до создания базы компании
        companyRepository.flush();

        // Создаём базу данных для компании
        String databaseName = "sv_company_" + companyId;
        try {
            databaseProvisioningService.createDatabaseForCompany(companyId, databaseName);
            log.info("Created database {} for company {}", databaseName, companyId);
        } catch (Exception e) {
            log.error("Failed to create database for company {}", companyId, e);
            throw new RuntimeException("Не удалось создать базу данных для компании: " + e.getMessage(), e);
        }

        // Справочники пишем уже в базу новой компании
        TenantContext.runAs(companyId, () -> companyDefaultsSeeder.seedDefaultsForCompany(companyId));
        return companyMapper.toDto(saved);
    }

    @CacheEvict(value = "companies", allEntries = true)
    public CompanyResponse updateCompany(Long id, CompanyCreateRequest request, Long requesterCompanyId) {
        verifyCompanyAccess(id, requesterCompanyId);
        Company company = getCompanyEntity(id);
        companyMapper.updateFromRequest(request, company);
        Company saved = companyRepository.save(company);
        return companyMapper.toDto(saved);
    }

    @CacheEvict(value = "companies", allEntries = true)
    public void deleteCompany(Long id, Long requesterCompanyId) {
        verifyCompanyAccess(id, requesterCompanyId);
        Company company = getCompanyEntity(id);
        companyRepository.delete(company);
    }

    private void verifyCompanyAccess(Long requestedCompanyId, Long requesterCompanyId) {
        if (requesterCompanyId != null && !requesterCompanyId.equals(requestedCompanyId)) {
            throw new NotFoundException("Компания не найдена");
        }
    }

    @Transactional(readOnly = true)
    private Company getCompanyEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Компания не найдена"));
    }
}
