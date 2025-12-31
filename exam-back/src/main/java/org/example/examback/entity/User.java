package org.example.examback.entity;

import lombok.Data;

@Data
public class User {
    private Integer id,user_id,class_id;
    private String username,account,password,avatar,sex,locality,general;
    private String role,phone,email,status;
    private Integer follow,fans;
    private Boolean is_online;
    private String professional;
}
