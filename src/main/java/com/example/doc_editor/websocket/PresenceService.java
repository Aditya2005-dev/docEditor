package com.example.doc_editor.websocket;

import com.example.doc_editor.user.User;
import com.example.doc_editor.user.UserRepository;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private final UserRepository userRepository;

    /*
     * documentId
     *      ↓
     * email → ActiveUser
     */
    private final Map<Long, Map<String, ActiveUser>> users =
            new ConcurrentHashMap<>();

    public PresenceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void join(Long documentId, String email) {

        User user = userRepository
                .findByEmail(email)
                .orElse(null);

        if (user == null) {
            return;
        }

        Map<String, ActiveUser> documentUsers =
                users.computeIfAbsent(
                        documentId,
                        key -> new ConcurrentHashMap<>()
                );

        documentUsers.put(
                email,
                new ActiveUser(
                        email,
                        user.getName(),
                        "VIEWING"
                )
        );
    }

    public void leave(Long documentId, String email) {

        Map<String, ActiveUser> documentUsers =
                users.get(documentId);

        if (documentUsers == null) {
            return;
        }

        documentUsers.remove(email);

        if (documentUsers.isEmpty()) {
            users.remove(documentId);
        }
    }

    public void updateStatus(
            Long documentId,
            String email,
            String status
    ) {

        Map<String, ActiveUser> documentUsers =
                users.get(documentId);

        if (documentUsers == null) {
            return;
        }

        ActiveUser user =
                documentUsers.get(email);

        if (user != null) {
            user.setStatus(status);
        }
    }

    public List<ActiveUser> getUsers(Long documentId) {

        Map<String, ActiveUser> documentUsers =
                users.get(documentId);

        if (documentUsers == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                documentUsers.values()
        );
    }
}