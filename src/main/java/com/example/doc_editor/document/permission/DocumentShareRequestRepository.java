package com.example.doc_editor.document.permission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentShareRequestRepository
        extends JpaRepository<DocumentShareRequest, Long> {

    List<DocumentShareRequest>
    findByReceiverIdAndStatusOrderByCreatedAtDesc(
            Long receiverId,
            ShareRequestStatus status
    );

    Optional<DocumentShareRequest>
    findByIdAndReceiverId(
            Long id,
            Long receiverId
    );

    boolean existsByDocumentIdAndReceiverIdAndStatus(
            Long documentId,
            Long receiverId,
            ShareRequestStatus status
    );

    // Used when deleting a document
    @Query("""
            SELECT r
            FROM DocumentShareRequest r
            WHERE r.document.id = :documentId
            """)
    List<DocumentShareRequest> findByDocumentId(
            @Param("documentId") Long documentId
    );
}