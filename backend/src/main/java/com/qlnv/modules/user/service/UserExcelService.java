package com.qlnv.modules.user.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.auth.entity.Account;
import com.qlnv.modules.auth.repository.AccountRepository;
import com.qlnv.modules.user.dto.AdminAccountRow;
import com.qlnv.modules.user.dto.ImportResultDto;
import com.qlnv.modules.user.dto.UserResponse;
import com.qlnv.modules.user.entity.Position;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.PositionRepository;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserExcelService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public byte[] generateInternImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Template_TTS");
            String[] headers = {
                "Mã TTS (*)", "Họ và tên (*)", "Giới tính", "Dân tộc", "Email Viettel",
                "Ngày sinh (yyyy-mm-dd)", "Quê quán", "Số điện thoại", "CCCD",
                "Tên ngân hàng", "Số tài khoản", "Dự án", "Vị trí", "Ngày vào (yyyy-mm-dd)",
                "Trợ cấp", "Loại TTS", "Trạng thái", "Hình thức làm việc"
            };

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Example row
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("TTS001");
            row.createCell(1).setCellValue("Nguyễn Văn A");
            row.createCell(2).setCellValue("Nam");
            row.createCell(3).setCellValue("Kinh");
            row.createCell(4).setCellValue("anv@viettel.com.vn");
            row.createCell(5).setCellValue("2002-01-15");
            row.createCell(6).setCellValue("Hà Nội");
            row.createCell(7).setCellValue("0912345678");
            row.createCell(8).setCellValue("001202000001");
            row.createCell(9).setCellValue("MB Bank");
            row.createCell(10).setCellValue("999988887777");
            row.createCell(11).setCellValue("AI Platform");
            row.createCell(12).setCellValue("Dev");
            row.createCell(13).setCellValue("2026-08-01");
            row.createCell(14).setCellValue("Có");
            row.createCell(15).setCellValue("TTS Trung tâm");
            row.createCell(16).setCellValue("Working");
            row.createCell(17).setCellValue("Fulltime");

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo template Excel: " + e.getMessage());
        }
    }

    public byte[] generateEmployeeImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Template_NhanSu");
            String[] headers = {
                "Mã NV (*)", "Họ và tên (*)", "Vị trí / Chức danh", "Quản lý trực tiếp",
                "Dự án", "Email Viettel", "Số điện thoại", "Số CCCD", "Tên ngân hàng",
                "Số tài khoản", "Ngày sinh (yyyy-mm-dd)", "Quê quán", "Giới tính",
                "Dân tộc", "Ngày vào (yyyy-mm-dd)", "Loại nhân sự", "Tình trạng HĐ",
                "Trạng thái làm việc", "Dùng Mac cty", "Vị trí ngồi", "Seri máy tính",
                "Thời hạn mượn", "Dự án mượn", "PM dự án mượn", "Trung tâm cho mượn"
            };

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }

            // Example row
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("NV001");
            row.createCell(1).setCellValue("Trần Thị B");
            row.createCell(2).setCellValue("Dev");
            row.createCell(3).setCellValue("admin");
            row.createCell(4).setCellValue("Core ERP");
            row.createCell(5).setCellValue("btt@viettel.com.vn");
            row.createCell(6).setCellValue("0987654321");
            row.createCell(7).setCellValue("001200000002");
            row.createCell(8).setCellValue("Vietcombank");
            row.createCell(9).setCellValue("001100223344");
            row.createCell(10).setCellValue("1998-05-20");
            row.createCell(11).setCellValue("Hải Phòng");
            row.createCell(12).setCellValue("Nữ");
            row.createCell(13).setCellValue("Kinh");
            row.createCell(14).setCellValue("2024-03-01");
            row.createCell(15).setCellValue("NS trung tâm");
            row.createCell(16).setCellValue("Chính thức");
            row.createCell(17).setCellValue("Working");
            row.createCell(18).setCellValue("Không");
            row.createCell(19).setCellValue("Tầng 5 - B12");
            row.createCell(20).setCellValue("VT-PC-9988");

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo template Excel: " + e.getMessage());
        }
    }

    @Transactional
    public ImportResultDto importUsersFromExcel(MultipartFile file, String userType) {
        ImportResultDto result = new ImportResultDto();
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new ApiException("File Excel không có dữ liệu");
            }

            // Read headers
            Row headerRow = rowIterator.next();
            Map<String, Integer> colIndexMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String val = getCellString(cell).trim().toLowerCase();
                colIndexMap.put(val, cell.getColumnIndex());
            }

            int imported = 0;
            int updated = 0;
            int skipped = 0;
            int rowNum = 1;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNum++;

                String empCode = getCellByColName(row, colIndexMap, "mã", "mã tts", "mã nv", "mã tts (*)", "mã nv (*)");
                String fullName = getCellByColName(row, colIndexMap, "họ và tên", "họ tên", "họ và tên (*)");

                if (empCode.isEmpty() || fullName.isEmpty()) {
                    skipped++;
                    continue;
                }

                try {
                    Optional<User> existingOpt = userRepository.findByEmployeeCode(empCode);
                    User user;
                    boolean isNew = false;
                    if (existingOpt.isPresent()) {
                        user = existingOpt.get();
                        updated++;
                    } else {
                        user = new User();
                        user.setEmployeeCode(empCode);
                        user.setCreatedAt(LocalDateTime.now());
                        user.setRole("user");
                        user.setUserType(userType);
                        user.setAccountStatus(1);
                        isNew = true;
                        imported++;
                    }

                    user.setFullName(fullName);
                    user.setUserType(userType);

                    String gender = getCellByColName(row, colIndexMap, "giới tính");
                    if (!gender.isEmpty()) user.setGender(gender);

                    String ethnicity = getCellByColName(row, colIndexMap, "dân tộc");
                    if (!ethnicity.isEmpty()) user.setEthnicity(ethnicity);

                    String email = getCellByColName(row, colIndexMap, "email viettel", "email");
                    if (!email.isEmpty()) user.setViettelEmail(email);

                    String birthdayStr = getCellByColName(row, colIndexMap, "ngày sinh", "ngày sinh (yyyy-mm-dd)");
                    LocalDate birthday = parseDate(birthdayStr, row, colIndexMap, "ngày sinh");
                    if (birthday != null) user.setBirthday(birthday);

                    String hometown = getCellByColName(row, colIndexMap, "quê quán");
                    if (!hometown.isEmpty()) user.setHometown(hometown);

                    String phone = getCellByColName(row, colIndexMap, "số điện thoại", "sđt", "phone");
                    if (!phone.isEmpty()) user.setPhone(phone);

                    String cccd = getCellByColName(row, colIndexMap, "cccd", "số cccd", "cmnd");
                    if (!cccd.isEmpty()) user.setCccd(cccd);

                    String bankName = getCellByColName(row, colIndexMap, "tên ngân hàng", "ngân hàng");
                    if (!bankName.isEmpty()) user.setBankName(bankName);

                    String bankAccount = getCellByColName(row, colIndexMap, "số tài khoản", "stk");
                    if (!bankAccount.isEmpty()) user.setBankAccount(bankAccount);

                    String project = getCellByColName(row, colIndexMap, "dự án");
                    if (!project.isEmpty()) user.setProject(project);

                    String position = getCellByColName(row, colIndexMap, "vị trí", "vị trí / chức danh", "chức danh");
                    if (!position.isEmpty()) {
                        user.setPosition(position);
                        Optional<Position> pOpt = positionRepository.findByName(position);
                        if (pOpt.isPresent()) {
                            user.setPositionId(pOpt.get().getId());
                        }
                    }

                    String joinDateStr = getCellByColName(row, colIndexMap, "ngày vào", "ngày vào (yyyy-mm-dd)", "ngày tham gia");
                    LocalDate joinDate = parseDate(joinDateStr, row, colIndexMap, "ngày vào");
                    if (joinDate != null) user.setJoinDate(joinDate);

                    String workingStatus = getCellByColName(row, colIndexMap, "trạng thái", "trạng thái làm việc");
                    if (!workingStatus.isEmpty()) user.setWorkingStatus(workingStatus);

                    if ("employee".equalsIgnoreCase(userType)) {
                        String directManager = getCellByColName(row, colIndexMap, "quản lý trực tiếp", "quản lý");
                        if (!directManager.isEmpty()) user.setDirectManager(directManager);

                        String staffCategory = getCellByColName(row, colIndexMap, "loại nhân sự");
                        if (!staffCategory.isEmpty()) user.setStaffCategory(staffCategory);

                        String employmentStatus = getCellByColName(row, colIndexMap, "tình trạng hđ", "loại hợp đồng");
                        if (!employmentStatus.isEmpty()) user.setEmploymentStatus(employmentStatus);

                        String mac = getCellByColName(row, colIndexMap, "dùng mac cty", "mac");
                        if (!mac.isEmpty()) user.setUseCompanyMac(mac);

                        String seat = getCellByColName(row, colIndexMap, "vị trí ngồi");
                        if (!seat.isEmpty()) user.setSeatPosition(seat);

                        String serial = getCellByColName(row, colIndexMap, "seri máy tính", "serial");
                        if (!serial.isEmpty()) user.setComputerSerial(serial);

                        String borrowEndStr = getCellByColName(row, colIndexMap, "thời hạn mượn");
                        LocalDate borrowEnd = parseDate(borrowEndStr, row, colIndexMap, "thời hạn mượn");
                        if (borrowEnd != null) user.setBorrowEndDate(borrowEnd);

                        String borrowProject = getCellByColName(row, colIndexMap, "dự án mượn");
                        if (!borrowProject.isEmpty()) user.setBorrowProject(borrowProject);

                        String borrowPm = getCellByColName(row, colIndexMap, "pm dự án mượn");
                        if (!borrowPm.isEmpty()) user.setBorrowPm(borrowPm);

                        String borrowCenter = getCellByColName(row, colIndexMap, "trung tâm cho mượn");
                        if (!borrowCenter.isEmpty()) user.setBorrowCenter(borrowCenter);
                    } else {
                        String allowance = getCellByColName(row, colIndexMap, "trợ cấp");
                        if (!allowance.isEmpty()) user.setAllowance(allowance);

                        String empType = getCellByColName(row, colIndexMap, "loại tts");
                        if (!empType.isEmpty()) user.setEmployeeType(empType);

                        String form = getCellByColName(row, colIndexMap, "hình thức làm việc");
                        if (!form.isEmpty()) user.setEmploymentType(form);
                    }

                    user = userRepository.save(user);

                    if (isNew && accountRepository.findByUserId(user.getId()).isEmpty()) {
                        Account acc = Account.builder()
                                .userId(user.getId())
                                .username(empCode)
                                .password(passwordEncoder.encode("User@123"))
                                .createdAt(LocalDateTime.now())
                                .build();
                        accountRepository.save(acc);
                    }

                } catch (Exception e) {
                    result.getErrors().add("Dòng " + rowNum + " (" + empCode + "): " + e.getMessage());
                }
            }

            result.setImportedCount(imported);
            result.setUpdatedCount(updated);
            result.setSkippedCount(skipped);
            return result;

        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lỗi đọc file Excel: " + e.getMessage());
        }
    }

    public byte[] exportAccountsToExcel() {
        List<AdminAccountRow> accounts = userService.getAccountsList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DanhSachTaiKhoan");
            String[] headers = { "STT", "Mã NV / TTS", "Họ và tên", "Username", "Vai trò", "Loại nhân sự", "Trạng thái tài khoản", "Trạng thái làm việc" };

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowIdx = 1;
            for (AdminAccountRow row : accounts) {
                Row r = sheet.createRow(rowIdx);
                r.createCell(0).setCellValue(rowIdx);
                r.createCell(1).setCellValue(row.getEmployeeCode() != null ? row.getEmployeeCode() : "");
                r.createCell(2).setCellValue(row.getFullName() != null ? row.getFullName() : "");
                r.createCell(3).setCellValue(row.getUsername() != null ? row.getUsername() : "");
                r.createCell(4).setCellValue("admin".equalsIgnoreCase(row.getRole()) ? "Admin" : "User");
                r.createCell(5).setCellValue("employee".equalsIgnoreCase(row.getUserType()) ? "Nhân viên" : "Thực tập sinh");
                r.createCell(6).setCellValue(row.getAccountStatus() == 1 ? "Hoạt động" : "Bị khóa");
                r.createCell(7).setCellValue("Resigned".equalsIgnoreCase(row.getWorkingStatus()) ? "Đã nghỉ việc" : "Đang làm việc");
                rowIdx++;
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất danh sách tài khoản: " + e.getMessage());
        }
    }

    public byte[] exportEmployeesToExcel(List<UserResponse> employees) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DanhSachNhanSu");
            String[] headers = {
                "STT", "Mã NV", "Họ và tên", "Vị trí / Chức danh", "Quản lý trực tiếp",
                "Dự án", "Email Viettel", "Số điện thoại", "Số CCCD", "Tên ngân hàng",
                "Số tài khoản", "Ngày sinh", "Quê quán", "Giới tính", "Dân tộc",
                "Ngày vào", "Loại nhân sự", "Tình trạng HĐ", "Trạng thái", "Dùng Mac cty",
                "Vị trí ngồi", "Seri máy tính"
            };

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }

            int rowIdx = 1;
            for (UserResponse emp : employees) {
                Row r = sheet.createRow(rowIdx);
                r.createCell(0).setCellValue(rowIdx);
                r.createCell(1).setCellValue(emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
                r.createCell(2).setCellValue(emp.getFullName() != null ? emp.getFullName() : "");
                r.createCell(3).setCellValue(emp.getPositionName() != null ? emp.getPositionName() : "");
                r.createCell(4).setCellValue(emp.getDirectManager() != null ? emp.getDirectManager() : "");
                r.createCell(5).setCellValue(emp.getProject() != null ? emp.getProject() : "");
                r.createCell(6).setCellValue(emp.getViettelEmail() != null ? emp.getViettelEmail() : "");
                r.createCell(7).setCellValue(emp.getPhone() != null ? emp.getPhone() : "");
                r.createCell(8).setCellValue(emp.getCccd() != null ? emp.getCccd() : "");
                r.createCell(9).setCellValue(emp.getBankName() != null ? emp.getBankName() : "");
                r.createCell(10).setCellValue(emp.getBankAccount() != null ? emp.getBankAccount() : "");
                r.createCell(11).setCellValue(emp.getBirthday() != null ? emp.getBirthday().toString() : "");
                r.createCell(12).setCellValue(emp.getHometown() != null ? emp.getHometown() : "");
                r.createCell(13).setCellValue(emp.getGender() != null ? emp.getGender() : "");
                r.createCell(14).setCellValue(emp.getEthnicity() != null ? emp.getEthnicity() : "");
                r.createCell(15).setCellValue(emp.getJoinDate() != null ? emp.getJoinDate().toString() : "");
                r.createCell(16).setCellValue(emp.getStaffCategory() != null ? emp.getStaffCategory() : "");
                r.createCell(17).setCellValue(emp.getEmploymentStatus() != null ? emp.getEmploymentStatus() : "");
                r.createCell(18).setCellValue(emp.getWorkingStatus() != null ? emp.getWorkingStatus() : "");
                r.createCell(19).setCellValue(emp.getUseCompanyMac() != null ? emp.getUseCompanyMac() : "");
                r.createCell(20).setCellValue(emp.getSeatPosition() != null ? emp.getSeatPosition() : "");
                r.createCell(21).setCellValue(emp.getComputerSerial() != null ? emp.getComputerSerial() : "");
                rowIdx++;
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể xuất danh sách nhân sự: " + e.getMessage());
        }
    }

    // ─── Helpers ───

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell);
    }

    private String getCellByColName(Row row, Map<String, Integer> colIndexMap, String... colNames) {
        for (String name : colNames) {
            Integer idx = colIndexMap.get(name.toLowerCase());
            if (idx != null) {
                Cell cell = row.getCell(idx);
                String val = getCellString(cell);
                if (!val.isEmpty()) return val;
            }
        }
        return "";
    }

    private LocalDate parseDate(String val, Row row, Map<String, Integer> colIndexMap, String colName) {
        if (val == null || val.trim().isEmpty()) {
            Integer idx = colIndexMap.get(colName.toLowerCase());
            if (idx != null) {
                Cell cell = row.getCell(idx);
                if (cell != null && DateUtil.isCellDateFormatted(cell)) {
                    Date d = cell.getDateCellValue();
                    if (d != null) {
                        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    }
                }
            }
            return null;
        }

        val = val.trim();
        List<String> patterns = List.of("yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "yyyy/MM/dd", "dd-MM-yyyy");
        for (String p : patterns) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return null;
    }
}
