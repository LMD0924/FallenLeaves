package org.example.examback.entity;

import lombok.Data;

@Data
public class QuestionType {
    private Integer id,teacher_id,course_id;
    private String questionType;//问题类型
    private String text;//问题类型描述
    private String score;//分数
    private String optionA,optionB,optionC,optionD,optionE,optionF,optionG;
    private String answer;//答案
    private String knowledge;//知识
    private String difficulty;//难度
    private String status;//状态
}
