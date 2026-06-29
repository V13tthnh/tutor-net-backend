package com.tutornet.tutor_net.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SecuritySandboxHelper {

    private static final String COOKIE_NAME = "security_sandbox";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Lấy danh sách các cờ lỗ hổng (flags) đang được bật từ Cookie.
     */
    public static List<String> getActiveFlags() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return Collections.emptyList();
            }

            HttpServletRequest request = attributes.getRequest();
            Cookie[] cookies = request.getCookies();
            if (cookies == null) {
                return Collections.emptyList();
            }

            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return decodeCookieValue(cookie.getValue());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi đọc Security Sandbox Cookie: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Giải mã Cookie (Định dạng: base64UrlPayload.signature)
     */
    private static List<String> decodeCookieValue(String cookieValue) {
        try {
            if (cookieValue == null || cookieValue.isEmpty()) {
                return Collections.emptyList();
            }

            int lastDot = cookieValue.lastIndexOf('.');
            String payloadBase64 = (lastDot == -1) ? cookieValue : cookieValue.substring(0, lastDot);

            byte[] decodedBytes = Base64.getUrlDecoder().decode(payloadBase64);
            String jsonPayload = new String(decodedBytes);

            return objectMapper.readValue(jsonPayload, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Lỗi khi parse JSON Security Sandbox: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Kiểm tra xem một lỗ hổng cụ thể có đang được bật hay không.
     * @param flag Tên lỗ hổng (Ví dụ: "bypass_login", "union_sqli")
     */
    public static boolean isVulnerable(String flag) {
        List<String> activeFlags = getActiveFlags();
        return activeFlags != null && activeFlags.contains(flag);
    }
}
