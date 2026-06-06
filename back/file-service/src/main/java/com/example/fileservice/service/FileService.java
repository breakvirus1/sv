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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {

    private final Path fileStorageLocation;

    private static final Map<Character, String> TRANSLIT_MAP = new HashMap<>();

    static {
        TRANSLIT_MAP.put('а', "a");   TRANSLIT_MAP.put('б', "b");   TRANSLIT_MAP.put('в', "v");
        TRANSLIT_MAP.put('г', "g");   TRANSLIT_MAP.put('д', "d");   TRANSLIT_MAP.put('е', "e");
        TRANSLIT_MAP.put('ё', "yo");  TRANSLIT_MAP.put('ж', "zh");  TRANSLIT_MAP.put('з', "z");
        TRANSLIT_MAP.put('и', "i");   TRANSLIT_MAP.put('й', "y");   TRANSLIT_MAP.put('к', "k");
        TRANSLIT_MAP.put('л', "l");   TRANSLIT_MAP.put('м', "m");   TRANSLIT_MAP.put('н', "n");
        TRANSLIT_MAP.put('о', "o");   TRANSLIT_MAP.put('п', "p");   TRANSLIT_MAP.put('р', "r");
        TRANSLIT_MAP.put('с', "s");   TRANSLIT_MAP.put('т', "t");   TRANSLIT_MAP.put('у', "u");
        TRANSLIT_MAP.put('ф', "f");   TRANSLIT_MAP.put('х', "kh");  TRANSLIT_MAP.put('ц', "ts");
        TRANSLIT_MAP.put('ч', "ch");  TRANSLIT_MAP.put('ш', "sh");  TRANSLIT_MAP.put('щ', "shch");
        TRANSLIT_MAP.put('ъ', "");    TRANSLIT_MAP.put('ы', "y");   TRANSLIT_MAP.put('ь', "");
        TRANSLIT_MAP.put('э', "e");   TRANSLIT_MAP.put('ю', "yu");  TRANSLIT_MAP.put('я', "ya");
        TRANSLIT_MAP.put('А', "A");   TRANSLIT_MAP.put('Б', "B");   TRANSLIT_MAP.put('В', "V");
        TRANSLIT_MAP.put('Г', "G");   TRANSLIT_MAP.put('Д', "D");   TRANSLIT_MAP.put('Е', "E");
        TRANSLIT_MAP.put('Ё', "Yo");  TRANSLIT_MAP.put('Ж', "Zh");  TRANSLIT_MAP.put('З', "Z");
        TRANSLIT_MAP.put('И', "I");   TRANSLIT_MAP.put('Й', "Y");   TRANSLIT_MAP.put('К', "K");
        TRANSLIT_MAP.put('Л', "L");   TRANSLIT_MAP.put('М', "M");   TRANSLIT_MAP.put('Н', "N");
        TRANSLIT_MAP.put('О', "O");   TRANSLIT_MAP.put('П', "P");   TRANSLIT_MAP.put('Р', "R");
        TRANSLIT_MAP.put('С', "S");   TRANSLIT_MAP.put('Т', "T");   TRANSLIT_MAP.put('У', "U");
        TRANSLIT_MAP.put('Ф', "F");   TRANSLIT_MAP.put('Х', "Kh");  TRANSLIT_MAP.put('Ц', "Ts");
        TRANSLIT_MAP.put('Ч', "Ch");  TRANSLIT_MAP.put('Ш', "Sh");  TRANSLIT_MAP.put('Щ', "Shch");
        TRANSLIT_MAP.put('Ъ', "");    TRANSLIT_MAP.put('Ы', "Y");   TRANSLIT_MAP.put('Ь', "");
        TRANSLIT_MAP.put('Э', "E");   TRANSLIT_MAP.put('Ю', "Yu");  TRANSLIT_MAP.put('Я', "Ya");
    }

    public FileService(@Value("${file.storage.location:uploads}") String storageLocation) {
        this.fileStorageLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public FileAttachment storeFile(MultipartFile file, Long orderId, Long orderItemId, String uploadedBy) {
        return storeFile(file, orderId, orderItemId, uploadedBy, null, null, null, null, null, null);
    }

    public FileAttachment storeFile(MultipartFile file, Long orderId, Long orderItemId, String uploadedBy,
                                     String orderNumber, String managerName, String clientName,
                                     String materialName, String operationNames, String operationParams) {
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String fileName = buildFileName(orderNumber, managerName, clientName, materialName, operationNames, operationParams, extension);
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

    private String buildFileName(String orderNumber, String managerName, String clientName,
                                  String materialName, String operationNames, String operationParams, String extension) {
        List<String> parts = new ArrayList<>();
        if (orderNumber != null && !orderNumber.isBlank()) {
            parts.add(sanitize(orderNumber));
        }
        if (managerName != null && !managerName.isBlank()) {
            parts.add(sanitize(managerName));
        }
        if (clientName != null && !clientName.isBlank()) {
            parts.add("client-" + sanitize(clientName));
        }
        if (materialName != null && !materialName.isBlank()) {
            parts.add(sanitize(materialName));
        }
        if (operationNames != null && !operationNames.isBlank()) {
            parts.add(sanitize(operationNames));
        }
        if (operationParams != null && !operationParams.isBlank()) {
            parts.add(sanitize(operationParams));
        }
        if (parts.isEmpty()) {
            parts.add(UUID.randomUUID().toString());
        }
        String base = String.join("_", parts);
        return extension != null && !extension.isBlank() ? base + "." + extension : base;
    }

    private String sanitize(String value) {
        String transliterated = transliterate(value);
        return transliterated.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private String transliterate(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            String replacement = TRANSLIT_MAP.get(c);
            sb.append(replacement != null ? replacement : c);
        }
        return sb.toString();
    }

    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return null;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) return null;
        return filename.substring(lastDot + 1);
    }

    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName).normalize();
    }

    public void deleteFile(FileAttachment file) {
        try {
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete file " + file.getFileName(), ex);
        }
    }
}
