package com.ead.sparkpoint.models;

public class AppUser {
    private String id;
    private String username;
    private String email;
    private Integer roleId;
    private String roleName;
    private String nic;
    private String accessToken;
    private String refreshToken;

    // Add these missing fields
    private String firstName;
    private String lastName;
    private String password; // Storing raw password in the model is generally not recommended for long term
    private String phone;

    // Constructor needs to be updated
    public AppUser(String id, String username, String email, Integer roleId, String roleName,
                   String firstName, String lastName, String password, String nic, String phone,
                   String accessToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roleId = roleId;
        this.roleName = roleName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.nic = nic;
        this.phone = phone;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // --- Existing Getters and Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // --- Add Getters and Setters for the new fields ---
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
