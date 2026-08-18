package com.example.commentservice.service;

import com.example.commentservice.entity.Image;
import com.example.commentservice.exception.FileStorageException;
import com.example.commentservice.exception.ResourceNotFoundException;
import com.example.commentservice.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    @Value("${app.images.dir:./images}")
    private String imagesDir;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image uploadImage(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "image";
            }

            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + (extension != null && !extension.isBlank() ? "." + extension : "");
            String contentType = file.getContentType();
            long size = file.getSize();

            Path dirPath = Paths.get(imagesDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path targetPath = dirPath.resolve(filename);
            file.transferTo(targetPath.toFile());

            Image image = new Image();
            image.setFilename(filename);
            image.setOriginalName(originalFilename);
            image.setContentType(contentType);
            image.setSize(size);
            image.setPath(targetPath.toString());

            return imageRepository.save(image);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось сохранить изображение: " + e.getMessage(), e);
        }
    }

    public List<Image> uploadImages(List<MultipartFile> files) {
        List<Image> images = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                images.add(uploadImage(file));
            }
        }
        return images;
    }

    public Image getImage(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Изображение не найдено: " + id));
    }

    public byte[] getImageBytes(Long id) {
        Image image = getImage(id);
        try {
            Path path = Paths.get(image.getPath());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось прочитать изображение: " + e.getMessage(), e);
        }
    }

    public void deleteImage(Long id) {
        Image image = getImage(id);
        try {
            Path path = Paths.get(image.getPath());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось удалить изображение: " + e.getMessage(), e);
        }
        imageRepository.delete(image);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }
}
