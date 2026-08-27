package com.qlnv.modules.schedule.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.schedule.entity.SchedulePeriod;
import com.qlnv.modules.schedule.repository.SchedulePeriodRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScheduleExcelService {

    private final ScheduleService scheduleService;
    private final SchedulePeriodRepository periodRepository;

    public byte[] exportScheduleMatrix(Integer periodId, String project, String position, String keyword) {
        Map<String, Object> matrix = scheduleService.getAdminScheduleMatrix(periodId, project, position, keyword);
        SchedulePeriod period = (SchedulePeriod) matrix.get("period");
        int daysInMonth = (int) matrix.get("days_in_month");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) matrix.get("users");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = period != null ? "Lich_T" + period.getMonth() + "_" + period.getYear() : "Lich_Lam_Viec";
            Sheet sheet = workbook.createSheet(sheetName);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG TỔNG HỢP LỊCH LÀM VIỆC THÁNG " + (period != null ? period.getMonth() + "/" + period.getYear() : ""));
            
            // Header row
            Row headerRow = sheet.createRow(2);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Mã NV/TTS");
            headerRow.createCell(2).setCellValue("Họ và tên");
            headerRow.createCell(3).setCellValue("Dự án");
            headerRow.createCell(4).setCellValue("Vị trí");

            for (int d = 1; d <= daysInMonth; d++) {
                Cell c = headerRow.createCell(4 + d);
                c.setCellValue("N" + d);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(4 + d, 5 * 256);
            }

            int lastCol = 4 + daysInMonth;
            headerRow.createCell(lastCol + 1).setCellValue("Tổng S");
            headerRow.createCell(lastCol + 2).setCellValue("Tổng C");
            headerRow.createCell(lastCol + 3).setCellValue("Tổng SC");
            headerRow.createCell(lastCol + 4).setCellValue("Tổng Ngày");

            int rowIdx = 3;
            for (int i = 0; i < users.size(); i++) {
                Map<String, Object> u = users.get(i);
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(i + 1);
                r.createCell(1).setCellValue(String.valueOf(u.getOrDefault("employee_code", "")));
                r.createCell(2).setCellValue(String.valueOf(u.getOrDefault("full_name", "")));
                r.createCell(3).setCellValue(String.valueOf(u.getOrDefault("project", "")));
                r.createCell(4).setCellValue(String.valueOf(u.getOrDefault("position", "")));

                @SuppressWarnings("unchecked")
                Map<Integer, String> shifts = (Map<Integer, String>) u.getOrDefault("shifts", Map.of());
                for (int d = 1; d <= daysInMonth; d++) {
                    String shift = shifts.getOrDefault(d, "");
                    r.createCell(4 + d).setCellValue(shift);
                }

                r.createCell(lastCol + 1).setCellValue(Integer.parseInt(String.valueOf(u.getOrDefault("total_s", 0))));
                r.createCell(lastCol + 2).setCellValue(Integer.parseInt(String.valueOf(u.getOrDefault("total_c", 0))));
                r.createCell(lastCol + 3).setCellValue(Integer.parseInt(String.valueOf(u.getOrDefault("total_sc", 0))));
                r.createCell(lastCol + 4).setCellValue(Integer.parseInt(String.valueOf(u.getOrDefault("total_days", 0))));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất lịch làm việc ra Excel: " + e.getMessage());
        }
    }
}
