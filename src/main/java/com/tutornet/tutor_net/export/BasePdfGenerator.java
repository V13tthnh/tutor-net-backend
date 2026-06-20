package com.tutornet.tutor_net.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public abstract class BasePdfGenerator<T> {

    protected final TemplateEngine templateEngine;

    // TEMPLATE METHOD 1: Sinh ra mảng byte
    public byte[] generatePdfBytes(T dataPayload) {
        Context ctx = new Context();
        buildContext(ctx, dataPayload);
        String htmlContent = templateEngine.process("email/" + getTemplateName(), ctx);
        return performRender(htmlContent);
    }

    // TEMPLATE METHOD 2: Trả trực tiếp file PDF về cho trình duyệt
    public void exportToHttpResponse(T dataPayload, HttpServletResponse response, String fileName) {
        try {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".pdf");

            byte[] pdfBytes = generatePdfBytes(dataPayload);

            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("Lỗi khi xuất PDF ra HTTP Response: {}", e.getMessage());
            throw new RuntimeException("Lỗi tải file PDF");
        }
    }

    // Lớp con triển khai 2 hàm này
    protected abstract String getTemplateName();
    protected abstract void buildContext(Context ctx, T dataPayload);

    // Xử lý core của thư viện OpenHTMLToPDF
    private byte[] performRender(String htmlContent) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFont(() -> {
                try {
                    return new ClassPathResource("fonts/times.ttf").getInputStream();
                } catch (IOException e) {
                    throw new RuntimeException("Không tìm thấy file font times.ttf", e);
                }
            }, "Times New Roman");
            builder.withHtmlContent(htmlContent, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi render PDF: " + e.getMessage(), e);
        }
    }
}