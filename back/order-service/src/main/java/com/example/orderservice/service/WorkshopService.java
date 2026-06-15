package com.example.orderservice.service;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Workshop;
import com.example.orderservice.mapper.WorkshopMapper;
import com.example.orderservice.repository.WorkshopRepository;
import com.example.orderservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final WorkshopMapper workshopMapper;

    @Transactional(readOnly = true)
    public Page<WorkshopResponse> getAllWorkshops(Specification<Workshop> spec, Pageable pageable) {
        return workshopRepository.findAll(spec, pageable)
                .map(workshopMapper::toDto);
    }

    @Transactional(readOnly = true)
    public WorkshopResponse getWorkshopById(Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Цех не найден"));
        return workshopMapper.toDto(workshop);
    }

    public WorkshopResponse createWorkshop(WorkshopCreateRequest request) {
        Workshop workshop = workshopMapper.toEntity(request);
        Workshop saved = workshopRepository.save(workshop);
        return workshopMapper.toDto(saved);
    }

    public WorkshopResponse updateWorkshop(Long id, WorkshopUpdateRequest request) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Цех не найден"));
        workshopMapper.updateEntityFromRequest(request, workshop);
        Workshop saved = workshopRepository.save(workshop);
        return workshopMapper.toDto(saved);
    }

    public void deleteWorkshop(Long id) {
        Workshop workshop = workshopRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Цех не найден"));
        workshop.setDeleted(true);
        workshopRepository.save(workshop);
    }
}
