package com.example.test.printsv.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.UserService;
import com.example.test.printsv.service.ZakazService;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/zakaz")
public class ZakazController {

    private final ZakazService zakazService;
    private final ZakazRepository zakazRepository;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Zakaz create(@RequestBody Zakaz zakaz) {
        return zakazRepository.save(zakaz);
    }
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public void getMyZakazList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("getPrincipal: "+authentication.getPrincipal());
        System.out.println("getAuthorities: "+authentication.getAuthorities());
        System.out.println("getName: "+authentication.getName());
        System.out.println("getDetails: "+authentication.getDetails());

        // return zakazService.getAllZakazByUserName(null);
    }

    
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Zakaz> getAll(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return zakazRepository.findAll(pageable);
    }

@GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')") // Add check if owns or admin
    public Zakaz getById(@PathVariable Long id) {
        return zakazRepository.findById(id).orElseThrow();
    }

    
    @GetMapping("/customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public List<ZakazResponse> getZakazByCustomerName(
            
            @Parameter(description = "Имя клиента") @PathVariable String customerName) {
        return zakazService.getAllZakazByCustomerName(customerName);
    }

    
    @PutMapping("/{id}")
    public ZakazResponse updateZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long id,
            @Parameter(description = "Обновленные данные заказа") @RequestBody @Valid ZakazRequest zakazRequest) {
        return zakazService.updateZakaz(userId, id, zakazRequest);
    }

    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long id) {
        zakazService.deleteZakaz(userId, id);
    }

    @GetMapping("/sum")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER')")
    public ResponseEntity<Integer> getSumForPeriod(@RequestParam String startDate, @RequestParam String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Adjust format as needed
        LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", formatter);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        Integer sum = zakazRepository.findSumByUserAndPeriod(user, start, end);
        return ResponseEntity.ok(sum != null ? sum : 0);
    }
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public List<Zakaz> search(@RequestParam(required = false) Integer minSum) {
        if (minSum != null) {
            return zakazRepository.findBySumGreaterThan(minSum);
        }
        return zakazRepository.findAll();
    }
}