package com.qlnv.modules.overtime.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.overtime.dto.OvertimeResponseDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OvertimeExportService {

    private final OvertimeService overtimeService;

    public byte[] exportOvertimeReport(
            String status, String project, Integer month, Integer year, LocalDate fromDate, LocalDate toDate, String keyword) {

        List<OvertimeResponseDto.Response> list = overtimeService.getAdminOvertimeList(
                status, project, month, year, fromDate, toDate, keyword
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("TongHopOT");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PHỤ LỤC 03 - TỔNG HỢP LÀM THÊM GIỜ (OT)");

            String[] headers = {
                "STT", "Mã NV", "Họ và tên", "Dự án", "Ngày OT", "Từ giờ", "Đến giờ",
                "Số giờ thực tế", "Hệ số", "Số giờ quy đổi", "Lý do", "Trạng thái", "Lý do từ chối"
            };

            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }

            int rowIdx = 3;
            double totalRaw = 0.0;
            double totalWeighted = 0.0;

            for (int i = 0; i < list.size(); i++) {
                OvertimeResponseDto.Response ot = list.get(i);
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(i + 1);
                r.createCell(1).setCellValue(ot.getEmployeeCode() != null ? ot.getEmployeeCode() : "");
                r.createCell(2).setCellValue(ot.getFullName() != null ? ot.getFullName() : "");
                r.createCell(3).setCellValue(ot.getProject() != null ? ot.getProject() : "");
                r.createCell(4).setCellValue(ot.getWorkDate() != null ? ot.getWorkDate().toString() : "");
                r.createCell(5).setCellValue(ot.getStartTime() != null ? ot.getStartTime() : "");
                r.createCell(6).setCellValue(ot.getEndTime() != null ? ot.getEndTime() : "");
                
                double raw = ot.getRawHours() != null ? ot.getRawHours() : 0.0;
                double factor = ot.getFactor() != null ? ot.getFactor() : 1.5;
                double weighted = ot.getWeightedHours() != null ? ot.getWeightedHours() : 0.0;

                r.createCell(7).setCellValue(raw);
                r.createCell(8).setCellValue(factor);
                r.createCell(9).setCellValue(weighted);
                r.createCell(10).setCellValue(ot.getReason() != null ? ot.getReason() : "");
                r.createCell(11).setCellValue(ot.getStatus() != null ? ot.getStatus() : "");
                r.createCell(12).setCellValue(ot.getRejectReason() != null ? ot.getRejectReason() : "");

                totalRaw += raw;
                totalWeighted += weighted;
            }

            // Summary Row
            Row summaryRow = sheet.createRow(rowIdx);
            summaryRow.createCell(2).setCellValue("TỔNG CỘNG");
            summaryRow.createCell(7).setCellValue(Math.round(totalRaw * 100.0) / 100.0);
            summaryRow.createCell(9).setCellValue(Math.round(totalWeighted * 100.0) / 100.0);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất báo cáo OT ra Excel: " + e.getMessage());
        }
    }
}
