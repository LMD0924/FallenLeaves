package org.example.examback.entity;

import lombok.Data;

@Data
public class ExamClass {
    private Integer id;//班级id
    private String name;//班级名称
    private Integer size;//班级人数
    private Integer teacher_id;//班主任
    private String introduction;//班级介绍
    private String status;//班级状态
    private String major;//专业
    private String grade;//年级
}
