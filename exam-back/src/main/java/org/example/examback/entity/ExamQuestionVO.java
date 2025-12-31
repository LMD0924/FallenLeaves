package org.example.examback.entity;

import lombok.Data;

@Data
public class ExamQuestionVO {
    private Integer exam_question_id;
    private Integer score;
    private Integer question_order;
    private Integer question_id;
    private String questionType;
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;
    private String optionG;
    private String answer;
    private String knowledge;
    private String difficulty;
    private Integer question_status;
    private String exam_title;
    private String exam_description;
    private Integer exam_total_score;
    private Integer exam_duration;
    private Integer exam_pass_score;
    private String exam_status;
}
