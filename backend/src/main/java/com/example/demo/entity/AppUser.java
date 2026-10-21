package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
/**
 * Application user record.
 *
 * Why this is minimal:
 * - This project focuses on the flight tracking DB and API integration.
 * - Authentication/authorization is not modeled deeply here, so the schema stores only the fields
 *   needed for basic CRUD-style experimentation in the UI/DB explorer.
 *
 * Security note:
 * - `upass` stores a BCrypt hash; plain-text passwords are never persisted.
 */
public class AppUser {

    /** Primary key (auto-increment). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "u_id")
    private Long uId;

    /** Username shown in the UI. */
    @Column(name = "uname", length = 100)
    private String uname;

    /** BCrypt-hashed password. */
    @Column(name = "upass", length = 255)
    private String upass;

    public AppUser() {}

    public Long getUId() { return uId; }
    public void setUId(Long uId) { this.uId = uId; }

    public String getUname() { return uname; }
    public void setUname(String uname) { this.uname = uname; }

    public String getUpass() { return upass; }
    public void setUpass(String upass) { this.upass = upass; }
}
