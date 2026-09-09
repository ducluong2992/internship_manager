package com.qlnv.modules.user.service;

import com.qlnv.common.exception.ApiException;
import com.qlnv.modules.auth.entity.Account;
import com.qlnv.modules.auth.repository.AccountRepository;
import com.qlnv.modules.overtime.repository.OvertimeRequestRepository;
import com.qlnv.modules.schedule.entity.Schedule;
import com.qlnv.modules.schedule.repository.ScheduleRepository;
import com.qlnv.modules.user.dto.AdminAccountRow;
import com.qlnv.modules.user.dto.ManagerResponse;
import com.qlnv.modules.user.dto.UserRequestDto;
import com.qlnv.modules.user.dto.UserResponse;
import com.qlnv.modules.user.entity.Position;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.PositionRepository;
import com.qlnv.modules.user.repository.UserRepository;
import com.qlnv.modules.user.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;
    private final ScheduleRepository scheduleRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Single-Value Fast Query Lookups ───

    public User findEntityById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));
    }

    public User findEntityByEmployeeCode(String employeeCode) {
        return userRepository.findByEmployeeCode(employeeCode.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với mã: " + employeeCode));
    }

    public UserResponse getUserById(Integer id) {
        User user = findEntityById(id);
        return toUserResponse(user);
    }

    public UserResponse getMe(Integer userId) {
        User user = findEntityById(userId);
        return toUserResponse(user);
    }

    // ─── Range & Criteria Multi-filter Queries ───

    public List<UserResponse> queryUsers(
            String keyword,
            String userType,
            String role,
            String project,
            String workingStatus,
            String staffCategory,
            String employmentStatus,
            LocalDate joinDateFrom,
            LocalDate joinDateTo,
            LocalDate borrowEndFrom,
            LocalDate borrowEndTo) {

        Specification<User> spec = UserSpecification.filter(
                keyword, userType, role, project, workingStatus, staffCategory, employmentStatus,
                joinDateFrom, joinDateTo, borrowEndFrom, borrowEndTo
        );

        return userRepository.findAll(spec).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllInterns() {
        return queryUsers(null, "intern", null, null, null, null, null, null, null, null, null);
    }

    public List<UserResponse> getAllEmployees() {
        return queryUsers(null, "employee", null, null, null, null, null, null, null, null, null);
    }

    // ─── Create, Update, Delete Operations ───

    @Transactional
    public UserResponse createUser(UserRequestDto req, String defaultUserType) {
        String empCode = req.getEmployeeCode() != null ? req.getEmployeeCode().trim() : "";
        if (empCode.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã nhân viên / thực tập sinh không được để trống");
        }

        if (userRepository.findByEmployeeCode(empCode).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mã " + empCode + " đã tồn tại trong hệ thống");
        }

        String userType = req.getUserType() != null && !req.getUserType().isEmpty() ? req.getUserType() : defaultUserType;
        String role = req.getRole() != null && !req.getRole().isEmpty() ? req.getRole() : "user";

        // Lookup position name if position_id provided
        String positionName = req.getPosition();
        if (req.getPositionId() != null) {
            positionRepository.findById(req.getPositionId()).ifPresent(p -> {});
        }

        User user = User.builder()
                .employeeCode(empCode)
                .fullName(req.getFullName())
                .role(role)
                .userType(userType)
                .gender(req.getGender())
                .ethnicity(req.getEthnicity())
                .viettelEmail(req.getViettelEmail())
                .birthday(req.getBirthday())
                .hometown(req.getHometown())
                .phone(req.getPhone())
                .cccd(req.getCccd())
                .bankName(req.getBankName())
                .bankAccount(req.getBankAccount())
                .project(req.getProject())
                .position(positionName)
                .positionId(req.getPositionId())
                .joinDate(req.getJoinDate())
                .allowance(req.getAllowance() != null ? req.getAllowance() : "Không")
                .employeeType(req.getEmployeeType() != null ? req.getEmployeeType() : "TTS Trung tâm")
                .workingStatus(UserExcelService.normalizeWorkingStatus(req.getWorkingStatus()))
                .employmentType(req.getEmploymentType() != null ? req.getEmploymentType() : "Fulltime")
                .directManager(req.getDirectManager())
                .computerSerial(req.getComputerSerial())
                .employmentStatus(req.getEmploymentStatus() != null ? req.getEmploymentStatus() : "Thử việc")
                .useCompanyMac(req.getUseCompanyMac() != null ? req.getUseCompanyMac() : "Không")
                .staffCategory(req.getStaffCategory() != null ? req.getStaffCategory() : "NS trung tâm")
                .seatPosition(req.getSeatPosition())
                .borrowEndDate(req.getBorrowEndDate())
                .borrowProject(req.getBorrowProject())
                .borrowPm(req.getBorrowPm())
                .borrowCenter(req.getBorrowCenter())
                .accountStatus(req.getAccountStatus() != null ? req.getAccountStatus() : 1)
                .createdAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        // Create Account ONLY for non-interns (Employee / Admin)
        boolean isIntern = "intern".equalsIgnoreCase(user.getUserType())
                || "tts".equalsIgnoreCase(user.getUserType())
                || (req.getUserType() != null && ("intern".equalsIgnoreCase(req.getUserType()) || "tts".equalsIgnoreCase(req.getUserType())))
                || (user.getEmployeeCode() != null && user.getEmployeeCode().trim().toUpperCase().startsWith("TTS"));
        if (!isIntern) {
            String username = (user.getViettelEmail() != null && !user.getViettelEmail().trim().isEmpty())
                    ? user.getViettelEmail().trim().toLowerCase()
                    : empCode;
            String rawPassword = req.getPassword() != null && !req.getPassword().isEmpty() ? req.getPassword() : "123456";
            if (accountRepository.findByUserId(user.getId()).isEmpty()) {
                Account account = Account.builder()
                        .userId(user.getId())
                        .username(username)
                        .password(passwordEncoder.encode(rawPassword))
                        .createdAt(LocalDateTime.now())
                        .build();
                accountRepository.save(account);
            }
        }

        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Integer id, UserRequestDto req) {
        User user = findEntityById(id);

        if (req.getEmployeeCode() != null && !req.getEmployeeCode().trim().isEmpty()) {
            String newCode = req.getEmployeeCode().trim();
            if (!newCode.equalsIgnoreCase(user.getEmployeeCode()) && userRepository.findByEmployeeCode(newCode).isPresent()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Mã nhân viên " + newCode + " đã tồn tại");
            }
            user.setEmployeeCode(newCode);
        }

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getRole() != null) user.setRole(req.getRole());
        if (req.getUserType() != null) user.setUserType(req.getUserType());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getEthnicity() != null) user.setEthnicity(req.getEthnicity());
        if (req.getViettelEmail() != null) {
            user.setViettelEmail(req.getViettelEmail());
            boolean isIntern = "intern".equalsIgnoreCase(user.getUserType()) || "tts".equalsIgnoreCase(user.getUserType());
            if (!isIntern) {
                String newUsername = !req.getViettelEmail().trim().isEmpty() ? req.getViettelEmail().trim().toLowerCase() : user.getEmployeeCode();
                accountRepository.findByUserId(user.getId()).ifPresent(acc -> {
                    acc.setUsername(newUsername);
                    accountRepository.save(acc);
                });
            }
        }
        if (req.getBirthday() != null) user.setBirthday(req.getBirthday());
        if (req.getHometown() != null) user.setHometown(req.getHometown());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getCccd() != null) user.setCccd(req.getCccd());
        if (req.getBankName() != null) user.setBankName(req.getBankName());
        if (req.getBankAccount() != null) user.setBankAccount(req.getBankAccount());
        if (req.getProject() != null) user.setProject(req.getProject());
        if (req.getPosition() != null) user.setPosition(req.getPosition());
        if (req.getPositionId() != null) user.setPositionId(req.getPositionId());
        if (req.getJoinDate() != null) user.setJoinDate(req.getJoinDate());
        if (req.getAllowance() != null) user.setAllowance(req.getAllowance());
        if (req.getEmployeeType() != null) user.setEmployeeType(req.getEmployeeType());
        if (req.getWorkingStatus() != null) user.setWorkingStatus(UserExcelService.normalizeWorkingStatus(req.getWorkingStatus()));
        if (req.getEmploymentType() != null) user.setEmploymentType(req.getEmploymentType());
        if (req.getDirectManager() != null) user.setDirectManager(req.getDirectManager());
        if (req.getComputerSerial() != null) user.setComputerSerial(req.getComputerSerial());
        if (req.getEmploymentStatus() != null) user.setEmploymentStatus(req.getEmploymentStatus());
        if (req.getUseCompanyMac() != null) user.setUseCompanyMac(req.getUseCompanyMac());
        if (req.getStaffCategory() != null) user.setStaffCategory(req.getStaffCategory());
        if (req.getSeatPosition() != null) user.setSeatPosition(req.getSeatPosition());
        if (req.getBorrowEndDate() != null) user.setBorrowEndDate(req.getBorrowEndDate());
        if (req.getBorrowProject() != null) user.setBorrowProject(req.getBorrowProject());
        if (req.getBorrowPm() != null) user.setBorrowPm(req.getBorrowPm());
        if (req.getBorrowCenter() != null) user.setBorrowCenter(req.getBorrowCenter());
        if (req.getAccountStatus() != null) user.setAccountStatus(req.getAccountStatus());

        user = userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional
    public void deleteUser(Integer id) {
        User user = findEntityById(id);
        if ("admin".equalsIgnoreCase(user.getEmployeeCode())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Không thể xóa tài khoản admin hệ thống");
        }

        // Delete cascade relationships
        overtimeRequestRepository.deleteByUserId(id);
        scheduleRepository.deleteByUserId(id);
        accountRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    @Transactional
    public Map<String, Object> deleteAllByUserType(String userType) {
        List<User> users = userRepository.findByUserType(userType);
        int count = 0;
        for (User user : users) {
            if ("admin".equalsIgnoreCase(user.getEmployeeCode())) continue;
            if ("admin".equalsIgnoreCase(user.getRole())) continue;
            overtimeRequestRepository.deleteByUserId(user.getId());
            scheduleRepository.deleteByUserId(user.getId());
            accountRepository.deleteByUserId(user.getId());
            userRepository.delete(user);
            count++;
        }
        return Map.of("message", "Đã xóa thành công " + count + " bản ghi", "deleted_count", count);
    }

    @Transactional
    public Map<String, Object> lockUser(Integer id, Integer targetStatus) {
        User user = findEntityById(id);
        int currentStatus = user.getAccountStatus() != null ? user.getAccountStatus() : 1;
        int newStatus = (targetStatus != null) ? targetStatus : (currentStatus == 1 ? 0 : 1);

        if ("admin".equalsIgnoreCase(user.getEmployeeCode()) && newStatus == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Không thể khóa tài khoản admin");
        }

        user.setAccountStatus(newStatus);
        userRepository.save(user);

        return Map.of(
                "message", newStatus == 0 ? "Khóa tài khoản thành công" : "Mở khóa tài khoản thành công",
                "status", newStatus,
                "account_status", newStatus
        );
    }

    @Transactional
    public Map<String, Object> lockResignedAccounts() {
        List<User> resignedUsers = userRepository.findByWorkingStatus("Resigned");
        int count = 0;
        for (User u : resignedUsers) {
            if (!"admin".equalsIgnoreCase(u.getEmployeeCode()) && (u.getAccountStatus() == null || u.getAccountStatus() != 0)) {
                u.setAccountStatus(0);
                userRepository.save(u);
                count++;
            }
        }
        return Map.of("message", "Đã khóa thành công " + count + " tài khoản nhân sự đã nghỉ việc", "locked_count", count);
    }

    @Transactional
    public Map<String, String> resetPassword(Integer id, String newPassword) {
        User user = findEntityById(id);
        if ("intern".equalsIgnoreCase(user.getUserType()) || "tts".equalsIgnoreCase(user.getUserType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Thực tập sinh không có tài khoản đăng nhập");
        }
        Account account = accountRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String username = (user.getViettelEmail() != null && !user.getViettelEmail().trim().isEmpty())
                            ? user.getViettelEmail().trim().toLowerCase()
                            : user.getEmployeeCode();
                    Account acc = Account.builder()
                            .userId(user.getId())
                            .username(username)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return acc;
                });

        String pwd = (newPassword != null && !newPassword.trim().isEmpty()) ? newPassword.trim() : "123456";
        account.setPassword(passwordEncoder.encode(pwd));
        accountRepository.save(account);

        return Map.of("message", "Đặt lại mật khẩu thành công");
    }

    public List<AdminAccountRow> getAccountsList() {
        List<User> users = userRepository.findAll();
        Map<Integer, Account> accountMap = accountRepository.findAll().stream()
                .collect(Collectors.toMap(Account::getUserId, a -> a, (a1, a2) -> a1));

        return users.stream()
                .filter(u -> !"intern".equalsIgnoreCase(u.getUserType()) && !"tts".equalsIgnoreCase(u.getUserType()))
                .map(u -> {
                    Account acc = accountMap.get(u.getId());
                    String defaultUsername = (u.getViettelEmail() != null && !u.getViettelEmail().trim().isEmpty())
                            ? u.getViettelEmail().trim().toLowerCase()
                            : u.getEmployeeCode();
                    return AdminAccountRow.builder()
                            .userId(u.getId())
                            .employeeCode(u.getEmployeeCode())
                            .fullName(u.getFullName())
                            .username(acc != null ? acc.getUsername() : defaultUsername)
                            .password(acc != null ? "••••••••" : "Chưa tạo")
                            .accountStatus(u.getAccountStatus() != null ? u.getAccountStatus() : 1)
                            .role(u.getRole())
                            .workingStatus(u.getWorkingStatus())
                            .userType(u.getUserType())
                            .build();
                }).collect(Collectors.toList());
    }

    public List<ManagerResponse> getManagers() {
        // Return users with role=admin or position is manager
        List<Position> managerPositions = positionRepository.findByIsManagerTrueOrderByNameAsc();
        Set<Integer> managerPosIds = managerPositions.stream().map(Position::getId).collect(Collectors.toSet());

        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> "admin".equalsIgnoreCase(u.getRole()) ||
                             (u.getPositionId() != null && managerPosIds.contains(u.getPositionId())))
                .map(u -> ManagerResponse.builder()
                        .username(u.getEmployeeCode())
                        .fullName(u.getFullName())
                        .position(u.getPositionRel() != null ? u.getPositionRel().getName() : u.getPosition())
                        .build())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStats() {
        long totalUsers = userRepository.count();
        List<User> all = userRepository.findAll();

        List<User> interns = all.stream().filter(u -> "intern".equalsIgnoreCase(u.getUserType())).collect(Collectors.toList());
        List<User> employees = all.stream().filter(u -> "employee".equalsIgnoreCase(u.getUserType())).collect(Collectors.toList());

        long workingInterns = interns.stream().filter(u -> "working".equalsIgnoreCase(UserExcelService.normalizeWorkingStatus(u.getWorkingStatus()))).count();
        long resignedInterns = interns.stream().filter(u -> "resigned".equalsIgnoreCase(UserExcelService.normalizeWorkingStatus(u.getWorkingStatus()))).count();
        long fulltime = interns.stream().filter(u -> "fulltime".equalsIgnoreCase(u.getEmploymentType())).count();
        long parttime = interns.stream().filter(u -> "parttime".equalsIgnoreCase(u.getEmploymentType())).count();
        long internCount = interns.stream().filter(u -> {
            String et = u.getEmployeeType() != null ? u.getEmployeeType().toLowerCase() : "";
            return et.contains("tts") || et.contains("intern") || et.contains("thực tập");
        }).count();
        long borrowedCount = interns.stream().filter(u -> {
            String et = u.getEmployeeType() != null ? u.getEmployeeType().toLowerCase() : "";
            return et.contains("mượn") || et.contains("borrowed");
        }).count();

        long empTrungTam = employees.stream().filter(u -> {
            String sc = u.getStaffCategory() != null ? u.getStaffCategory().toLowerCase() : "";
            return sc.contains("trung tâm");
        }).count();
        long empChoMuon = employees.stream().filter(u -> {
            String sc = u.getStaffCategory() != null ? u.getStaffCategory().toLowerCase() : "";
            return sc.contains("mượn");
        }).count();
        long empOnsite = employees.stream().filter(u -> {
            String sc = u.getStaffCategory() != null ? u.getStaffCategory().toLowerCase() : "";
            return sc.contains("onsite");
        }).count();

        long empProbation = employees.stream().filter(u -> {
            String es = u.getEmploymentStatus() != null ? u.getEmploymentStatus().toLowerCase() : "";
            return es.contains("thử việc") || es.contains("thu viec") || es.contains("học việc") || es.contains("hoc viec");
        }).count();
        long empResigned = employees.stream().filter(u -> {
            String es = u.getEmploymentStatus() != null ? u.getEmploymentStatus().toLowerCase() : "";
            String ws = u.getWorkingStatus() != null ? u.getWorkingStatus().toLowerCase() : "";
            return es.contains("nghỉ việc") || es.contains("nghi viec") || "resigned".equals(ws);
        }).count();

        long empInProject = employees.stream().filter(u -> u.getProject() != null && !u.getProject().trim().isEmpty() && !"—".equals(u.getProject().trim())).count();
        long empNoProject = Math.max(0, employees.size() - empInProject);

        long activeAccounts = all.stream().filter(u -> u.getAccountStatus() != null && u.getAccountStatus() == 1).count();
        long lockedAccounts = all.stream().filter(u -> u.getAccountStatus() != null && u.getAccountStatus() == 0).count();

        long workingEmployees = Math.max(0, employees.size() - empResigned);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_users", totalUsers);
        stats.put("total_interns", interns.size());
        stats.put("total_employees", employees.size());
        stats.put("working", workingInterns);
        stats.put("working_interns", workingInterns);
        stats.put("working_employees", workingEmployees);
        stats.put("emp_working", workingEmployees);
        stats.put("resigned", resignedInterns);
        stats.put("resigned_interns", resignedInterns);
        stats.put("fulltime", fulltime);
        stats.put("parttime", parttime);
        stats.put("intern_count", internCount);
        stats.put("borrowed_count", borrowedCount);

        stats.put("emp_trung_tam", empTrungTam);
        stats.put("emp_cho_muon", empChoMuon);
        stats.put("emp_onsite", empOnsite);
        stats.put("emp_probation", empProbation);
        stats.put("emp_resigned", empResigned);
        stats.put("emp_in_project", empInProject);
        stats.put("emp_no_project", empNoProject);

        stats.put("active_accounts", activeAccounts);
        stats.put("locked_accounts", lockedAccounts);

        // ─── Today Workers (TTS đi làm hôm nay: S = Sáng, C = Chiều, SC = Cả ngày) ───
        LocalDate today = LocalDate.now();
        List<Schedule> todaySchedules = scheduleRepository.findByWorkDay(today);
        List<Map<String, Object>> todayWorkers = new ArrayList<>();
        for (Schedule s : todaySchedules) {
            String shift = s.getShift() != null ? s.getShift().trim().toUpperCase() : "";
            if ("S".equals(shift) || "C".equals(shift) || "SC".equals(shift)) {
                User u = s.getUser();
                if (u == null && s.getUserId() != null) {
                    u = userRepository.findById(s.getUserId()).orElse(null);
                }
                if (u != null) {
                    boolean isIntern = "intern".equalsIgnoreCase(u.getUserType())
                            || "tts".equalsIgnoreCase(u.getUserType())
                            || (u.getEmployeeCode() != null && u.getEmployeeCode().toUpperCase().startsWith("TTS"));
                    if (isIntern) {
                        Map<String, Object> w = new HashMap<>();
                        w.put("id", s.getId());
                        w.put("user_id", u.getId());
                        w.put("employee_code", u.getEmployeeCode());
                        w.put("full_name", u.getFullName());
                        w.put("project", u.getProject() != null && !u.getProject().trim().isEmpty() ? u.getProject() : "—");
                        w.put("position", u.getPosition() != null && !u.getPosition().trim().isEmpty() ? u.getPosition() : "—");
                        w.put("shift", shift);
                        w.put("work_day", s.getWorkDay().toString());
                        todayWorkers.add(w);
                    }
                }
            }
        }
        todayWorkers.sort((a, b) -> String.valueOf(a.get("employee_code")).compareTo(String.valueOf(b.get("employee_code"))));
        stats.put("today_workers", todayWorkers);

        return stats;
    }

    // ─── Convert Entity to DTO ───

    public UserResponse toUserResponse(User user) {
        if (user == null) return null;

        String posName = user.getPosition();
        if (user.getPositionId() != null && user.getPositionRel() != null) {
            posName = user.getPositionRel().getName();
        } else if (user.getPositionId() != null) {
            posName = positionRepository.findById(user.getPositionId()).map(Position::getName).orElse(user.getPosition());
        }

        String username = user.getEmployeeCode();
        Optional<Account> acc = accountRepository.findByUserId(user.getId());
        if (acc.isPresent()) {
            username = acc.get().getUsername();
        }

        return UserResponse.builder()
                .id(user.getId())
                .employeeCode(user.getEmployeeCode())
                .fullName(user.getFullName())
                .role(user.getRole())
                .userType(user.getUserType())
                .gender(user.getGender())
                .ethnicity(user.getEthnicity())
                .viettelEmail(user.getViettelEmail())
                .birthday(user.getBirthday())
                .hometown(user.getHometown())
                .phone(user.getPhone())
                .cccd(user.getCccd())
                .bankName(user.getBankName())
                .bankAccount(user.getBankAccount())
                .project(user.getProject())
                .position(posName)
                .positionId(user.getPositionId())
                .positionName(posName)
                .joinDate(user.getJoinDate())
                .allowance(user.getAllowance())
                .employeeType(user.getEmployeeType())
                .workingStatus(user.getWorkingStatus())
                .employmentType(user.getEmploymentType())
                .directManager(user.getDirectManager())
                .computerSerial(user.getComputerSerial())
                .employmentStatus(user.getEmploymentStatus())
                .useCompanyMac(user.getUseCompanyMac())
                .staffCategory(user.getStaffCategory())
                .seatPosition(user.getSeatPosition())
                .borrowEndDate(user.getBorrowEndDate())
                .borrowProject(user.getBorrowProject())
                .borrowPm(user.getBorrowPm())
                .borrowCenter(user.getBorrowCenter())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus() : 1)
                .createdAt(user.getCreatedAt())
                .username(username)
                .build();
    }
}
