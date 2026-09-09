package com.qlnv.common.config;

import com.qlnv.modules.auth.entity.Account;
import com.qlnv.modules.auth.repository.AccountRepository;
import com.qlnv.modules.user.entity.Position;
import com.qlnv.modules.user.entity.User;
import com.qlnv.modules.user.repository.PositionRepository;
import com.qlnv.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPositions();
        seedAdmin();
        cleanUpInternAccounts();
        syncEmployeeAccounts();
    }

    private void cleanUpInternAccounts() {
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            boolean isIntern = "intern".equalsIgnoreCase(u.getUserType())
                    || "tts".equalsIgnoreCase(u.getUserType())
                    || (u.getEmployeeCode() != null && u.getEmployeeCode().trim().toUpperCase().startsWith("TTS"));
            if (isIntern) {
                accountRepository.deleteByUserId(u.getId());
            }
        }
        log.info("[OK] Intern accounts cleaned up - TTS has no login accounts");
    }

    private void syncEmployeeAccounts() {
        String encodedDefaultPassword = passwordEncoder.encode("123456");
        List<User> employees = userRepository.findByUserType("employee");
        for (User u : employees) {
            String desiredUsername = (u.getViettelEmail() != null && !u.getViettelEmail().trim().isEmpty())
                    ? u.getViettelEmail().trim().toLowerCase()
                    : (u.getEmployeeCode() != null ? u.getEmployeeCode().trim() : null);
            if (desiredUsername == null || desiredUsername.isEmpty()) continue;

            java.util.Optional<Account> accOpt = accountRepository.findByUserId(u.getId());
            if (accOpt.isPresent()) {
                Account acc = accOpt.get();
                acc.setUsername(desiredUsername);
                acc.setPassword(encodedDefaultPassword);
                accountRepository.save(acc);
            } else {
                Account acc = Account.builder()
                        .userId(u.getId())
                        .username(desiredUsername)
                        .password(encodedDefaultPassword)
                        .createdAt(LocalDateTime.now())
                        .build();
                accountRepository.save(acc);
            }
        }
        log.info("[OK] Employee accounts synced: username = viettel_email, password = 123456");
    }

    private void seedPositions() {
        List<Position> defaultPositions = List.of(
            Position.builder().name("Trợ lý dự án").isManager(false).build(),
            Position.builder().name("PM").isManager(true).build(),
            Position.builder().name("DU Lead").isManager(true).build(),
            Position.builder().name("GDTT").isManager(true).build(),
            Position.builder().name("PGDTT").isManager(true).build(),
            Position.builder().name("Dev").isManager(false).build(),
            Position.builder().name("Dev Lead").isManager(true).build(),
            Position.builder().name("Dev Mobile").isManager(false).build(),
            Position.builder().name("DevOps").isManager(false).build(),
            Position.builder().name("Tester").isManager(false).build(),
            Position.builder().name("Test Lead").isManager(true).build(),
            Position.builder().name("BA").isManager(false).build(),
            Position.builder().name("BA Lead").isManager(true).build(),
            Position.builder().name("QA").isManager(false).build(),
            Position.builder().name("DA").isManager(false).build(),
            Position.builder().name("AI").isManager(false).build()
        );

        for (Position p : defaultPositions) {
            if (positionRepository.findByName(p.getName()).isEmpty()) {
                positionRepository.save(p);
            }
        }
        log.info("[OK] Positions verified/seeded");
    }

    private void seedAdmin() {
        User admin = userRepository.findByEmployeeCode("admin").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .employeeCode("admin")
                    .fullName("Quản trị viên")
                    .role("admin")
                    .userType("admin")
                    .accountStatus(1)
                    .createdAt(LocalDateTime.now())
                    .build();
            admin = userRepository.save(admin);
        } else {
            admin.setRole("admin");
            if (!"admin".equalsIgnoreCase(admin.getUserType())) {
                admin.setUserType("admin");
            }
            admin.setAccountStatus(1);
            admin = userRepository.save(admin);
        }

        final Integer adminUserId = admin.getId();
        Account adminAcc = accountRepository.findByUsername("admin")
                .or(() -> accountRepository.findByUserId(adminUserId))
                .orElse(null);

        if (adminAcc == null) {
            adminAcc = Account.builder()
                    .userId(admin.getId())
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .createdAt(LocalDateTime.now())
                    .build();
            accountRepository.save(adminAcc);
            log.info("[OK] Admin account created: admin / Admin@123");
        } else {
            adminAcc.setUserId(admin.getId());
            adminAcc.setUsername("admin");
            adminAcc.setPassword(passwordEncoder.encode("Admin@123"));
            accountRepository.save(adminAcc);
            log.info("[OK] Admin account verified & synchronized: admin / Admin@123");
        }
    }
}
