package org.example.examback.entity;

import lombok.Data;

@Data
public class Course {
    private Integer id;
    private String name;
    private String score;  // 学分
    private Integer teacher_id;  // 教师ID（关联xm_teacher.user_id）
    private String status; // 审核状态

    // 关联教师信息
    private String teacher_name;
    private String teacher_phone;
    private String teacher_email;
    private Integer user_id;
    private String role;
    private Integer course_id;
    private String account;
    //加入课程的审核状态
    private String statue;
}
