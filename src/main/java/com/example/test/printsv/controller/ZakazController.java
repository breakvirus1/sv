package com.example.test.printsv.controller;

import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;
import com.example.test.printsv.service.UserService;
import com.example.test.printsv.service.ZakazService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/user/{userId}/zakaz")
public class ZakazController {

    private final ZakazService zakazService;
    private final UserService userService;

    @Operation(summary = "Create a new order", description = "Creates a new order for the specified user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "User or customer not found")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZakazResponse createZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order request data") @RequestBody @Valid ZakazRequest zakazRequest) {
        return zakazService.createZakaz(userId, zakazRequest);
    }

    @Operation(summary = "Get all orders for a user", description = "Retrieves all orders for the specified user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user")
    })
    @GetMapping
    public List<ZakazResponse> getAllZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId) {
        return zakazService.getAllZakazByUserId(userId);
    }

    @Operation(summary = "Get order by ID", description = "Retrieves a specific order by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ZakazResponse getZakazById(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long id) {
        return zakazService.getZakazById(userId, id);
    }

    @Operation(summary = "Get orders by customer name", description = "Retrieves orders for a user by customer name")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user")
    })
    @GetMapping("/customer/{customerName}")
    public List<ZakazResponse> getZakazByCustomerName(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Customer name") @PathVariable String customerName) {
        return zakazService.getAllZakazByCustomerName(userId, customerName);
    }

    @Operation(summary = "Update an order", description = "Updates an existing order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Order updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Order or customer not found")
    })
    @PutMapping("/{id}")
    public ZakazResponse updateZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long id,
            @Parameter(description = "Updated order data") @RequestBody @Valid ZakazRequest zakazRequest) {
        return zakazService.updateZakaz(userId, id, zakazRequest);
    }

    @Operation(summary = "Delete an order", description = "Deletes an existing order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long id) {
        zakazService.deleteZakaz(userId, id);
    }
}