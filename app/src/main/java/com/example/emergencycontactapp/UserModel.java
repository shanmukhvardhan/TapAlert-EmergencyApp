package com.example.emergencycontactapp;

public class UserModel {
    public String userId;
    public String name;
    public String email;

    public UserModel() {
        // Default constructor required for calls to DataSnapshot.getValue(UserModel.class)
    }

    public UserModel(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }
}
