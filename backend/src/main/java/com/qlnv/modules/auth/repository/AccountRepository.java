package com.qlnv.modules.auth.repository;

import com.qlnv.modules.auth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByUsername(String username);
    Optional<Account> findByUserId(Integer userId);
    boolean existsByUsername(String username);
    boolean existsByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
