package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.config.FileStorageProperties;
import com.tutornet.tutor_net.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl {

    private final FileStorageProperties props;

    // ---------------------------------------------------------------
    // Existing methods (giữ nguyên)
    // ---------------------------------------------------------------

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

        Path uploadPath = Paths.get(props.getDir(), "avatars");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String ext      = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + ext;
        Path   filePath = uploadPath.resolve(fileName);

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

        Path uploadPath = Paths.get(props.getDir(), "documents");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String ext      = getExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "." + ext;
        Path   filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/documents/" + fileName;
    }

    // ---------------------------------------------------------------
    // Bổ sung: lưu PDF hợp đồng từ byte[] (dùng bởi ContractSignServiceImpl)
    // ---------------------------------------------------------------

    /**
     * Lưu PDF hợp đồng từ byte[] sinh ra bởi OpenHTMLToPDF.
     * Không cần MultipartFile — file được tạo nội bộ, không upload từ client.
     *
     * @param contractNumber Mã hợp đồng dùng làm tên file (VD: TN-2026-001)
     * @param pdfBytes       Nội dung PDF dạng byte[]
     * @return Đường dẫn lưu vào DB   (VD: /uploads/contracts/contract_TN-2026-001.pdf)
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
        Path   filePath = uploadPath.resolve(fileName);

        // TRUNCATE_EXISTING: nếu ký lại thì ghi đè file cũ (không để thừa file)
        Files.write(filePath, pdfBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        return "/uploads/contracts/" + fileName;
    }

    // ---------------------------------------------------------------
    // Existing helpers (giữ nguyên)
    // ---------------------------------------------------------------

    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(props.getBaseUrl())) return;
        try {
            String fileName = avatarUrl.substring(props.getBaseUrl().length() + 1);
            Path   filePath = Paths.get(props.getDir()).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
    }

    public String toFullUrl(String filePath) {
        if (filePath == null) return null;
        if (filePath.startsWith("http")) return filePath;
        return props.getBaseUrl() + filePath;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /** Loại bỏ ký tự đặc biệt để dùng làm tên file an toàn */
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
    }
}