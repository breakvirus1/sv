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
@RequestMapping("/api/zakaz/{zakazId}/subzakaz")
public class SubZakazController {

    private final ZakazService zakazService;
    private final UserService userService;

    @Operation(summary = "Создать новый подзаказ", description = "Создает новый подзаказ для указанного заказа")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Подзаказ успешно создан"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "ID пользователя не совпадает с авторизованным пользователем"),
        @ApiResponse(responseCode = "404", description = "Заказ или материал не найден")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubZakazResponse createSubZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long zakazId,
            @Parameter(description = "Данные запроса подзаказа") @RequestBody @Valid SubZakazRequest subZakazRequest) {
        return zakazService.createSubZakaz(userId, zakazId, subZakazRequest);
    }

    @Operation(summary = "Получить все подзаказы для заказа", description = "Извлекает все подзаказы для указанного заказа")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подзаказы успешно извлечены"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "ID пользователя не совпадает с авторизованным пользователем"),
        @ApiResponse(responseCode = "404", description = "Заказ не найден")
    })
    @GetMapping
    public List<SubZakazResponse> getAllSubZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long zakazId) {
        return zakazService.getAllSubZakazByZakazId(userId, zakazId);
    }

    @Operation(summary = "Получить подзаказ по ID", description = "Извлекает конкретный подзаказ по его ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подзаказ успешно извлечен"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "ID пользователя не совпадает с авторизованным пользователем"),
        @ApiResponse(responseCode = "404", description = "Подзаказ или заказ не найден")
    })
    @GetMapping("/{id}")
    public SubZakazResponse getSubZakazById(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long zakazId,
            @Parameter(description = "ID подзаказа") @PathVariable @Positive Long id) {
        return zakazService.getSubZakazById(userId, zakazId, id);
    }

    @Operation(summary = "Обновить подзаказ", description = "Обновляет существующий подзаказ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Подзаказ успешно обновлен"),
        @ApiResponse(responseCode = "400", description = "Неверные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "ID пользователя не совпадает с авторизованным пользователем"),
        @ApiResponse(responseCode = "404", description = "Подзаказ или заказ не найден")
    })
    @PutMapping("/{id}")
    public SubZakazResponse updateSubZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long zakazId,
            @Parameter(description = "ID подзаказа") @PathVariable @Positive Long id,
            @Parameter(description = "Обновленные данные подзаказа") @RequestBody @Valid SubZakazRequest subZakazRequest) {
        return zakazService.updateSubZakaz(userId, zakazId, id, subZakazRequest);
    }

    @Operation(summary = "Удалить подзаказ", description = "Удаляет существующий подзаказ")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Подзаказ успешно удален"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "ID пользователя не совпадает с авторизованным пользователем"),
        @ApiResponse(responseCode = "404", description = "Подзаказ или заказ не найден")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubZakaz(
            @Parameter(description = "ID пользователя") @PathVariable @Positive Long userId,
            @Parameter(description = "ID заказа") @PathVariable @Positive Long zakazId,
            @Parameter(description = "ID подзаказа") @PathVariable @Positive Long id) {
        zakazService.deleteSubZakaz(userId, zakazId, id);
    }
}