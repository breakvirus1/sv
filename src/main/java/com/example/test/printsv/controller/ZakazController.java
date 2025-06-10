package com.example.test.printsv.controller;



import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.ZakazService;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/zakaz")
public class ZakazController {
    private ZakazService zakazService;
    @PostMapping
    @Operation(summary = "Создание заказа")
            @ApiResponse(responseCode = "201", description = "Успех")
            @ApiResponse(responseCode = "400", description = "Неверно переданные данные")
            @ApiResponse(responseCode = "500", description = "Ошибка работы сервиса")
    public void createZakaz(@Parameter(description = "запрос на создание заказа") @RequestBody @Valid ZakazRequest ZakazRequest) {

        zakazService.createZakaz(ZakazRequest);
    }
    
    @GetMapping("/")
       
    public List<ZakazResponse> getAllZakaz(@RequestParam(value = "zakazpagelimit", defaultValue = "0", required = false) 
                                            @Positive Integer zakazpagelimit)   {
        return zakazService.getAllZakaz();

    }
    @GetMapping("/id")
    
    
    public ZakazResponse getZakazById(@PathVariable @PositiveOrZero Long id){

        return zakazService.getZakazById(id);

    }
    @GetMapping("/customerName")
    public List<ZakazResponse> getZakazByCustomerName(@PathVariable @PositiveOrZero String customerName){

        return zakazService.getAllZakazByCustomerName(customerName);
    }

    
    




    
}
