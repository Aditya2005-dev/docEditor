package com.example.doc_editor.document.dto;

import com.example.doc_editor.document.permission.Permission;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShareRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Permission permission;

    public ShareRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}