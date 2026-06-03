package com.example.fileservice.service;

import com.example.fileservice.entity.FileAttachment;
import com.example.fileservice.entity.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private final Path fileStorageLocation;

    public FileService(@Value("${file.storage.location:uploads}") String storageLocation) {
        this.fileStorageLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public FileAttachment storeFile(MultipartFile file, Long orderId, Long orderItemId, String uploadedBy) {
        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalName;
        String mimeType = file.getContentType();
        long fileSize = file.getSize();

        Path targetLocation = this.fileStorageLocation.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), targetLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }

        FileAttachment attachment = new FileAttachment();
        attachment.setFileName(fileName);
        attachment.setOriginalName(originalName);
        attachment.setFilePath(targetLocation.toString());
        attachment.setFileUrl("/api/files/download/" + fileName);
        attachment.setMimeType(mimeType);
        attachment.setFileSize(fileSize);
        attachment.setOrderId(orderId);
        attachment.setUploadedBy(uploadedBy);

        if (orderItemId != null) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(orderItemId);
            attachment.setOrderItem(orderItem);
        }

        return attachment;
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }
}
