package com.qlnv.modules.schedule.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.schedule.entity.Schedule;
import com.qlnv.modules.schedule.entity.SchedulePeriod;
import com.qlnv.modules.schedule.repository.SchedulePeriodRepository;
import com.qlnv.modules.schedule.repository.ScheduleRepository;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleExcelService {

    private final ScheduleService scheduleService;
    private final SchedulePeriodRepository periodRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    private SchedulePeriod resolvePeriod(Integer periodId) {
        if (periodId != null) {
            return periodRepository.findById(periodId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy kỳ đăng ký lịch (ID: " + periodId + ")"));
        }
        return periodRepository.findFirstByStatusOrderByIdDesc("open")
                .orElseGet(() -> periodRepository.findAllByOrderByYearDescMonthDesc().stream().findFirst()
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Chưa có kỳ đăng ký lịch nào được tạo")));
    }

    public byte[] generateScheduleImportTemplate(Integer periodId) {
        SchedulePeriod period = resolvePeriod(periodId);

        int month = period.getMonth();
        int year = period.getYear();
        int numDays = YearMonth.of(year, month).lengthOfMonth();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import_Lich_t" + month + "_" + year);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            Cell c0 = headerRow.createCell(0);
            c0.setCellValue("HỌ VÀ TÊN (*)");
            c0.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 22 * 256);

            Cell c1 = headerRow.createCell(1);
            c1.setCellValue("MÃ NV (*)");
            c1.setCellStyle(headerStyle);
            sheet.setColumnWidth(1, 15 * 256);

            for (int d = 1; d <= numDays; d++) {
                Cell c = headerRow.createCell(1 + d);
                c.setCellValue(String.valueOf(d));
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(1 + d, 6 * 256);
            }

            // Example row
            Row exRow = sheet.createRow(1);
            exRow.createCell(0).setCellValue("Nguyễn Văn A");
            exRow.createCell(1).setCellValue("TTS1");
            for (int d = 1; d <= numDays; d++) {
                LocalDate date = LocalDate.of(year, month, d);
                if (date.getDayOfWeek().getValue() <= 5) {
                    exRow.createCell(1 + d).setCellValue("SC");
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo mẫu import lịch: " + e.getMessage());
        }
    }

    public List<String> getScheduleLinkSheets(String urlStr) {
        try (Workbook workbook = downloadWorkbookFromUrl(urlStr)) {
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
            return sheetNames;
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể lấy danh sách sheet: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> importScheduleFromExcel(Integer periodId, MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            return processScheduleWorkbook(periodId, workbook, null);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Excel lịch: " + e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> importScheduleFromLink(Integer periodId, String url, String sheetName) {
        try (Workbook workbook = downloadWorkbookFromUrl(url)) {
            return processScheduleWorkbook(periodId, workbook, sheetName);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể import lịch từ link: " + e.getMessage());
        }
    }

    private Map<String, Object> processScheduleWorkbook(Integer periodId, Workbook workbook, String targetSheetName) {
        SchedulePeriod period = resolvePeriod(periodId);

        int month = period.getMonth();
        int year = period.getYear();
        int numDays = YearMonth.of(year, month).lengthOfMonth();

        Sheet sheet = targetSheetName != null ? workbook.getSheet(targetSheetName) : workbook.getSheetAt(0);
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        if (sheet == null) {
            throw new ApiException("Không tìm thấy sheet trong file");
        }

        Iterator<Row> rowIterator = sheet.iterator();
        if (!rowIterator.hasNext()) {
            return Map.of("message", "File không có dữ liệu", "success", 0, "skipped", 0);
        }

        // Read all rows
        List<Row> rows = new ArrayList<>();
        while (rowIterator.hasNext()) {
            rows.add(rowIterator.next());
        }

        Integer empCodeCol = null;
        Integer nameCol = null;
        Map<Integer, Integer> dayColMap = new HashMap<>(); // dayNumber (1..numDays) -> colIndex

        int maxHeaderScan = Math.min(10, rows.size());
        int headerLastRowIdx = 0;

        for (int rIdx = 0; rIdx < maxHeaderScan; rIdx++) {
            Row r = rows.get(rIdx);
            for (Cell cell : r) {
                String val = getCellString(cell).trim().toLowerCase();

                if (empCodeCol == null && (val.contains("mã") || val.contains("code") || val.contains("manv") || val.contains("mã tts") || val.contains("mã nv"))) {
                    empCodeCol = cell.getColumnIndex();
                    headerLastRowIdx = Math.max(headerLastRowIdx, rIdx);
                }
                if (nameCol == null && (val.contains("họ và tên") || val.contains("họ tên") || val.contains("full_name") || val.equals("tên") || val.contains("họ tts"))) {
                    nameCol = cell.getColumnIndex();
                    headerLastRowIdx = Math.max(headerLastRowIdx, rIdx);
                }

                Integer day = parseDayNumber(cell, numDays);
                if (day != null) {
                    dayColMap.put(day, cell.getColumnIndex());
                    headerLastRowIdx = Math.max(headerLastRowIdx, rIdx);
                }
            }
        }

        if (empCodeCol == null && nameCol == null) {
            empCodeCol = 1;
            nameCol = 0;
        }

        List<User> allUsers = userRepository.findAll();
        Map<String, User> userByCode = new HashMap<>();
        Map<String, User> userByName = new HashMap<>();
        for (User u : allUsers) {
            if (u.getEmployeeCode() != null) {
                userByCode.put(normalizeCode(u.getEmployeeCode()), u);
                userByCode.put(u.getEmployeeCode().trim().toLowerCase(), u);
            }
            if (u.getFullName() != null) {
                userByName.put(normalizeName(u.getFullName()), u);
            }
        }

        int successCount = 0;
        int skipCount = 0;

        for (int rIdx = headerLastRowIdx + 1; rIdx < rows.size(); rIdx++) {
            Row r = rows.get(rIdx);

            String codeStr = (empCodeCol != null) ? getCellString(r.getCell(empCodeCol)).trim() : "";
            String nameStr = (nameCol != null) ? getCellString(r.getCell(nameCol)).trim() : "";

            if (codeStr.isEmpty() && nameStr.isEmpty()) {
                continue;
            }

            User user = null;
            if (!codeStr.isEmpty()) {
                user = userByCode.get(normalizeCode(codeStr));
                if (user == null) user = userByCode.get(codeStr.toLowerCase());
            }
            if (user == null && !nameStr.isEmpty()) {
                user = userByName.get(normalizeName(nameStr));
            }

            if (user == null) {
                // Ignore legend/note rows (like "S", "Sáng", "C", "Chiều", etc.)
                if (nameStr.length() > 3 || codeStr.length() > 2) {
                    log.info("Schedule row skipped: code='{}', name='{}'", codeStr, nameStr);
                    skipCount++;
                }
                continue;
            }

            for (Map.Entry<Integer, Integer> entry : dayColMap.entrySet()) {
                int day = entry.getKey();
                int col = entry.getValue();
                Cell shiftCell = r.getCell(col);
                String rawShift = getCellString(shiftCell).trim().toUpperCase();

                String shift = normalizeShift(rawShift);
                LocalDate workDay = LocalDate.of(year, month, day);

                Optional<Schedule> existing = scheduleRepository.findByPeriodIdAndUserIdAndWorkDay(period.getId(), user.getId(), workDay);
                if (shift != null && !shift.isEmpty()) {
                    Schedule s;
                    if (existing.isPresent()) {
                        s = existing.get();
                    } else {
                        s = Schedule.builder()
                                .periodId(period.getId())
                                .userId(user.getId())
                                .workDay(workDay)
                                .createdAt(LocalDateTime.now())
                                .build();
                    }
                    s.setShift(shift);
                    scheduleRepository.save(s);
                } else if (existing.isPresent()) {
                    scheduleRepository.delete(existing.get());
                }
            }
            successCount++;
        }

        String msg = String.format("Import lịch thành công cho %d nhân sự (Kỳ tháng %d/%d)", successCount, period.getMonth(), period.getYear());
        return Map.of("message", msg, "success", successCount, "skipped", skipCount);
    }

    private String normalizeCode(String code) {
        if (code == null) return "";
        code = code.trim().toLowerCase();
        Matcher m = Pattern.compile("^([a-z]+)0*(\\d+)$").matcher(code);
        if (m.find()) {
            return m.group(1) + m.group(2);
        }
        return code;
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("\\s+", " ")
                .replace("\u00A0", " ")
                .trim();
    }

    private Integer parseDayNumber(Cell cell, int maxDays) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                if (d != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    int day = cal.get(Calendar.DAY_OF_MONTH);
                    if (day >= 1 && day <= maxDays) return day;
                }
            }
            double num = cell.getNumericCellValue();
            if (num >= 1 && num <= maxDays && num == Math.floor(num)) {
                return (int) num;
            }
        }

        String val = getCellString(cell).trim();
        if (val.isEmpty()) return null;

        // Ignore pure day-of-week strings (e.g. "Thứ 2", "Thứ 7", "CN", "T2")
        if (val.matches("(?i)^(thứ\\s*[2-7]|t[2-7]|cn|chủ\\s*nhật)$")) {
            return null;
        }

        // 1. If multi-line (e.g. "1\nT2" or "01\nThứ 2"), parse first line
        String firstLine = val.split("\r?\n")[0].trim();
        try {
            int d = Integer.parseInt(firstLine);
            if (d >= 1 && d <= maxDays) return d;
        } catch (Exception ignored) {}

        // 2. Pattern: "1 (T2)", "01 (T2)", "N1", "N01", "Ngày 1"
        Matcher m1 = Pattern.compile("^(?:n|ngày\\s*)?(\\d{1,2})(?:\\s*\\(.*|\\s*[-/].*)?$", Pattern.CASE_INSENSITIVE).matcher(firstLine);
        if (m1.find()) {
            try {
                int d = Integer.parseInt(m1.group(1));
                if (d >= 1 && d <= maxDays) return d;
            } catch (Exception ignored) {}
        }

        // 3. Date format: "01/08", "1/8", "01/08/2026", "01-08-2026"
        Matcher m2 = Pattern.compile("^(\\d{1,2})[/-]\\d{1,2}(?:[/-]\\d{2,4})?").matcher(val);
        if (m2.find()) {
            try {
                int d = Integer.parseInt(m2.group(1));
                if (d >= 1 && d <= maxDays) return d;
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String normalizeShift(String raw) {
        if (raw == null) return "";
        raw = raw.trim().toUpperCase();
        if (raw.equals("SC") || raw.equals("S/C") || raw.equals("CẢ NGÀY") || raw.equals("CA NGAY") || raw.equals("FULL")) return "SC";
        if (raw.equals("S") || raw.equals("SÁNG") || raw.equals("SANG")) return "S";
        if (raw.equals("C") || raw.equals("CHIỀU") || raw.equals("CHIEU")) return "C";
        if (raw.equals("OFF") || raw.equals("NGHỈ") || raw.equals("NGHI") || raw.equals("-") || raw.equals("X")) return "";
        return raw;
    }

    private Workbook downloadWorkbookFromUrl(String urlStr) {
        try {
            String exportUrl = urlStr;
            Matcher matcher = Pattern.compile("/d/([a-zA-Z0-9-_]+)").matcher(urlStr);
            if (matcher.find()) {
                String sheetId = matcher.group(1);
                exportUrl = "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=xlsx";
            }
            URL u = new URL(exportUrl);
            URLConnection conn = u.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            try (InputStream is = conn.getInputStream()) {
                return WorkbookFactory.create(is);
            }
        } catch (Exception e) {
            log.error("Error downloading spreadsheet from URL: {}", urlStr, e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể tải file từ Google Sheets link: " + e.getMessage());
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date d = cell.getDateCellValue();
                if (d != null) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
                }
            }
            double num = cell.getNumericCellValue();
            if (num == Math.floor(num) && !Double.isInfinite(num)) {
                return java.math.BigDecimal.valueOf(num).toBigInteger().toString();
            } else {
                return java.math.BigDecimal.valueOf(num).stripTrailingZeros().toPlainString();
            }
        }
        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell).trim();
        if (val.matches("^\\d+\\.0+$")) {
            val = val.substring(0, val.indexOf('.'));
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    public byte[] exportScheduleMatrix(Integer periodId, Integer month, Integer year, String project, String position, String keyword) {
        Map<String, Object> matrix = scheduleService.getAdminScheduleMatrix(periodId, month, year, project, position, keyword);
        Map<String, Object> period = (Map<String, Object>) matrix.get("period");
        int daysInMonth = (int) matrix.get("days_in_month");
        List<Map<String, Object>> users = (List<Map<String, Object>>) matrix.get("rows");

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = period != null ? "Lich_T" + period.get("month") + "_" + period.get("year") : "Lich_Lam_Viec";
            Sheet sheet = workbook.createSheet(sheetName);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG TỔNG HỢP LỊCH LÀM VIỆC THÁNG " + (period != null ? period.get("month") + "/" + period.get("year") : ""));
            
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
