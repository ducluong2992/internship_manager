package com.qlnv.modules.user.service;

import com.qlnv.common.config.CustomByteArrayMultipartFile;
import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.auth.entity.Account;
import com.qlnv.modules.auth.repository.AccountRepository;
import com.qlnv.modules.user.dto.AdminAccountRow;
import com.qlnv.modules.user.dto.ConfirmImportRequest;
import com.qlnv.modules.user.dto.ImportResultDto;
import com.qlnv.modules.user.dto.ReconcilePreviewDto;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Row headerRow = findHeaderRow(sheet);
            if (headerRow == null) {
                throw new ApiException("File Excel không có dữ liệu tiêu đề");
            }

            // Read headers
            Map<String, Integer> colIndexMap = buildColIndexMap(headerRow);

            int imported = 0;
            int updated = 0;
            int skipped = 0;
            int startRow = headerRow.getRowNum() + 1;
            int lastRow = sheet.getLastRowNum();

            for (int r = startRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                int rowNum = r + 1;

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

                    String rawWorkingStatus = getCellByColName(row, colIndexMap,
                            "trạng thái", "trạng thái làm việc", "trạng thái tts",
                            "tình trạng", "tình trạng làm việc", "tình trạng tts", "tình trạng hiện tại",
                            "hiện trạng", "status", "working status", "working_status");
                    if (!rawWorkingStatus.isEmpty()) {
                        user.setWorkingStatus(normalizeWorkingStatus(rawWorkingStatus));
                    }

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

                    boolean isTts = "intern".equalsIgnoreCase(userType) || "tts".equalsIgnoreCase(userType)
                            || "intern".equalsIgnoreCase(user.getUserType()) || "tts".equalsIgnoreCase(user.getUserType())
                            || (empCode != null && empCode.trim().toUpperCase().startsWith("TTS"));
                    if (isNew && !isTts && accountRepository.findByUserId(user.getId()).isEmpty()) {
                        String username = (user.getViettelEmail() != null && !user.getViettelEmail().trim().isEmpty())
                                ? user.getViettelEmail().trim().toLowerCase()
                                : empCode;
                        Account acc = Account.builder()
                                .userId(user.getId())
                                .username(username)
                                .password(passwordEncoder.encode("123456"))
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

    public Workbook downloadWorkbookFromUrl(String urlStr) {
        try {
            String exportUrl = urlStr;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/d/([a-zA-Z0-9-_]+)");
            java.util.regex.Matcher matcher = pattern.matcher(urlStr);
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể tải hoặc đọc dữ liệu từ link. Hãy chắc chắn link đã được chia sẻ công khai 'Bất kỳ ai có liên kết'. Lỗi: " + e.getMessage());
        }
    }

    public ReconcilePreviewDto previewInternImportLink(String url) {
        try (Workbook workbook = downloadWorkbookFromUrl(url)) {
            return previewInternImportWorkbook(workbook);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error previewing intern import link: {}", url, e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đối chiếu dữ liệu: " + e.getMessage());
        }
    }

    public ReconcilePreviewDto previewInternImportFile(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            return previewInternImportWorkbook(workbook);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error previewing intern import file", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Excel: " + e.getMessage());
        }
    }

    public ReconcilePreviewDto previewEmployeeImportLink(String url) {
        try (Workbook workbook = downloadWorkbookFromUrl(url)) {
            return previewEmployeeImportWorkbook(workbook);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error previewing employee import link: {}", url, e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đối chiếu dữ liệu: " + e.getMessage());
        }
    }

    public ReconcilePreviewDto previewEmployeeImportFile(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            return previewEmployeeImportWorkbook(workbook);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Error previewing employee import file", e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file Excel: " + e.getMessage());
        }
    }

    public ReconcilePreviewDto previewInternImportWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = findHeaderRow(sheet);

        if (headerRow == null) {
            throw new ApiException("File hoặc link Google Sheet không có dữ liệu tiêu đề");
        }

        Map<String, Integer> colIndexMap = buildColIndexMap(headerRow);

        List<User> dbInterns = userRepository.findByUserType("intern");
        Map<String, User> dbByCode = new HashMap<>();
        Map<String, User> dbByName = new HashMap<>();
        for (User u : dbInterns) {
            if (u.getEmployeeCode() != null) dbByCode.put(u.getEmployeeCode().trim().toLowerCase(), u);
            if (u.getFullName() != null) dbByName.put(u.getFullName().trim().toLowerCase(), u);
        }

        List<Map<String, Object>> updated = new ArrayList<>();
        List<Map<String, Object>> added = new ArrayList<>();
        Set<Integer> processedDbIds = new HashSet<>();
        List<String> formatWarnings = new ArrayList<>();
        int unchangedCount = 0;
        int startRow = headerRow.getRowNum() + 1;
        int lastRow = sheet.getLastRowNum();

        for (int r = startRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int rowNum = r + 1;

            String empCode = getCellByColName(row, colIndexMap, "mã", "mã tts", "mã nv", "mã tts (*)", "mã nv (*)");
            String fullName = getCellByColName(row, colIndexMap, "họ và tên", "họ tên", "họ và tên (*)");

            if (empCode.isEmpty() && fullName.isEmpty()) continue;

            String position = getCellByColName(row, colIndexMap, "vị trí", "vị trí / chức danh", "chức danh", "role");
            String gender = getCellByColName(row, colIndexMap, "giới tính", "gender");
            String ethnicity = getCellByColName(row, colIndexMap, "dân tộc", "ethnicity");
            String email = getCellByColName(row, colIndexMap, "email viettel", "email");
            String birthdayStr = getCellByColName(row, colIndexMap, "ngày sinh", "ngày sinh (yyyy-mm-dd)", "birthday");
            LocalDate birthday = parseDate(birthdayStr, row, colIndexMap, "ngày sinh");
            if (birthday == null && !birthdayStr.isEmpty()) {
                formatWarnings.add("Dòng " + rowNum + " (" + fullName + "): Ngày sinh '" + birthdayStr + "' không đúng định dạng yyyy-MM-dd");
            }

            String hometown = getCellByColName(row, colIndexMap, "quê quán", "hometown");
            String phone = getCellByColName(row, colIndexMap, "số điện thoại", "sđt", "phone");
            String cccd = getCellByColName(row, colIndexMap, "cccd", "số cccd", "cmnd");
            String bankName = getCellByColName(row, colIndexMap, "tên ngân hàng", "ngân hàng", "bank_name");
            String bankAccount = getCellByColName(row, colIndexMap, "số tài khoản", "stk", "bank_account");
            String project = getCellByColName(row, colIndexMap, "dự án", "project");
            String joinDateStr = getCellByColName(row, colIndexMap, "ngày vào", "ngày vào (yyyy-mm-dd)", "ngày tham gia", "join_date");
            LocalDate joinDate = parseDate(joinDateStr, row, colIndexMap, "ngày vào");
            if (joinDate == null && !joinDateStr.isEmpty()) {
                formatWarnings.add("Dòng " + rowNum + " (" + fullName + "): Ngày vào '" + joinDateStr + "' không đúng định dạng yyyy-MM-dd");
            }

            String allowance = getCellByColName(row, colIndexMap, "trợ cấp", "allowance");
            String employeeType = getCellByColName(row, colIndexMap, "loại tts", "loại nhân sự", "employee_type");
            String rawWorkingStatus = getCellByColName(row, colIndexMap,
                    "trạng thái", "trạng thái làm việc", "trạng thái tts",
                    "tình trạng", "tình trạng làm việc", "tình trạng tts", "tình trạng hiện tại",
                    "hiện trạng", "status", "working status", "working_status");
            String workingStatus = !rawWorkingStatus.isEmpty() ? normalizeWorkingStatus(rawWorkingStatus) : "";
            String employmentType = getCellByColName(row, colIndexMap, "hình thức làm việc", "employment_type");

            Map<String, Object> sheetData = new HashMap<>();
            sheetData.put("employee_code", empCode);
            sheetData.put("full_name", fullName);
            if (!position.isEmpty()) sheetData.put("position", position);
            if (!gender.isEmpty()) sheetData.put("gender", gender);
            if (!ethnicity.isEmpty()) sheetData.put("ethnicity", ethnicity);
            if (!email.isEmpty()) sheetData.put("viettel_email", email);
            if (birthday != null) sheetData.put("birthday", birthday.toString());
            if (!hometown.isEmpty()) sheetData.put("hometown", hometown);
            if (!phone.isEmpty()) sheetData.put("phone", phone);
            if (!cccd.isEmpty()) sheetData.put("cccd", cccd);
            if (!bankName.isEmpty()) sheetData.put("bank_name", bankName);
            if (!bankAccount.isEmpty()) sheetData.put("bank_account", bankAccount);
            if (!project.isEmpty()) sheetData.put("project", project);
            if (joinDate != null) sheetData.put("join_date", joinDate.toString());
            if (!allowance.isEmpty()) sheetData.put("allowance", allowance);
            if (!employeeType.isEmpty()) sheetData.put("employee_type", employeeType);
            if (!workingStatus.isEmpty()) {
                sheetData.put("working_status", workingStatus);
            } else {
                sheetData.put("working_status", "Working");
            }
            if (!employmentType.isEmpty()) sheetData.put("employment_type", employmentType);

            User matched = null;
            if (!empCode.isEmpty() && dbByCode.containsKey(empCode.toLowerCase())) {
                matched = dbByCode.get(empCode.toLowerCase());
            } else if (!fullName.isEmpty() && dbByName.containsKey(fullName.toLowerCase())) {
                matched = dbByName.get(fullName.toLowerCase());
            }

            if (matched != null) {
                processedDbIds.add(matched.getId());
                List<Map<String, String>> changes = new ArrayList<>();

                checkFieldChange(changes, "Họ và tên", matched.getFullName(), fullName);
                checkFieldChange(changes, "Vị trí", matched.getPosition(), position);
                checkFieldChange(changes, "Giới tính", matched.getGender(), gender);
                checkFieldChange(changes, "Dân tộc", matched.getEthnicity(), ethnicity);
                checkFieldChange(changes, "Email Viettel", matched.getViettelEmail(), email);
                checkFieldChange(changes, "Ngày sinh", matched.getBirthday() != null ? matched.getBirthday().toString() : "", birthday != null ? birthday.toString() : "");
                checkFieldChange(changes, "Quê quán", matched.getHometown(), hometown);
                checkFieldChange(changes, "Số điện thoại", matched.getPhone(), phone);
                checkFieldChange(changes, "Số CCCD", matched.getCccd(), cccd);
                checkFieldChange(changes, "Ngân hàng", matched.getBankName(), bankName);
                checkFieldChange(changes, "Số tài khoản", matched.getBankAccount(), bankAccount);
                checkFieldChange(changes, "Dự án", matched.getProject(), project);
                checkFieldChange(changes, "Ngày vào", matched.getJoinDate() != null ? matched.getJoinDate().toString() : "", joinDate != null ? joinDate.toString() : "");
                checkFieldChange(changes, "Trợ cấp", matched.getAllowance(), allowance);
                checkFieldChange(changes, "Loại nhân sự", matched.getEmployeeType(), employeeType);
                if (!workingStatus.isEmpty()) {
                    String oldStatusNorm = normalizeWorkingStatus(matched.getWorkingStatus());
                    String newStatusNorm = workingStatus;
                    if (!oldStatusNorm.equalsIgnoreCase(newStatusNorm)) {
                        checkFieldChange(changes, "Trạng thái", formatWorkingStatusDisplay(oldStatusNorm), formatWorkingStatusDisplay(newStatusNorm));
                    }
                }
                checkFieldChange(changes, "Hình thức", matched.getEmploymentType(), employmentType);

                if (!changes.isEmpty()) {
                    Map<String, Object> updateItem = new HashMap<>();
                    updateItem.put("id", matched.getId());
                    updateItem.put("employee_code", matched.getEmployeeCode());
                    updateItem.put("full_name", matched.getFullName());
                    updateItem.put("changes", changes);
                    updateItem.put("new_data", sheetData);
                    updated.add(updateItem);
                } else {
                    unchangedCount++;
                }
            } else {
                Map<String, Object> addItem = new HashMap<>();
                addItem.put("employee_code", empCode);
                addItem.put("full_name", fullName);
                addItem.put("project", project);
                addItem.put("position", position);
                addItem.put("viettel_email", email);
                addItem.put("new_data", sheetData);
                added.add(addItem);
            }
        }

        List<Map<String, Object>> removed = new ArrayList<>();
        for (User u : dbInterns) {
            if (!processedDbIds.contains(u.getId())) {
                Map<String, Object> remItem = new HashMap<>();
                remItem.put("id", u.getId());
                remItem.put("employee_code", u.getEmployeeCode());
                remItem.put("full_name", u.getFullName());
                remItem.put("position", u.getPosition());
                remItem.put("project", u.getProject());
                removed.add(remItem);
            }
        }

        return ReconcilePreviewDto.builder()
                .updated(updated)
                .added(added)
                .removed(removed)
                .unchangedCount(unchangedCount)
                .formatWarnings(formatWarnings)
                .build();
    }

    public ReconcilePreviewDto previewEmployeeImportWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        Row headerRow = findHeaderRow(sheet);

        if (headerRow == null) {
            throw new ApiException("File hoặc link Google Sheet không có dữ liệu tiêu đề");
        }

        Map<String, Integer> colIndexMap = buildColIndexMap(headerRow);

        List<User> dbEmployees = userRepository.findByUserType("employee");
        Map<String, User> dbByCode = new HashMap<>();
        Map<String, User> dbByName = new HashMap<>();
        for (User u : dbEmployees) {
            if (u.getEmployeeCode() != null) dbByCode.put(u.getEmployeeCode().trim().toLowerCase(), u);
            if (u.getFullName() != null) dbByName.put(u.getFullName().trim().toLowerCase(), u);
        }

        List<Map<String, Object>> updated = new ArrayList<>();
        List<Map<String, Object>> added = new ArrayList<>();
        Set<Integer> processedDbIds = new HashSet<>();
        List<String> formatWarnings = new ArrayList<>();
        int unchangedCount = 0;
        int startRow = headerRow.getRowNum() + 1;
        int lastRow = sheet.getLastRowNum();

        for (int r = startRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int rowNum = r + 1;

            String empCode = getCellByColName(row, colIndexMap, "mã", "mã nv", "mã nv (*)", "manv", "code");
            String fullName = getCellByColName(row, colIndexMap, "họ và tên", "họ tên", "họ và tên (*)", "full_name");

            if (empCode.isEmpty() && fullName.isEmpty()) continue;

            String position = getCellByColName(row, colIndexMap, "vị trí", "vị trí / chức danh", "chức danh", "role", "position");
            String directManager = getCellByColName(row, colIndexMap, "quản lý trực tiếp", "quản lí trực tiếp", "quản lý", "quản lí", "manager", "direct_manager");
            String project = getCellByColName(row, colIndexMap, "dự án", "project");
            String email = getCellByColName(row, colIndexMap, "email viettel", "email", "viettel_email");
            String phone = getCellByColName(row, colIndexMap, "số điện thoại", "sđt", "phone");
            String cccd = getCellByColName(row, colIndexMap, "cccd", "số cccd", "cmnd");
            String bankName = getCellByColName(row, colIndexMap, "tên ngân hàng", "ngân hàng", "bank_name");
            String bankAccount = getCellByColName(row, colIndexMap, "số tài khoản", "stk", "bank_account", "số tài khoản viettel money", "viettel money");
            String birthdayStr = getCellByColName(row, colIndexMap, "ngày sinh", "ngày sinh (yyyy-mm-dd)", "birthday");
            LocalDate birthday = parseDate(birthdayStr, row, colIndexMap, "ngày sinh");
            if (birthday == null && !birthdayStr.isEmpty()) {
                formatWarnings.add("Dòng " + rowNum + " (" + fullName + "): Ngày sinh '" + birthdayStr + "' không đúng định dạng ngày tháng");
            }
            String hometown = getCellByColName(row, colIndexMap, "quê quán", "hometown");
            String gender = getCellByColName(row, colIndexMap, "giới tính", "gender");
            String ethnicity = getCellByColName(row, colIndexMap, "dân tộc", "ethnicity");
            String joinDateStr = getCellByColName(row, colIndexMap, "ngày vào", "ngày vào (yyyy-mm-dd)", "ngày tham gia", "join_date");
            LocalDate joinDate = parseDate(joinDateStr, row, colIndexMap, "ngày vào");
            if (joinDate == null && !joinDateStr.isEmpty()) {
                formatWarnings.add("Dòng " + rowNum + " (" + fullName + "): Ngày vào '" + joinDateStr + "' không đúng định dạng ngày tháng");
            }
            String staffCategory = getCellByColName(row, colIndexMap, "loại nhân sự", "loại ns", "staff_category");
            String employmentStatus = getCellByColName(row, colIndexMap, "tình trạng hđ", "tình trạng hợp đồng", "tình trạng", "employment_status");
            String workingStatus = getCellByColName(row, colIndexMap, "trạng thái", "trạng thái làm việc", "working_status");
            String useCompanyMac = getCellByColName(row, colIndexMap, "dùng mac cty", "dùng mac", "use_company_mac");
            String seatPosition = getCellByColName(row, colIndexMap, "vị trí ngồi", "chỗ ngồi", "seat_position");
            String computerSerial = getCellByColName(row, colIndexMap, "seri máy tính", "serial máy tính", "computer_serial", "seri máy tính chính chủ", "serial");

            Map<String, Object> sheetData = new HashMap<>();
            sheetData.put("employee_code", empCode);
            sheetData.put("full_name", fullName);
            if (!position.isEmpty()) sheetData.put("position", position);
            if (!directManager.isEmpty()) sheetData.put("direct_manager", directManager);
            if (!project.isEmpty()) sheetData.put("project", project);
            if (!email.isEmpty()) sheetData.put("viettel_email", email);
            if (!phone.isEmpty()) sheetData.put("phone", phone);
            if (!cccd.isEmpty()) sheetData.put("cccd", cccd);
            if (!bankName.isEmpty()) sheetData.put("bank_name", bankName);
            if (!bankAccount.isEmpty()) sheetData.put("bank_account", bankAccount);
            if (birthday != null) sheetData.put("birthday", birthday.toString());
            if (!hometown.isEmpty()) sheetData.put("hometown", hometown);
            if (!gender.isEmpty()) sheetData.put("gender", gender);
            if (!ethnicity.isEmpty()) sheetData.put("ethnicity", ethnicity);
            if (joinDate != null) sheetData.put("join_date", joinDate.toString());
            if (!staffCategory.isEmpty()) sheetData.put("staff_category", staffCategory);
            if (!employmentStatus.isEmpty()) sheetData.put("employment_status", employmentStatus);
            if (!workingStatus.isEmpty()) sheetData.put("working_status", workingStatus);
            if (!useCompanyMac.isEmpty()) sheetData.put("use_company_mac", useCompanyMac);
            if (!seatPosition.isEmpty()) sheetData.put("seat_position", seatPosition);
            if (!computerSerial.isEmpty()) sheetData.put("computer_serial", computerSerial);

            User matched = null;
            if (!empCode.isEmpty() && dbByCode.containsKey(empCode.toLowerCase())) {
                matched = dbByCode.get(empCode.toLowerCase());
            } else if (!fullName.isEmpty() && dbByName.containsKey(fullName.toLowerCase())) {
                matched = dbByName.get(fullName.toLowerCase());
            }

            if (matched != null) {
                processedDbIds.add(matched.getId());
                List<Map<String, String>> changes = new ArrayList<>();

                checkFieldChange(changes, "Họ và tên", matched.getFullName(), fullName);
                checkFieldChange(changes, "Vị trí", matched.getPosition(), position);
                checkFieldChange(changes, "Quản lý trực tiếp", matched.getDirectManager(), directManager);
                checkFieldChange(changes, "Dự án", matched.getProject(), project);
                checkFieldChange(changes, "Email Viettel", matched.getViettelEmail(), email);
                checkFieldChange(changes, "Số điện thoại", matched.getPhone(), phone);
                checkFieldChange(changes, "Số CCCD", matched.getCccd(), cccd);
                checkFieldChange(changes, "Ngân hàng", matched.getBankName(), bankName);
                checkFieldChange(changes, "Số tài khoản", matched.getBankAccount(), bankAccount);
                checkFieldChange(changes, "Ngày sinh", matched.getBirthday() != null ? matched.getBirthday().toString() : "", birthday != null ? birthday.toString() : "");
                checkFieldChange(changes, "Quê quán", matched.getHometown(), hometown);
                checkFieldChange(changes, "Giới tính", matched.getGender(), gender);
                checkFieldChange(changes, "Dân tộc", matched.getEthnicity(), ethnicity);
                checkFieldChange(changes, "Ngày vào", matched.getJoinDate() != null ? matched.getJoinDate().toString() : "", joinDate != null ? joinDate.toString() : "");
                checkFieldChange(changes, "Loại nhân sự", matched.getStaffCategory(), staffCategory);
                checkFieldChange(changes, "Tình trạng HĐ", matched.getEmploymentStatus(), employmentStatus);
                checkFieldChange(changes, "Trạng thái", matched.getWorkingStatus(), workingStatus);
                checkFieldChange(changes, "Dùng Mac cty", matched.getUseCompanyMac(), useCompanyMac);
                checkFieldChange(changes, "Vị trí ngồi", matched.getSeatPosition(), seatPosition);
                checkFieldChange(changes, "Seri máy tính", matched.getComputerSerial(), computerSerial);

                if (!changes.isEmpty()) {
                    Map<String, Object> updateItem = new HashMap<>();
                    updateItem.put("id", matched.getId());
                    updateItem.put("employee_code", matched.getEmployeeCode());
                    updateItem.put("full_name", matched.getFullName());
                    updateItem.put("changes", changes);
                    updateItem.put("new_data", sheetData);
                    updated.add(updateItem);
                } else {
                    unchangedCount++;
                }
            } else {
                Map<String, Object> addItem = new HashMap<>();
                addItem.put("employee_code", empCode);
                addItem.put("full_name", fullName);
                addItem.put("project", project);
                addItem.put("position", position);
                addItem.put("viettel_email", email);
                addItem.put("new_data", sheetData);
                added.add(addItem);
            }
        }

        List<Map<String, Object>> removed = new ArrayList<>();
        for (User u : dbEmployees) {
            if (!processedDbIds.contains(u.getId())) {
                Map<String, Object> remItem = new HashMap<>();
                remItem.put("id", u.getId());
                remItem.put("employee_code", u.getEmployeeCode());
                remItem.put("full_name", u.getFullName());
                remItem.put("position", u.getPosition());
                remItem.put("project", u.getProject());
                removed.add(remItem);
            }
        }

        return ReconcilePreviewDto.builder()
                .updated(updated)
                .added(added)
                .removed(removed)
                .unchangedCount(unchangedCount)
                .formatWarnings(formatWarnings)
                .build();
    }

    private void checkFieldChange(List<Map<String, String>> changes, String label, String oldVal, String newVal) {
        if (newVal == null || newVal.trim().isEmpty()) return;
        String sOld = cleanNumericString(oldVal);
        String sNew = cleanNumericString(newVal);
        if (!sOld.equalsIgnoreCase(sNew)) {
            changes.add(Map.of("field_name", label, "old_value", sOld, "new_value", sNew));
        }
    }

    private String cleanNumericString(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.matches("^\\d+\\.0+$")) {
            return val.substring(0, val.indexOf('.'));
        }
        return val;
    }

    @Transactional
    public Map<String, Object> confirmInternImportLink(ConfirmImportRequest req) {
        return confirmImportByType(req, "intern");
    }

    @Transactional
    public Map<String, Object> confirmEmployeeImport(ConfirmImportRequest req) {
        return confirmImportByType(req, "employee");
    }

    private Map<String, Object> confirmImportByType(ConfirmImportRequest req, String userType) {
        int updatedCount = 0;
        int addedCount = 0;
        int deletedCount = 0;

        if (req.getUpdates() != null) {
            for (Map<String, Object> item : req.getUpdates()) {
                Integer id = item.get("id") != null ? Integer.parseInt(item.get("id").toString()) : null;
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) item.get("new_data");
                if (id == null || data == null) continue;

                Optional<User> opt = userRepository.findById(id);
                if (opt.isPresent()) {
                    User user = opt.get();
                    applyUserDataMap(user, data);
                    userRepository.save(user);

                    if ("employee".equalsIgnoreCase(userType) || "employee".equalsIgnoreCase(user.getUserType())) {
                        String username = (user.getViettelEmail() != null && !user.getViettelEmail().trim().isEmpty())
                                ? user.getViettelEmail().trim().toLowerCase()
                                : user.getEmployeeCode();
                        accountRepository.findByUserId(user.getId()).ifPresent(acc -> {
                            acc.setUsername(username);
                            accountRepository.save(acc);
                        });
                    }
                    updatedCount++;
                }
            }
        }

        if (req.getAdditions() != null) {
            for (Map<String, Object> item : req.getAdditions()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) item.get("new_data");
                if (data == null) data = item;

                String empCode = String.valueOf(data.getOrDefault("employee_code", "")).trim();
                String fullName = String.valueOf(data.getOrDefault("full_name", "")).trim();
                if (empCode.isEmpty() || fullName.isEmpty()) continue;

                Optional<User> existing = userRepository.findByEmployeeCode(empCode);
                User user;
                boolean isNew = false;
                if (existing.isPresent()) {
                    user = existing.get();
                } else {
                    user = new User();
                    user.setEmployeeCode(empCode);
                    user.setCreatedAt(LocalDateTime.now());
                    user.setUserType(userType);
                    user.setRole("user");
                    user.setAccountStatus(1);
                    isNew = true;
                }
                user.setFullName(fullName);
                applyUserDataMap(user, data);
                user = userRepository.save(user);

                // For employees, ensure they have an account (NEVER for TTS)
                boolean isTts = "intern".equalsIgnoreCase(userType) || "tts".equalsIgnoreCase(userType)
                        || "intern".equalsIgnoreCase(user.getUserType()) || "tts".equalsIgnoreCase(user.getUserType())
                        || (empCode != null && empCode.trim().toUpperCase().startsWith("TTS"));
                if (!isTts && ("employee".equalsIgnoreCase(userType) || "employee".equalsIgnoreCase(user.getUserType()))) {
                    String username = (user.getViettelEmail() != null && !user.getViettelEmail().trim().isEmpty())
                            ? user.getViettelEmail().trim().toLowerCase()
                            : empCode;
                    Optional<Account> accOpt = accountRepository.findByUserId(user.getId());
                    if (accOpt.isEmpty()) {
                        Account acc = Account.builder()
                                .userId(user.getId())
                                .username(username)
                                .password(passwordEncoder.encode("123456"))
                                .createdAt(LocalDateTime.now())
                                .build();
                        accountRepository.save(acc);
                    } else {
                        Account acc = accOpt.get();
                        acc.setUsername(username);
                        accountRepository.save(acc);
                    }
                }
                addedCount++;
            }
        }

        if (req.getDeleteIds() != null) {
            for (Integer id : req.getDeleteIds()) {
                if (id != null && userRepository.existsById(id)) {
                    accountRepository.deleteByUserId(id);
                    userRepository.deleteById(id);
                    deletedCount++;
                }
            }
        }

        String msg = String.format("Cập nhật thành công: %d sửa, %d thêm mới, %d đã xóa", updatedCount, addedCount, deletedCount);
        return Map.of(
                "message", msg,
                "updated", updatedCount,
                "added", addedCount,
                "deleted", deletedCount
        );
    }

    private void applyUserDataMap(User user, Map<String, Object> data) {
        if (data.containsKey("full_name") && data.get("full_name") != null) user.setFullName(data.get("full_name").toString());
        if (data.containsKey("position") && data.get("position") != null) {
            String pos = data.get("position").toString();
            user.setPosition(pos);
            Optional<Position> pOpt = positionRepository.findByName(pos);
            pOpt.ifPresent(p -> user.setPositionId(p.getId()));
        }
        if (data.containsKey("direct_manager") && data.get("direct_manager") != null) user.setDirectManager(data.get("direct_manager").toString());
        if (data.containsKey("gender") && data.get("gender") != null) user.setGender(data.get("gender").toString());
        if (data.containsKey("ethnicity") && data.get("ethnicity") != null) user.setEthnicity(data.get("ethnicity").toString());
        if (data.containsKey("viettel_email") && data.get("viettel_email") != null) user.setViettelEmail(data.get("viettel_email").toString());
        if (data.containsKey("birthday") && data.get("birthday") != null) {
            try { user.setBirthday(LocalDate.parse(data.get("birthday").toString())); } catch (Exception ignored) {}
        }
        if (data.containsKey("hometown") && data.get("hometown") != null) user.setHometown(data.get("hometown").toString());
        if (data.containsKey("phone") && data.get("phone") != null) user.setPhone(data.get("phone").toString());
        if (data.containsKey("cccd") && data.get("cccd") != null) user.setCccd(data.get("cccd").toString());
        if (data.containsKey("bank_name") && data.get("bank_name") != null) user.setBankName(data.get("bank_name").toString());
        if (data.containsKey("bank_account") && data.get("bank_account") != null) user.setBankAccount(data.get("bank_account").toString());
        if (data.containsKey("project") && data.get("project") != null) user.setProject(data.get("project").toString());
        if (data.containsKey("join_date") && data.get("join_date") != null) {
            try { user.setJoinDate(LocalDate.parse(data.get("join_date").toString())); } catch (Exception ignored) {}
        }
        if (data.containsKey("allowance") && data.get("allowance") != null) user.setAllowance(data.get("allowance").toString());
        if (data.containsKey("employee_type") && data.get("employee_type") != null) user.setEmployeeType(data.get("employee_type").toString());
        if (data.containsKey("working_status") && data.get("working_status") != null) {
            user.setWorkingStatus(normalizeWorkingStatus(data.get("working_status").toString()));
        }
        if (data.containsKey("employment_type") && data.get("employment_type") != null) user.setEmploymentType(data.get("employment_type").toString());
        if (data.containsKey("staff_category") && data.get("staff_category") != null) user.setStaffCategory(data.get("staff_category").toString());
        if (data.containsKey("employment_status") && data.get("employment_status") != null) user.setEmploymentStatus(data.get("employment_status").toString());
        if (data.containsKey("use_company_mac") && data.get("use_company_mac") != null) user.setUseCompanyMac(data.get("use_company_mac").toString());
        if (data.containsKey("seat_position") && data.get("seat_position") != null) user.setSeatPosition(data.get("seat_position").toString());
        if (data.containsKey("computer_serial") && data.get("computer_serial") != null) user.setComputerSerial(data.get("computer_serial").toString());
    }

    @Transactional
    public ImportResultDto importUsersFromUrl(String url, String userType) {
        try (Workbook workbook = downloadWorkbookFromUrl(url); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            workbook.write(baos);
            byte[] bytes = baos.toByteArray();
            MultipartFile multipartFile = new CustomByteArrayMultipartFile("sheet.xlsx", bytes);
            return importUsersFromExcel(multipartFile, userType);
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Lỗi import từ link: " + e.getMessage());
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
        return getCellString(cell, new HashSet<>());
    }

    private String getCellString(Cell cell, Set<String> visited) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
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
                case STRING:
                    String strVal = cell.getStringCellValue().trim();
                    if (strVal.contains("!") && strVal.matches("^(?:=)?(?:'[^']+'|[a-zA-Z0-9_\\-\\s]+)![A-Za-z]+\\d+$")) {
                        String resolved = resolveCellReference(cell.getSheet().getWorkbook(), strVal, visited);
                        if (resolved != null && !resolved.isEmpty()) return resolved;
                    }
                    return strVal;
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        String formula = cell.getCellFormula();
                        if (formula != null && !formula.isEmpty()) {
                            String resolved = resolveCellReference(cell.getSheet().getWorkbook(), formula, visited);
                            if (resolved != null && !resolved.isEmpty()) return resolved;
                        }
                    } catch (Exception ignored) {}

                    try {
                        CellType cachedType = cell.getCachedFormulaResultType();
                        switch (cachedType) {
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    Date d = cell.getDateCellValue();
                                    if (d != null) {
                                        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(d);
                                    }
                                }
                                double fnum = cell.getNumericCellValue();
                                if (fnum == Math.floor(fnum) && !Double.isInfinite(fnum)) {
                                    return java.math.BigDecimal.valueOf(fnum).toBigInteger().toString();
                                } else {
                                    return java.math.BigDecimal.valueOf(fnum).stripTrailingZeros().toPlainString();
                                }
                            case STRING:
                                String s = cell.getStringCellValue();
                                if (s != null && !s.trim().isEmpty()) {
                                    String trimmed = s.trim();
                                    if (trimmed.contains("!") && trimmed.matches("^(?:=)?(?:'[^']+'|[a-zA-Z0-9_\\-\\s]+)![A-Za-z]+\\d+$")) {
                                        String resolved = resolveCellReference(cell.getSheet().getWorkbook(), trimmed, visited);
                                        if (resolved != null && !resolved.isEmpty()) return resolved;
                                    }
                                    return trimmed;
                                }
                                break;
                            case BOOLEAN:
                                return String.valueOf(cell.getBooleanCellValue());
                            default:
                                break;
                        }
                    } catch (Exception ignored) {}
                    break;
                case BLANK:
                    return "";
                default:
                    break;
            }
        } catch (Exception ignored) {}

        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell).trim();
        if (val.contains("!") && val.matches("^(?:=)?(?:'[^']+'|[a-zA-Z0-9_\\-\\s]+)![A-Za-z]+\\d+$")) {
            String resolved = resolveCellReference(cell.getSheet().getWorkbook(), val, visited);
            if (resolved != null && !resolved.isEmpty()) return resolved;
        }
        if (val.matches("^\\d+\\.0+$")) {
            val = val.substring(0, val.indexOf('.'));
        }
        return val;
    }

    private String resolveCellReference(Workbook workbook, String refStr, Set<String> visited) {
        if (workbook == null || refStr == null) return null;
        refStr = refStr.trim();
        if (refStr.startsWith("=")) refStr = refStr.substring(1).trim();

        Matcher m = Pattern.compile("^(?:(?:'([^']+)'|([a-zA-Z0-9_\\-\\s]+))!)?([A-Za-z]+)(\\d+)$").matcher(refStr);
        if (!m.find()) return null;

        String sheetName = m.group(1) != null ? m.group(1) : m.group(2);
        String colLetters = m.group(3);
        int rowNum = Integer.parseInt(m.group(4));

        Sheet targetSheet = (sheetName != null) ? workbook.getSheet(sheetName) : null;
        if (targetSheet == null && sheetName != null) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).equalsIgnoreCase(sheetName)) {
                    targetSheet = workbook.getSheetAt(i);
                    break;
                }
            }
        }
        if (targetSheet == null) return null;

        int colIdx = 0;
        for (int i = 0; i < colLetters.length(); i++) {
            colIdx = colIdx * 26 + (Character.toUpperCase(colLetters.charAt(i)) - 'A' + 1);
        }
        colIdx = colIdx - 1;
        int rowIdx = rowNum - 1;

        String cellKey = targetSheet.getSheetName() + "!" + colIdx + "," + rowIdx;
        if (visited.contains(cellKey)) return null;
        visited.add(cellKey);

        Row row = targetSheet.getRow(rowIdx);
        if (row == null) return null;
        Cell targetCell = row.getCell(colIdx);
        if (targetCell == null) return null;

        return getCellString(targetCell, visited);
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        String s = header.trim().toLowerCase();
        s = s.replaceAll("\\([^)]*\\)", " ");
        s = s.replaceAll("\\[[^\\]]*\\]", " ");
        s = s.replaceAll("[*:\\-_/]", " ");
        s = s.replace("lí", "lý");
        return s.replaceAll("\\s+", " ").trim();
    }

    private Row findHeaderRow(Sheet sheet) {
        if (sheet == null) return null;
        for (int r = 0; r <= Math.min(15, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int matchCount = 0;
            for (Cell cell : row) {
                String norm = normalizeHeader(getCellString(cell));
                if (norm.contains("họ và tên") || norm.contains("họ tên")
                        || norm.contains("mã nv") || norm.contains("mã tts") || norm.equals("mã")
                        || norm.contains("email") || norm.equals("stt") || norm.contains("vị trí")
                        || norm.contains("role") || norm.contains("dự án") || norm.contains("tình trạng")
                        || norm.contains("trạng thái")) {
                    matchCount++;
                }
            }
            if (matchCount >= 2) {
                return row;
            }
        }
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                for (Cell cell : row) {
                    if (!getCellString(cell).trim().isEmpty()) {
                        return row;
                    }
                }
            }
        }
        return sheet.getRow(0);
    }

    public static String normalizeWorkingStatus(String val) {
        if (val == null) return "Working";
        String s = val.trim().toLowerCase();
        if (s.isEmpty()) return "Working";

        if (s.contains("nghỉ") || s.contains("nghi") || s.contains("dừng") || s.contains("dung")
                || s.contains("resigned") || s.contains("inactive") || s.contains("thôi")
                || s.contains("kết thúc") || s.contains("ket thuc")) {
            return "Resigned";
        }
        if (s.contains("chính thức") || s.contains("chinh thuc")) {
            return "Lên chính thức";
        }
        if (s.contains("chuyển") || s.contains("chuyen")) {
            return "Chuyển trung tâm";
        }
        if (s.contains("đang") || s.contains("dang") || s.contains("làm") || s.contains("lam")
                || s.contains("working") || s.contains("active") || s.contains("thực tập") || s.contains("thuc tap")) {
            return "Working";
        }
        if ("resigned".equalsIgnoreCase(s)) return "Resigned";
        return "Working";
    }

    public static String formatWorkingStatusDisplay(String status) {
        if (status == null) return "Đang làm";
        String s = status.trim().toLowerCase();
        if (s.equals("resigned") || s.contains("nghỉ") || s.contains("nghi")) return "Đã nghỉ";
        if (s.contains("chính thức") || s.contains("chinh thuc")) return "Lên chính thức";
        if (s.contains("chuyển") || s.contains("chuyen")) return "Chuyển TT";
        return "Đang làm";
    }

    private Map<String, Integer> buildColIndexMap(Row headerRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (headerRow == null) return map;
        for (Cell cell : headerRow) {
            String raw = getCellString(cell).trim().toLowerCase();
            if (raw.isEmpty()) continue;
            map.put(raw, cell.getColumnIndex());
            String norm = normalizeHeader(raw);
            if (!norm.isEmpty()) {
                map.putIfAbsent(norm, cell.getColumnIndex());
            }
        }
        return map;
    }

    private String getCellByColName(Row row, Map<String, Integer> colIndexMap, String... colNames) {
        if (row == null || colIndexMap == null) return "";
        for (String name : colNames) {
            String target = name.trim().toLowerCase();
            String normTarget = normalizeHeader(target);

            Integer idx = colIndexMap.get(target);
            if (idx == null) {
                idx = colIndexMap.get(normTarget);
            }

            if (idx == null) {
                for (Map.Entry<String, Integer> entry : colIndexMap.entrySet()) {
                    String colKey = entry.getKey();
                    String normKey = normalizeHeader(colKey);

                    if (normKey.equals(normTarget)
                            || normKey.startsWith(normTarget)
                            || normTarget.startsWith(normKey)
                            || (normTarget.length() >= 4 && normKey.contains(normTarget))
                            || (normKey.length() >= 4 && normTarget.contains(normKey))) {
                        idx = entry.getValue();
                        break;
                    }
                }
            }

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
            if (colIndexMap != null && row != null) {
                Integer idx = colIndexMap.get(colName.toLowerCase());
                if (idx == null) {
                    idx = colIndexMap.get(normalizeHeader(colName));
                }
                if (idx != null) {
                    Cell cell = row.getCell(idx);
                    if (cell != null && DateUtil.isCellDateFormatted(cell)) {
                        Date d = cell.getDateCellValue();
                        if (d != null) {
                            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        }
                    }
                }
            }
            return null;
        }

        val = val.trim();
        List<String> patterns = List.of(
                "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "yyyy/MM/dd", "dd-MM-yyyy", "d-M-yyyy",
                "dd.MM.yyyy", "d.M.yyyy", "dd/MM/yy", "d/M/yy"
        );
        for (String p : patterns) {
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {}
        }
        return null;
    }
}
