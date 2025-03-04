package com.Yu.midterm.Model;

public class Contact {
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String resourceName;



    // Constructor with resourceName (updated to use firstName and lastName)
    public Contact(String firstName, String lastName, String email, String phoneNumber, String resourceName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.resourceName = resourceName;
    }

    // Empty constructor
    public Contact() {}

    // Getters and setters (including resourceName)

    public String getFirstName() { // Corrected getter
        return firstName;
    }

    public void setFirstName(String firstName) { // Corrected setter
        this.firstName = firstName;
    }

    public String getLastName() { // Corrected getter
        return lastName;
    }

    public void setLastName(String lastName) { // Corrected setter
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}