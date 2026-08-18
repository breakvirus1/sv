package com.example.commentservice.repository;

import com.example.commentservice.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByFilename(String filename);
    boolean existsByFilename(String filename);
}
