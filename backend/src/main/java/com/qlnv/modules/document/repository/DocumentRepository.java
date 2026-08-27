package com.qlnv.modules.document.repository;

import com.qlnv.modules.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findAllByOrderByCreatedAtDesc();
    List<Document> findByIsActiveTrue();
}
