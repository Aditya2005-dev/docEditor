package com.example.doc_editor.document.permission;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(
            Long documentId,
            Long userId
    );

    List<DocumentPermission> findByDocumentId(Long documentId);

    List<DocumentPermission> findByUserId(Long userId);

    @Transactional
    @Modifying
    @Query("""
            DELETE FROM DocumentPermission p
            WHERE p.document.id = :documentId
            """)
    void deleteByDocumentId(@Param("documentId") Long documentId);
}