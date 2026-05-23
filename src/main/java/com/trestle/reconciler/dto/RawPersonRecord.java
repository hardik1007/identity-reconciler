package com.trestle.reconciler.dto;

import lombok.Data;

@Data
public class RawPersonRecord {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;   // some sources provide only full name
    private String phone;
    private String email;
    private String dob;        // raw string — format varies per source
    private String address;
}
