package com.qlnv.modules.document.repository;

import com.qlnv.modules.document.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Integer documentId);

    List<DocumentChunk> findByDocumentIdIn(List<Integer> documentIds);

    long countByDocumentId(Integer documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk c WHERE c.documentId = :docId")
    void deleteByDocumentId(@Param("docId") Integer documentId);

    @Query("SELECT c FROM DocumentChunk c WHERE c.documentId IN :docIds AND c.embedding IS NOT NULL")
    List<DocumentChunk> findEmbeddedByDocumentIdIn(@Param("docIds") List<Integer> documentIds);
}
