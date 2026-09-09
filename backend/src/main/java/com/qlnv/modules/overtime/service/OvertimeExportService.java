package com.qlnv.modules.overtime.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.overtime.dto.OvertimeResponseDto;
import com.qlnv.modules.overtime.entity.OvertimeRequest;
import com.qlnv.modules.overtime.repository.OvertimeRequestRepository;
import com.qlnv.modules.overtime.repository.OvertimeSpecification;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OvertimeExportService {

    private final OvertimeService overtimeService;
    private final OvertimeRequestRepository overtimeRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ══════════════════════════════════════════════════════════════════════════
    // FILE 1: PHỤ LỤC 02 — BẢNG TỔNG HỢP CÔNG THỜI GIAN LÀM THÊM GIỜ (Ảnh 1)
    // ══════════════════════════════════════════════════════════════════════════
    public byte[] exportPhuLuc02Summary(
            String status, String project, Integer month, Integer year, LocalDate fromDate, LocalDate toDate, String keyword) {

        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null && year >= 2000) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        int numDays = ym.lengthOfMonth();
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        // Lấy danh sách OT trong tháng (lọc theo status/project/keyword nếu có)
        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(
                null,
                (status != null && !status.trim().isEmpty()) ? status : null,
                project,
                startOfMonth,
                endOfMonth,
                keyword
        );
        List<OvertimeRequest> otList = overtimeRepository.findAll(spec);

        // Gom nhóm OT theo User ID
        Map<Integer, List<OvertimeRequest>> userOtMap = otList.stream()
                .collect(Collectors.groupingBy(OvertimeRequest::getUserId));

        // Load thông tin các User tương ứng
        Map<Integer, User> userCache = new HashMap<>();
        if (!userOtMap.isEmpty()) {
            List<User> userEntities = userRepository.findAllById(userOtMap.keySet());
            for (User u : userEntities) {
                userCache.put(u.getId(), u);
            }
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PL02_TongHopOT");
            sheet.setDisplayGridlines(true);

            // ── Colors ──
            byte[] greenRgb  = new byte[]{(byte) 226, (byte) 239, (byte) 218}; // #E2EFDA (Tháng header)
            byte[] yellowRgb = new byte[]{(byte) 255, (byte) 242, (byte) 204}; // #FFF2CC (Làm thêm giờ header)
            byte[] pinkRgb   = new byte[]{(byte) 252, (byte) 228, (byte) 214}; // #FCE4D6 (CN header)
            byte[] brightYellowRgb = new byte[]{(byte) 255, (byte) 255, (byte) 0}; // #FFFF00 (Tổng cột / Tổng dòng)
            byte[] grayRgb   = new byte[]{(byte) 242, (byte) 242, (byte) 242}; // #F2F2F2 (Tên dự án)

            // ── Styles ──
            XSSFCellStyle styleCompany = createTextStyle(workbook, 10, true, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            XSSFCellStyle styleDept    = createTextStyle(workbook, 10, true, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

            XSSFCellStyle styleNationalTop = createTextStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            XSSFCellStyle styleNationalSub = createTextStyle(workbook, 10, true, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

            XSSFCellStyle styleTitle = createTextStyle(workbook, 13, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

            XSSFCellStyle styleProjectHeader = createColorStyle(workbook, 10, true, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, grayRgb, true);

            XSSFCellStyle styleHeaderBasic = createBorderedStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);
            XSSFCellStyle styleHeaderGreen = createColorStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, greenRgb, true);
            XSSFCellStyle styleHeaderYellow = createColorStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, yellowRgb, true);
            XSSFCellStyle styleHeaderPink   = createColorStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, pinkRgb, true);
            XSSFCellStyle styleHeaderBrightYellow = createColorStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, brightYellowRgb, true);

            XSSFCellStyle styleDataLeft   = createBorderedStyle(workbook, 9, false, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, null);
            XSSFCellStyle styleDataCenter = createBorderedStyle(workbook, 9, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);
            XSSFCellStyle styleDataNum    = createBorderedStyle(workbook, 9, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);
            XSSFCellStyle styleDataTotalCol = createColorStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, brightYellowRgb, true);

            XSSFCellStyle styleSummaryRow = createColorStyle(workbook, 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, brightYellowRgb, true);

            int totalCols = 5 + numDays + 7; // 5 fixed + numDays + 7 summary breakdown cols

            // ── Row 0 & 1: Header Công ty & Quốc hiệu ──
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("CÔNG TY ĐẦU TƯ CÔNG NGHỆ VIETTEL");
            c0.setCellStyle(styleCompany);

            Cell c0Right = r0.createCell(totalCols - 14);
            c0Right.setCellValue("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
            c0Right.setCellStyle(styleNationalTop);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("PHÒNG CHÍNH TRỊ NHÂN SỰ");
            c1.setCellStyle(styleDept);

            Cell c1Right = r1.createCell(totalCols - 14);
            c1Right.setCellValue("Độc lập – Tự do – Hạnh phúc");
            c1Right.setCellStyle(styleNationalSub);

            // ── Row 3: Title ──
            Row r3 = sheet.createRow(3);
            Cell cTitle = r3.createCell(0);
            cTitle.setCellValue("PHỤ LỤC 02: BẢNG TỔNG HỢP CÔNG THỜI GIAN LÀM THÊM GIỜ");
            cTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, totalCols - 1));

            // ── Row 5: Tên dự án ──
            Row r5 = sheet.createRow(5);
            for (int col = 0; col < totalCols; col++) {
                Cell cell = r5.createCell(col);
                cell.setCellStyle(styleProjectHeader);
                if (col == 0) {
                    cell.setCellValue(project != null && !project.trim().isEmpty() ? "TÊN DỰ ÁN: " + project.trim() : "TÊN DỰ ÁN");
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, totalCols - 1));

            // ── Rows 6..8: Table Headers ──
            Row r6 = sheet.createRow(6);
            Row r7 = sheet.createRow(7);
            Row r8 = sheet.createRow(8);

            // Initialize all cells for rows 6..8 to avoid empty border issues
            for (int r = 6; r <= 8; r++) {
                Row curRow = sheet.getRow(r);
                for (int c = 0; c < totalCols; c++) {
                    Cell cell = curRow.createCell(c);
                    cell.setCellStyle(styleHeaderBasic);
                }
            }

            // Fixed Columns (0..4)
            r6.getCell(0).setCellValue("STT");
            r6.getCell(1).setCellValue("MNV");
            r6.getCell(2).setCellValue("Họ và tên");
            r6.getCell(3).setCellValue("Phòng/ban/Trung tâm");
            r6.getCell(4).setCellValue("Chức danh");

            for (int c = 0; c < 5; c++) {
                sheet.addMergedRegion(new CellRangeAddress(6, 8, c, c));
            }

            // Calendar Columns (5 .. 4 + numDays)
            // Row 6: Tháng ... năm ...
            r6.getCell(5).setCellValue(String.format("Tháng   %02d   năm   %d", targetMonth, targetYear));
            for (int c = 5; c < 5 + numDays; c++) {
                r6.getCell(c).setCellStyle(styleHeaderGreen);
            }
            sheet.addMergedRegion(new CellRangeAddress(6, 6, 5, 4 + numDays));

            // Row 7 (Day number) & Row 8 (Day of week)
            String[] dowNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
            for (int d = 1; d <= numDays; d++) {
                int colIdx = 4 + d;
                LocalDate date = LocalDate.of(targetYear, targetMonth, d);
                DayOfWeek dow = date.getDayOfWeek();
                int dowIdx = (dow == DayOfWeek.SUNDAY) ? 0 : dow.getValue(); // 0=CN, 1=T2,... 6=T7
                String dowStr = dowNames[dowIdx];

                Cell cellDayNum = r7.getCell(colIdx);
                cellDayNum.setCellValue(String.format("%02d", d));

                Cell cellDow = r8.getCell(colIdx);
                cellDow.setCellValue(dowStr);

                if (dow == DayOfWeek.SUNDAY) {
                    cellDayNum.setCellStyle(styleHeaderPink);
                    cellDow.setCellStyle(styleHeaderPink);
                } else {
                    cellDayNum.setCellStyle(styleHeaderBasic);
                    cellDow.setCellStyle(styleHeaderBasic);
                }
            }

            // Summary Breakdown Columns (5 + numDays .. totalCols - 1)
            int sumStartCol = 5 + numDays;
            r6.getCell(sumStartCol).setCellValue("Làm thêm giờ");
            for (int c = sumStartCol; c < totalCols; c++) {
                r6.getCell(c).setCellStyle(styleHeaderYellow);
            }
            sheet.addMergedRegion(new CellRangeAddress(6, 6, sumStartCol, totalCols - 1));

            String[] breakdownHeaders = {
                "12-16 (hoặc 22h)\nHs x 1,5",
                "22-06 (hoặc 22h-06h)\nHs x 2,1",
                "T7/CN\nHs x 2,0",
                "T7/CN sau 22h\nHs x 2,7",
                "Ngày lễ\nHs x 3,0",
                "Ngày lễ sau 22h\nHs x 3,9",
                "Tổng số giờ OT"
            };

            for (int i = 0; i < breakdownHeaders.length; i++) {
                int colIdx = sumStartCol + i;
                Cell cell = r7.getCell(colIdx);
                cell.setCellValue(breakdownHeaders[i]);
                if (i == breakdownHeaders.length - 1) {
                    cell.setCellStyle(styleHeaderBrightYellow);
                    r8.getCell(colIdx).setCellStyle(styleHeaderBrightYellow);
                } else {
                    cell.setCellStyle(styleHeaderYellow);
                    r8.getCell(colIdx).setCellStyle(styleHeaderYellow);
                }
                sheet.addMergedRegion(new CellRangeAddress(7, 8, colIdx, colIdx));
            }

            // ── Data Rows ──
            int dataRowIdx = 9;
            int stt = 1;

            // Sort users by employeeCode
            List<Integer> sortedUserIds = new ArrayList<>(userOtMap.keySet());
            sortedUserIds.sort((u1, u2) -> {
                User usr1 = userCache.get(u1);
                User usr2 = userCache.get(u2);
                String code1 = usr1 != null && usr1.getEmployeeCode() != null ? usr1.getEmployeeCode() : "";
                String code2 = usr2 != null && usr2.getEmployeeCode() != null ? usr2.getEmployeeCode() : "";
                return code1.compareToIgnoreCase(code2);
            });

            double[] colSums = new double[numDays + 1]; // 1-indexed for days
            double sumHs1_5 = 0.0;
            double sumHs2_1 = 0.0;
            double sumHs2_0 = 0.0;
            double sumHs2_7 = 0.0;
            double sumHs3_0 = 0.0;
            double sumHs3_9 = 0.0;
            double grandTotal = 0.0;

            for (Integer uId : sortedUserIds) {
                User user = userCache.get(uId);
                List<OvertimeRequest> userOts = userOtMap.get(uId);

                Row row = sheet.createRow(dataRowIdx++);

                // Fixed data
                String empCode = user != null && user.getEmployeeCode() != null ? user.getEmployeeCode() : "";
                String fullName = user != null && user.getFullName() != null ? user.getFullName() : "";
                String department = getDepartmentName(user);
                String position = getPositionName(user);

                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(empCode);
                row.createCell(2).setCellValue(fullName);
                row.createCell(3).setCellValue(department);
                row.createCell(4).setCellValue(position);

                row.getCell(0).setCellStyle(styleDataCenter);
                row.getCell(1).setCellStyle(styleDataCenter);
                row.getCell(2).setCellStyle(styleDataLeft);
                row.getCell(3).setCellStyle(styleDataCenter);
                row.getCell(4).setCellStyle(styleDataCenter);

                // Map hours by day & calculate factors
                Map<Integer, Double> dayHoursMap = new HashMap<>();
                double userHs1_5 = 0.0;
                double userHs2_1 = 0.0;
                double userHs2_0 = 0.0;
                double userHs2_7 = 0.0;
                double userHs3_0 = 0.0;
                double userHs3_9 = 0.0;
                double userTotalHours = 0.0;

                for (OvertimeRequest ot : userOts) {
                    if (ot.getWorkDate() == null) continue;
                    int d = ot.getWorkDate().getDayOfMonth();
                    double raw = ot.getRawHours() != null ? ot.getRawHours() : 0.0;
                    dayHoursMap.put(d, dayHoursMap.getOrDefault(d, 0.0) + raw);

                    double factor = ot.getFactor() != null ? ot.getFactor() : 1.5;
                    if (Math.abs(factor - 1.5) < 0.01) userHs1_5 += raw;
                    else if (Math.abs(factor - 2.1) < 0.01) userHs2_1 += raw;
                    else if (Math.abs(factor - 2.0) < 0.01) userHs2_0 += raw;
                    else if (Math.abs(factor - 2.7) < 0.01) userHs2_7 += raw;
                    else if (Math.abs(factor - 3.0) < 0.01) userHs3_0 += raw;
                    else if (Math.abs(factor - 3.9) < 0.01) userHs3_9 += raw;
                    else userHs1_5 += raw;

                    userTotalHours += raw;
                }

                // Fill day cells
                for (int d = 1; d <= numDays; d++) {
                    int colIdx = 4 + d;
                    Cell cell = row.createCell(colIdx);
                    cell.setCellStyle(styleDataNum);
                    Double val = dayHoursMap.get(d);
                    if (val != null && val > 0) {
                        cell.setCellValue(formatHours(val));
                        colSums[d] += val;
                    }
                }

                // Fill breakdown summary columns
                row.createCell(sumStartCol + 0).setCellValue(userHs1_5 > 0 ? formatHours(userHs1_5) : 0);
                row.createCell(sumStartCol + 1).setCellValue(userHs2_1 > 0 ? formatHours(userHs2_1) : 0);
                row.createCell(sumStartCol + 2).setCellValue(userHs2_0 > 0 ? formatHours(userHs2_0) : 0);
                row.createCell(sumStartCol + 3).setCellValue(userHs2_7 > 0 ? formatHours(userHs2_7) : 0);
                row.createCell(sumStartCol + 4).setCellValue(userHs3_0 > 0 ? formatHours(userHs3_0) : 0);
                row.createCell(sumStartCol + 5).setCellValue(userHs3_9 > 0 ? formatHours(userHs3_9) : 0);

                Cell cellTotal = row.createCell(sumStartCol + 6);
                cellTotal.setCellValue(formatHours(userTotalHours));
                cellTotal.setCellStyle(styleDataTotalCol);

                for (int i = 0; i < 6; i++) {
                    row.getCell(sumStartCol + i).setCellStyle(styleDataNum);
                }

                sumHs1_5 += userHs1_5;
                sumHs2_1 += userHs2_1;
                sumHs2_0 += userHs2_0;
                sumHs2_7 += userHs2_7;
                sumHs3_0 += userHs3_0;
                sumHs3_9 += userHs3_9;
                grandTotal += userTotalHours;
            }

            // ── Row Tổng cộng ──
            Row rSummary = sheet.createRow(dataRowIdx++);
            for (int c = 0; c < totalCols; c++) {
                Cell cell = rSummary.createCell(c);
                cell.setCellStyle(styleSummaryRow);
            }
            rSummary.getCell(0).setCellValue("Tổng");
            sheet.addMergedRegion(new CellRangeAddress(dataRowIdx - 1, dataRowIdx - 1, 0, 4));

            for (int d = 1; d <= numDays; d++) {
                int colIdx = 4 + d;
                rSummary.getCell(colIdx).setCellValue(colSums[d] > 0 ? formatHours(colSums[d]) : 0);
            }

            rSummary.getCell(sumStartCol + 0).setCellValue(formatHours(sumHs1_5));
            rSummary.getCell(sumStartCol + 1).setCellValue(formatHours(sumHs2_1));
            rSummary.getCell(sumStartCol + 2).setCellValue(formatHours(sumHs2_0));
            rSummary.getCell(sumStartCol + 3).setCellValue(formatHours(sumHs2_7));
            rSummary.getCell(sumStartCol + 4).setCellValue(formatHours(sumHs3_0));
            rSummary.getCell(sumStartCol + 5).setCellValue(formatHours(sumHs3_9));
            rSummary.getCell(sumStartCol + 6).setCellValue(formatHours(grandTotal));

            // ── Footer Signatures ──
            int signRowIdx = dataRowIdx + 2;
            Row rSignTitle = sheet.createRow(signRowIdx);
            XSSFCellStyle styleSign = createTextStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

            // Columns distribution for signatures
            rSignTitle.createCell(2).setCellValue("PHÒNG CHÍNH TRỊ NHÂN SỰ");
            rSignTitle.getCell(2).setCellStyle(styleSign);

            int colTTPM1 = Math.min(totalCols - 18, 10);
            rSignTitle.createCell(colTTPM1).setCellValue("TTPM TÀI CHÍNH SỐ");
            rSignTitle.getCell(colTTPM1).setCellStyle(styleSign);

            int colTTPM2 = Math.min(totalCols - 12, 18);
            rSignTitle.createCell(colTTPM2).setCellValue("TTPM QUẢN TRỊ");
            rSignTitle.getCell(colTTPM2).setCellStyle(styleSign);

            int colTTPM3 = Math.min(totalCols - 6, 26);
            rSignTitle.createCell(colTTPM3).setCellValue("TTPM VIỄN THÔNG");
            rSignTitle.getCell(colTTPM3).setCellStyle(styleSign);

            // Set column widths
            sheet.setColumnWidth(0, 5 * 256);  // STT
            sheet.setColumnWidth(1, 11 * 256); // MNV
            sheet.setColumnWidth(2, 22 * 256); // Họ và tên
            sheet.setColumnWidth(3, 18 * 256); // Phòng/ban
            sheet.setColumnWidth(4, 13 * 256); // Chức danh

            for (int d = 1; d <= numDays; d++) {
                sheet.setColumnWidth(4 + d, 5 * 256); // Days columns
            }

            for (int i = 0; i < 6; i++) {
                sheet.setColumnWidth(sumStartCol + i, 11 * 256);
            }
            sheet.setColumnWidth(sumStartCol + 6, 14 * 256); // Tổng số giờ OT

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting Phu Luc 02 report", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất Phụ lục 02 Bảng tổng hợp OT: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FILE 2: PHỤ LỤC 03 — THỜI GIAN LÀM THÊM GIỜ CỦA CBNV THEO DỰ ÁN (Ảnh 2)
    // ══════════════════════════════════════════════════════════════════════════
    public byte[] exportPhuLuc03DetailByProject(
            String status, String project, Integer month, Integer year, LocalDate fromDate, LocalDate toDate, String keyword) {

        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null && year >= 2000) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());

        YearMonth ym = YearMonth.of(targetYear, targetMonth);
        LocalDate startOfMonth = (fromDate != null) ? fromDate : ym.atDay(1);
        LocalDate endOfMonth = (toDate != null) ? toDate : ym.atEndOfMonth();

        // Lấy danh sách OT
        Specification<OvertimeRequest> spec = OvertimeSpecification.filter(
                null,
                (status != null && !status.trim().isEmpty()) ? status : null,
                project,
                startOfMonth,
                endOfMonth,
                keyword
        );
        List<OvertimeRequest> otList = overtimeRepository.findAll(spec);

        // Load thông tin User
        Set<Integer> userIds = otList.stream().map(OvertimeRequest::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, User> userCache = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> userEntities = userRepository.findAllById(userIds);
            for (User u : userEntities) {
                userCache.put(u.getId(), u);
            }
        }

        // Nhóm OT theo Dự án
        Map<String, List<OvertimeRequest>> projectOtMap = new LinkedHashMap<>();
        for (OvertimeRequest ot : otList) {
            String pName = (ot.getProject() != null && !ot.getProject().trim().isEmpty()) ? ot.getProject().trim() : "Dự án khác";
            projectOtMap.computeIfAbsent(pName, k -> new ArrayList<>()).add(ot);
        }

        // Sort danh sách OT trong mỗi dự án theo ngày và thời gian bắt đầu
        for (List<OvertimeRequest> pList : projectOtMap.values()) {
            pList.sort(Comparator.comparing(OvertimeRequest::getWorkDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(OvertimeRequest::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PL03_ChiTietOT");
            sheet.setDisplayGridlines(true);

            // ── Colors ──
            byte[] grayHeaderRgb = new byte[]{(byte) 242, (byte) 242, (byte) 242}; // #F2F2F2
            byte[] yellowDataRgb = new byte[]{(byte) 255, (byte) 255, (byte) 0};   // #FFFF00 (Vàng theo mẫu)

            // ── Styles ──
            XSSFCellStyle styleCompany = createTextStyle(workbook, 10, true, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);
            XSSFCellStyle styleDept    = createTextStyle(workbook, 10, true, true, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

            XSSFCellStyle styleNationalTop = createTextStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            XSSFCellStyle styleNationalSub = createTextStyle(workbook, 10, true, true, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

            XSSFCellStyle styleMainTitle = createTextStyle(workbook, 14, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);

            XSSFCellStyle styleProjectSectionTitle = createTextStyle(workbook, 13, true, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER);

            XSSFCellStyle styleTableHeader = createColorStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, grayHeaderRgb, true);

            XSSFCellStyle styleSubHeaderMonth = createBorderedStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);
            XSSFCellStyle styleSubHeaderSum   = createBorderedStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);

            XSSFCellStyle styleDataYellowCenter = createColorStyle(workbook, 10, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, yellowDataRgb, true);
            XSSFCellStyle styleDataYellowLeft   = createColorStyle(workbook, 10, false, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, yellowDataRgb, true);
            XSSFCellStyle styleDataEmptyBorder  = createBorderedStyle(workbook, 10, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, null);

            int totalCols = 13;

            // ── Row 0 & 1: Header Công ty & Quốc hiệu ──
            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("CÔNG TY ĐẦU TƯ CÔNG NGHỆ VIETTEL");
            c0.setCellStyle(styleCompany);

            Cell c0Right = r0.createCell(7);
            c0Right.setCellValue("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
            c0Right.setCellStyle(styleNationalTop);

            Row r1 = sheet.createRow(1);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("PHÒNG CHÍNH TRỊ NHÂN SỰ");
            c1.setCellStyle(styleDept);

            Cell c1Right = r1.createCell(7);
            c1Right.setCellValue("Độc lập – Tự do – Hạnh phúc");
            c1Right.setCellStyle(styleNationalSub);

            // ── Row 3: Title ──
            Row r3 = sheet.createRow(3);
            Cell cTitle = r3.createCell(0);
            cTitle.setCellValue("PHỤ LỤC 03: THỜI GIAN LÀM THÊM GIỜ CỦA CBNV");
            cTitle.setCellStyle(styleMainTitle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, totalCols - 1));

            String[] headers = {
                "STT", "MNV", "Họ và tên", "Phòng ban", "Chức danh",
                "Dự án", "Thứ", "Ngày", "Giờ", "Thời gian\nOT",
                "Số MM OT", "Số MM khách hàng\nghi nhận", "Ghi chú"
            };

            int curRowIdx = 5;
            int projectIndex = 1;

            if (projectOtMap.isEmpty()) {
                // Nếu chưa có OT nào, vẽ khung rỗng mẫu của dự án 1
                projectOtMap.put(project != null && !project.trim().isEmpty() ? project.trim() : "BU01.VTS.VHKT.OSDC", Collections.emptyList());
            }

            for (Map.Entry<String, List<OvertimeRequest>> entry : projectOtMap.entrySet()) {
                String projectName = entry.getKey();
                List<OvertimeRequest> pOts = entry.getValue();

                // 1. Tiêu đề dự án: "1. Dự án BU01.VTS.VHKT.OSDC"
                Row rProj = sheet.createRow(curRowIdx++);
                Cell cProj = rProj.createCell(0);
                cProj.setCellValue(String.format("%d. Dự án %s", projectIndex++, projectName));
                cProj.setCellStyle(styleProjectSectionTitle);

                // 2. Table Header
                Row rHeader = sheet.createRow(curRowIdx++);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = rHeader.createCell(c);
                    cell.setCellValue(headers[c]);
                    cell.setCellStyle(styleTableHeader);
                }

                // 3. Sub-header (THÁNG MM/YYYY + SUM OT)
                Row rSub = sheet.createRow(curRowIdx++);
                for (int c = 0; c < totalCols; c++) {
                    Cell cell = rSub.createCell(c);
                    cell.setCellStyle(styleSubHeaderMonth);
                }

                rSub.getCell(0).setCellValue(String.format("THÁNG %02d/%d", targetMonth, targetYear));
                sheet.addMergedRegion(new CellRangeAddress(curRowIdx - 1, curRowIdx - 1, 0, 5));

                // Tính tổng thời gian OT của dự án
                double projectTotalHours = pOts.stream()
                        .mapToDouble(o -> o.getRawHours() != null ? o.getRawHours() : 0.0)
                        .sum();

                Cell cProjSum = rSub.getCell(9);
                cProjSum.setCellValue(formatHours(projectTotalHours));
                cProjSum.setCellStyle(styleSubHeaderSum);

                // 4. Data Rows
                int pStt = 1;
                for (OvertimeRequest ot : pOts) {
                    User user = userCache.get(ot.getUserId());
                    Row row = sheet.createRow(curRowIdx++);

                    for (int c = 0; c < totalCols; c++) {
                        Cell cell = row.createCell(c);
                        cell.setCellStyle(styleDataYellowCenter);
                    }

                    String empCode = user != null && user.getEmployeeCode() != null ? user.getEmployeeCode() : (ot.getUser() != null ? ot.getUser().getEmployeeCode() : "");
                    String fullName = user != null && user.getFullName() != null ? user.getFullName() : (ot.getUser() != null ? ot.getUser().getFullName() : "");
                    String department = getDepartmentName(user);
                    String position = getPositionName(user);

                    String dowStr = "";
                    String dateStr = "";
                    if (ot.getWorkDate() != null) {
                        dowStr = getVietnameseDayOfWeek(ot.getWorkDate());
                        dateStr = ot.getWorkDate().format(DATE_FORMATTER);
                    }

                    String timeRange = formatTimeRange(ot.getStartTime(), ot.getEndTime());
                    double raw = ot.getRawHours() != null ? ot.getRawHours() : 0.0;
                    String reason = ot.getReason() != null ? ot.getReason() : "";

                    row.getCell(0).setCellValue(pStt++);
                    row.getCell(1).setCellValue(empCode);
                    row.getCell(2).setCellValue(fullName);
                    row.getCell(2).setCellStyle(styleDataYellowLeft);

                    row.getCell(3).setCellValue(department);
                    row.getCell(4).setCellValue(position);
                    row.getCell(5).setCellValue(projectName);
                    row.getCell(5).setCellStyle(styleDataYellowLeft);

                    row.getCell(6).setCellValue(dowStr);
                    row.getCell(7).setCellValue(dateStr);
                    row.getCell(8).setCellValue(timeRange);
                    row.getCell(9).setCellValue(formatHours(raw));

                    // Số MM OT (Quy đổi nếu cần, tạm thời để trống hoặc tính raw/168)
                    row.getCell(10).setCellValue("");
                    row.getCell(11).setCellValue("");
                    row.getCell(12).setCellValue(reason);
                    row.getCell(12).setCellStyle(styleDataYellowLeft);
                }

                // Cách 2 dòng trống trước dự án tiếp theo
                curRowIdx += 2;
            }

            // ── Footer Signature ──
            Row rSign = sheet.createRow(curRowIdx);
            XSSFCellStyle styleSign = createTextStyle(workbook, 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
            rSign.createCell(0).setCellValue("TTPM TÀI CHÍNH SỐ");
            rSign.getCell(0).setCellStyle(styleSign);

            // Set column widths
            sheet.setColumnWidth(0, 6 * 256);   // STT
            sheet.setColumnWidth(1, 12 * 256);  // MNV
            sheet.setColumnWidth(2, 24 * 256);  // Họ và tên
            sheet.setColumnWidth(3, 16 * 256);  // Phòng ban
            sheet.setColumnWidth(4, 14 * 256);  // Chức danh
            sheet.setColumnWidth(5, 24 * 256);  // Dự án
            sheet.setColumnWidth(6, 8 * 256);   // Thứ
            sheet.setColumnWidth(7, 14 * 256);  // Ngày
            sheet.setColumnWidth(8, 18 * 256);  // Giờ
            sheet.setColumnWidth(9, 14 * 256);  // Thời gian OT
            sheet.setColumnWidth(10, 12 * 256); // Số MM OT
            sheet.setColumnWidth(11, 20 * 256); // Số MM KH ghi nhận
            sheet.setColumnWidth(12, 26 * 256); // Ghi chú

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error exporting Phu Luc 03 report", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất Phụ lục 03 Chi tiết OT: " + e.getMessage());
        }
    }

    // ─── Helper Methods for POI Styles ───

    private XSSFCellStyle createTextStyle(XSSFWorkbook wb, int fontSize, boolean bold, boolean underline,
                                          HorizontalAlignment hAlign, VerticalAlignment vAlign) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        if (underline) font.setUnderline(FontUnderline.SINGLE);
        style.setFont(font);
        style.setAlignment(hAlign);
        style.setVerticalAlignment(vAlign);
        return style;
    }

    private XSSFCellStyle createBorderedStyle(XSSFWorkbook wb, int fontSize, boolean bold, boolean underline,
                                              HorizontalAlignment hAlign, VerticalAlignment vAlign, byte[] rgbBg) {
        XSSFCellStyle style = createTextStyle(wb, fontSize, bold, underline, hAlign, vAlign);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setWrapText(true);

        if (rgbBg != null) {
            style.setFillForegroundColor(new XSSFColor(rgbBg, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private XSSFCellStyle createColorStyle(XSSFWorkbook wb, int fontSize, boolean bold, boolean underline,
                                           HorizontalAlignment hAlign, VerticalAlignment vAlign, byte[] rgbBg, boolean bordered) {
        XSSFCellStyle style = bordered
                ? createBorderedStyle(wb, fontSize, bold, underline, hAlign, vAlign, rgbBg)
                : createTextStyle(wb, fontSize, bold, underline, hAlign, vAlign);
        if (rgbBg != null) {
            style.setFillForegroundColor(new XSSFColor(rgbBg, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private String getDepartmentName(User user) {
        if (user == null) return "TTPMQT";
        if (user.getBorrowCenter() != null && !user.getBorrowCenter().trim().isEmpty()) {
            return user.getBorrowCenter().trim();
        }
        if (user.getStaffCategory() != null && !user.getStaffCategory().trim().isEmpty() && !"NS trung tâm".equalsIgnoreCase(user.getStaffCategory())) {
            return user.getStaffCategory().trim();
        }
        return "TTPMQT";
    }

    private String getPositionName(User user) {
        if (user == null) return "DEV";
        if (user.getPositionRel() != null && user.getPositionRel().getName() != null) {
            return user.getPositionRel().getName();
        }
        if (user.getPosition() != null && !user.getPosition().trim().isEmpty()) {
            return user.getPosition().trim();
        }
        return "DEV";
    }

    private String getVietnameseDayOfWeek(LocalDate date) {
        if (date == null) return "";
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY) return "CN";
        return String.valueOf(dow.getValue() + 1); // Monday is 2, Tuesday is 3...
    }

    private String formatTimeRange(String start, String end) {
        String s = start != null ? start.trim() : "";
        String e = end != null ? end.trim() : "";
        if (s.isEmpty() && e.isEmpty()) return "";
        return (s.replace(":", "h") + " - " + e.replace(":", "h"));
    }

    public byte[] exportZipBundle(
            String status, String project, Integer month, Integer year, LocalDate fromDate, LocalDate toDate, String keyword) {
        int targetMonth = (month != null && month >= 1 && month <= 12) ? month : (fromDate != null ? fromDate.getMonthValue() : LocalDate.now().getMonthValue());
        int targetYear = (year != null && year >= 2000) ? year : (fromDate != null ? fromDate.getYear() : LocalDate.now().getYear());

        byte[] pl02Bytes = exportPhuLuc02Summary(status, project, month, year, fromDate, toDate, keyword);
        byte[] pl03Bytes = exportPhuLuc03DetailByProject(status, project, month, year, fromDate, toDate, keyword);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {

            String pl02Name = String.format("Phu_Luc_02_Bang_Tong_Hop_Cong_OT_T%02d_%d.xlsx", targetMonth, targetYear);
            java.util.zip.ZipEntry entry02 = new java.util.zip.ZipEntry(pl02Name);
            zos.putNextEntry(entry02);
            zos.write(pl02Bytes);
            zos.closeEntry();

            String pl03Name = String.format("Phu_Luc_03_Thoi_Gian_Lam_Them_Gio_CBNV_T%02d_%d.xlsx", targetMonth, targetYear);
            java.util.zip.ZipEntry entry03 = new java.util.zip.ZipEntry(pl03Name);
            zos.putNextEntry(entry03);
            zos.write(pl03Bytes);
            zos.closeEntry();

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error creating OT export zip bundle: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi nén file báo cáo OT: " + e.getMessage());
        }
    }

    private double formatHours(double hours) {
        return Math.round(hours * 10.0) / 10.0;
    }
}

