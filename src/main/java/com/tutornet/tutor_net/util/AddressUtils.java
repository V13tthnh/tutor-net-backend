package com.tutornet.tutor_net.util;

/**
 * Tiện ích ghép/tách chuỗi địa chỉ 3 cấp.
 *
 * Định dạng lưu DB: "[address], [ward], [province]"
 * Ví dụ: "123 Nguyễn Huệ, Phường Bến Nghé, TP. Hồ Chí Minh"
 *
 * Dùng dấu phân cách " | " thay vì dấu phẩy để tránh nhầm lẫn
 * khi tên địa chỉ bản thân đã có dấu phẩy.
 */
public final class AddressUtils {

    private static final String SEPARATOR = " | ";

    private AddressUtils() {}

    /**
     * Ghép 3 thành phần thành 1 chuỗi lưu vào cột address.
     *
     * @param address  Số nhà, tên đường (bắt buộc)
     * @param ward     Phường/Xã/Thị trấn (bắt buộc)
     * @param province Tỉnh/Thành phố (bắt buộc)
     * @return Chuỗi đã ghép, ví dụ: "123 Nguyễn Huệ | Phường Bến Nghé | TP. Hồ Chí Minh"
     */
    public static String build(String address, String ward, String province) {
        // Xử lý an toàn với null
        String a = (address == null) ? "" : address.trim();
        String w = (ward == null) ? "" : ward.trim();
        String p = (province == null) ? "" : province.trim();

        // Nếu user không nhập gì cả, trả về chuỗi rỗng thay vì " |  | "
        if (a.isEmpty() && w.isEmpty() && p.isEmpty()) {
            return null;
        }

        return a + SEPARATOR + w + SEPARATOR + p;
    }

    /**
     * Tách chuỗi địa chỉ từ DB thành DTO 3 thành phần.
     * Nếu chuỗi không đúng định dạng (dữ liệu cũ, migration),
     * toàn bộ chuỗi được trả về trong address; ward và province là chuỗi rỗng.
     *
     * @param fullAddress Chuỗi lưu trong DB
     * @return Parts chứa address / ward / province
     */
    public static Parts parse(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return new Parts("", "", "");
        }
        String[] parts = fullAddress.split("\\s*\\|\\s*", -1);
        if (parts.length == 3) {
            return new Parts(parts[0].trim(), parts[1].trim(), parts[2].trim());
        }
        // Dữ liệu legacy không có dấu phân cách — giữ nguyên toàn bộ ở address
        return new Parts(fullAddress.trim(), "", "");
    }

    public record Parts(String address, String ward, String province) {}
}