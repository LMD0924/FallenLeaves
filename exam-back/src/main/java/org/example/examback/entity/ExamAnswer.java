package org.example.examback.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * @Author:总会落叶
 * @Date:2026/1/12
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamAnswer {
    private Integer id;
    private Integer examRecordId;//关联考试记录id
    private Integer studentId;//关联学生id
    private Integer examId;//关联考试id
    private Integer questionId;//关联题目id
    private Integer teacherId;//关联批阅老师id
    private String studentAnswer;//学生答案
    private String questionAnswer;//题目答案
    private Integer score;//得分
    private Integer isCorrect;//是否正确
    private String teacherComment;//老师评语
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
