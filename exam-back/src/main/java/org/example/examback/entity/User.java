package org.example.examback.entity;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id,userId,classId;
    @NotNull(message = "用户名不能为空")
    private String username;
    private String account,password,avatar,sex,locality,general;
    private String role,phone,email,status;
    private Integer follow,fans;
    private Boolean isOnline;
    private String professional;
    private String college;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endLoginTime;
}
