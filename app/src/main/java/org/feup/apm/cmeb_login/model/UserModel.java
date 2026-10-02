package org.feup.apm.cmeb_login.model;


import com.google.firebase.Timestamp;

public class UserModel {
    private String username;
    private String email;

    private Timestamp createdTimestamp;

    private String userId;

    private String fcmToken;

    private int yearsExperience;

    private String hospital;

    private String specialty;



    public UserModel() {
    }

    public UserModel(String username, String email, Timestamp createdTimestamp, String userId, int yearsExperience, String hospital, String specialty) {
        this.username = username;
        this.email = email;
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
        this.yearsExperience = yearsExperience;
        this.hospital = hospital;
        this.specialty = specialty;
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

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "UserModel{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public int getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(int yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }
}
