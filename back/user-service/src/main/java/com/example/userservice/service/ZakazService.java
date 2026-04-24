package com.example.userservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.userservice.response.ZakazResponse;

@Service
public class ZakazService {

    public List<ZakazResponse> getAllZakazByUserId(Long userId) {
        throw new UnsupportedOperationException("Zakaz service not yet implemented in user-service");
    }
}
