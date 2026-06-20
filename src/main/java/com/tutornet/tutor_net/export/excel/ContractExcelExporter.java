package com.tutornet.tutor_net.export.excel;

import com.tutornet.tutor_net.dto.response.AdminContractResponse;
import com.tutornet.tutor_net.export.BaseExcelExporter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class ContractExcelExporter extends BaseExcelExporter<AdminContractResponse> {

    @Override
    protected String getSheetName() {
        return "Danh_Sach_Hop_Dong";
    }

    @Override
    protected String[] getHeaders() {
        return new String[]{
                "Mã HĐ", "Mã Lớp", "Gia Sư", "Phụ Huynh", "Phí Nhận Lớp", "Ngày Tạo", "Trạng Thái HĐ", "Trạng Thái Thu Phí"
        };
    }

    @Override
    protected void writeDataRow(AdminContractResponse data, Row row, Workbook workbook) {
        row.createCell(0).setCellValue(data.contractNumber());
        row.createCell(1).setCellValue(data.classCode() != null ? data.classCode() : "N/A");
        row.createCell(2).setCellValue(data.tutorName());
        row.createCell(3).setCellValue(data.contactName());
        row.createCell(4).setCellValue(data.introductionFee() != null ? data.introductionFee().doubleValue() : 0);
        row.createCell(5).setCellValue(data.createdAt().toString());
        row.createCell(6).setCellValue(data.status().name());
        row.createCell(7).setCellValue(data.isFeePaid() ? "Đã Thu" : "Chưa Thu");
    }
}