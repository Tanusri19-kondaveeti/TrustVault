
package com.trustvault.dto;

public class RegisterResponse {

    private Long id;
    private String name;
    private String email;
    private String message;

    public RegisterResponse() {
    }

    public RegisterResponse(Long id, String name, String email, String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }
}
