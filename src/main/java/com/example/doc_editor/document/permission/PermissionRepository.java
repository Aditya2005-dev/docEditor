package com.example.doc_editor.document.permission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository
        extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(
            Long documentId,
            Long userId
    );

    List<DocumentPermission> findByDocumentId(
            Long documentId
    );

    List<DocumentPermission> findByUserId(
            Long userId
    );
}