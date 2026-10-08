package com.example.clientservice.service;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.entity.Company;
import com.example.clientservice.mapper.CompanyMapper;
import com.example.clientservice.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public Page<CompanyResponse> getAllCompanies(Specification<Company> spec, Pageable pageable) {
        return companyRepository.findAll(spec, pageable)
                .map(companyMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Компания не найдена"));
        return companyMapper.toDto(company);
    }

    public CompanyResponse createCompany(CompanyCreateRequest request) {
        if (companyRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Компания с таким названием уже существует");
        }
        Company company = companyMapper.createFromRequest(request);
        Company saved = companyRepository.save(company);
        return companyMapper.toDto(saved);
    }

    public CompanyResponse updateCompany(Long id, CompanyCreateRequest request) {
        Company company = getCompanyEntity(id);
        companyMapper.updateFromRequest(request, company);
        Company saved = companyRepository.save(company);
        return companyMapper.toDto(saved);
    }

    public void deleteCompany(Long id) {
        Company company = getCompanyEntity(id);
        companyRepository.delete(company);
    }

    @Transactional(readOnly = true)
    private Company getCompanyEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Компания не найдена"));
    }
}
