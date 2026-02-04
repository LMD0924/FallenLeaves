package org.example.examback.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Date;

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endLoginTime;
}
