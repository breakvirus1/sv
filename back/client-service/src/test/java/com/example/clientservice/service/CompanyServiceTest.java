package com.example.clientservice.service;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.entity.Company;
import com.example.clientservice.exception.NotFoundException;
import com.example.clientservice.mapper.CompanyMapper;
import com.example.clientservice.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private CompanyDefaultsSeeder companyDefaultsSeeder;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("Пользователь другой компании не может получить компанию по ID")
    void cannotGetForeignCompany() {
        assertThatThrownBy(() -> companyService.getCompanyById(1L, 2L))
                .isInstanceOf(NotFoundException.class);

        verify(companyRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("Пользователь своей компании получает компанию по ID")
    void canGetOwnCompany() {
        Company company = new Company();
        company.setId(1L);
        CompanyResponse response = new CompanyResponse();
        response.setId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyMapper.toDto(company)).thenReturn(response);

        assertThat(companyService.getCompanyById(1L, 1L).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GOD без company_id может получить любую компанию")
    void godCanGetAnyCompany() {
        Company company = new Company();
        company.setId(1L);
        CompanyResponse response = new CompanyResponse();
        response.setId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyMapper.toDto(company)).thenReturn(response);

        assertThat(companyService.getCompanyById(1L, null).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Пользователь другой компании не может обновить компанию")
    void cannotUpdateForeignCompany() {
        assertThatThrownBy(() -> companyService.updateCompany(1L, new CompanyCreateRequest("Новая"), 2L))
                .isInstanceOf(NotFoundException.class);

        verify(companyRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("Пользователь другой компании не может удалить компанию")
    void cannotDeleteForeignCompany() {
        assertThatThrownBy(() -> companyService.deleteCompany(1L, 2L))
                .isInstanceOf(NotFoundException.class);

        verify(companyRepository, never()).delete(org.mockito.ArgumentMatchers.any(Company.class));
    }
}
