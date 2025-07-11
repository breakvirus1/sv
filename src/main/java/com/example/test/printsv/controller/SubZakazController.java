package com.example.test.printsv.controller;

import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;
import com.example.test.printsv.service.ZakazService;
import com.example.test.printsv.service.UserService;
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
@RequestMapping("/api/user/{userId}/zakaz/{zakazId}/subzakaz")
public class SubZakazController {

    private final ZakazService zakazService;
    private final UserService userService;

    @Operation(summary = "Create a new sub-order", description = "Creates a new sub-order for a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Sub-order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Order or material not found")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubZakazResponse createSubZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long zakazId,
            @Parameter(description = "Sub-order request data") @RequestBody @Valid SubZakazRequest subZakazRequest) {
        return zakazService.createSubZakaz(userId, zakazId, subZakazRequest);
    }

    @Operation(summary = "Get all sub-orders for an order", description = "Retrieves all sub-orders for a specific order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sub-orders retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping
    public List<SubZakazResponse> getAllSubZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long zakazId) {
        return zakazService.getAllSubZakazByZakazId(userId, zakazId);
    }

    @Operation(summary = "Get sub-order by ID", description = "Retrieves a specific sub-order by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sub-order retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Sub-order or order not found")
    })
    @GetMapping("/{id}")
    public SubZakazResponse getSubZakazById(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long zakazId,
            @Parameter(description = "Sub-order ID") @PathVariable @Positive Long id) {
        return zakazService.getSubZakazById(userId, zakazId, id);
    }

    @Operation(summary = "Update a sub-order", description = "Updates an existing sub-order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Sub-order updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Sub-order or order not found")
    })
    @PutMapping("/{id}")
    public SubZakazResponse updateSubZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long zakazId,
            @Parameter(description = "Sub-order ID") @PathVariable @Positive Long id,
            @Parameter(description = "Updated sub-order data") @RequestBody @Valid SubZakazRequest subZakazRequest) {
        return zakazService.updateSubZakaz(userId, zakazId, id, subZakazRequest);
    }

    @Operation(summary = "Delete a sub-order", description = "Deletes an existing sub-order")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Sub-order deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "User ID does not match authenticated user"),
        @ApiResponse(responseCode = "404", description = "Sub-order or order not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubZakaz(
            @Parameter(description = "User ID") @PathVariable @Positive Long userId,
            @Parameter(description = "Order ID") @PathVariable @Positive Long zakazId,
            @Parameter(description = "Sub-order ID") @PathVariable @Positive Long id) {
        zakazService.deleteSubZakaz(userId, zakazId, id);
    }
}