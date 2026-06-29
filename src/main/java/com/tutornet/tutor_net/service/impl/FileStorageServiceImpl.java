package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.config.FileStorageProperties;
import com.tutornet.tutor_net.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl {

    private final FileStorageProperties props;

    public String storeAvatar(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File không được rỗng");
        }
        if (file.getSize() > props.getMaxSize()) {
            throw new BusinessException("File không được vượt quá 2MB");
        }
        if (!props.getAllowedTypes().contains(file.getContentType())) {
            throw new BusinessException("Chỉ chấp nhận file ảnh jpg, png, webp, gif");
        }

        if (!validateMagicBytes(file)) {
            throw new BusinessException("Cấu trúc tệp tin thực tế không hợp lệ hoặc chứa mã thực thi độc hại (MIME Spoofing)!");
        }

        String ext = getExtension(file.getOriginalFilename());
        List<String> allowedExts = List.of("jpg", "jpeg", "png", "webp", "gif");
        if (!allowedExts.contains(ext)) {
            throw new BusinessException("Tên đuôi file mở rộng không hợp lệ. Chỉ chấp nhận jpg, jpeg, png, webp, gif");
        }

        Path uploadPath = Paths.get(props.getDir(), "avatars");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "." + ext;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/avatars/" + fileName;
    }

    public String storeDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("File không được rỗng");
        }
        if (file.getSize() > props.getMaxSize()) {
            throw new BusinessException("File không được vượt quá 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                (!props.getAllowedTypes().contains(contentType) && !contentType.equals("application/pdf"))) {
            throw new BusinessException("Chỉ chấp nhận file ảnh (jpg, png, webp, gif) hoặc tài liệu PDF");
        }

        if (!validateMagicBytes(file)) {
            throw new BusinessException(
                    "Từ chối: Cấu trúc tệp tin thực tế không hợp lệ hoặc chứa mã thực thi độc hại (MIME Spoofing)!");
        }

        String ext = getExtension(file.getOriginalFilename());
        List<String> allowedExts = List.of("pdf", "jpg", "jpeg", "png", "webp", "gif");
        if (!allowedExts.contains(ext)) {
            throw new BusinessException(
                    "Tên đuôi file mở rộng không hợp lệ. Chỉ chấp nhận pdf, jpg, jpeg, png, webp, gif");
        }

        Path uploadPath = Paths.get(props.getDir(), "documents");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID() + "." + ext;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/documents/" + fileName;
    }

    private boolean validateMagicBytes(MultipartFile file) {
        try {
            byte[] header = new byte[4];
            try (var is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4)
                    return false;
            }

            // Đọc 200 byte để quét thẻ script độc hại (chống polyglot)
            byte[] sample = new byte[200];
            try (var is = file.getInputStream()) {
                int read = is.read(sample);
                if (read > 0) {
                    String sampleStr = new String(sample, 0, read, java.nio.charset.StandardCharsets.UTF_8);
                    if (sampleStr.contains("<?php") || sampleStr.contains("<?") || sampleStr.contains("<script")) {
                        return false;
                    }
                }
            }

            String contentType = file.getContentType();
            if (contentType == null)
                return false;

            if (contentType.equals("image/jpeg")) {
                return header[0] == (byte) 0xFF && header[1] == (byte) 0xD8;
            } else if (contentType.equals("image/png")) {
                return header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E
                        && header[3] == (byte) 0x47;
            } else if (contentType.equals("application/pdf")) {
                return header[0] == (byte) 0x25 && header[1] == (byte) 0x50 && header[2] == (byte) 0x44
                        && header[3] == (byte) 0x46;
            } else if (contentType.equals("image/gif")) {
                return header[0] == (byte) 'G' && header[1] == (byte) 'I' && header[2] == (byte) 'F';
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Lưu PDF hợp đồng từ byte[] dùng bởi ContractSignServiceImpl
    // ---------------------------------------------------------------

    /**
     * Lưu PDF hợp đồng từ byte[] sinh ra bởi OpenHTMLToPDF.
     * Không cần MultipartFile — file được tạo nội bộ, không upload từ client.
     *
     * @param contractNumber Mã hợp đồng dùng làm tên file (VD: TN-2026-001)
     * @param pdfBytes       Nội dung PDF dạng byte[]
     * @return Đường dẫn lưu vào DB (VD:
     *         /uploads/contracts/contract_TN-2026-001.pdf)
     */
    public String storeContract(String contractNumber, byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException("PDF bytes không được rỗng");
        }

        Path uploadPath = Paths.get(props.getDir(), "contracts");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = "contract_" + sanitize(contractNumber) + ".pdf";
        Path filePath = uploadPath.resolve(fileName);

        // TRUNCATE_EXISTING: nếu ký lại thì ghi đè file cũ (không để thừa file)
        Files.write(filePath, pdfBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/uploads/contracts/" + fileName;
    }

    // ---------------------------------------------------------------
    // Existing helpers
    // ---------------------------------------------------------------

    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(props.getBaseUrl()))
            return;
        try {
            String fileName = avatarUrl.substring(props.getBaseUrl().length() + 1);
            Path filePath = Paths.get(props.getDir()).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    public String toFullUrl(String filePath) {
        if (filePath == null)
            return null;
        if (filePath.startsWith("http"))
            return filePath;
        return props.getBaseUrl() + filePath;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /** Loại bỏ ký tự đặc biệt để dùng làm tên file an toàn */
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }
}