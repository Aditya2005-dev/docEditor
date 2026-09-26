package com.example.doc_editor.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    List<Document> findByOwnerId(Long ownerId);

    @Modifying
    @Query("""
            UPDATE Document d
            SET d.content = :content,
                d.version = d.version + 1
            WHERE d.id = :documentId
              AND d.version = :version
            """)
    int updateContentIfVersionMatches(
            @Param("documentId") Long documentId,
            @Param("content") String content,
            @Param("version") Long version
    );
}