package com.qlnv.modules.importjob.repository;

import com.qlnv.modules.importjob.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Integer> {
    List<ImportJob> findTop20ByOrderByCreatedAtDesc();
    List<ImportJob> findByStatus(String status);
}
