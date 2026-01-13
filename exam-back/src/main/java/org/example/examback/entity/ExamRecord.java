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
public class ExamRecord {
    private Integer id;
    private Integer studentId;//关联学生id
    private Integer examId;//关联考试id
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;//考试开始时间
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;//考试结束时间
    private Integer totalScore;//所得总分
    private String status;//考试状态
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime timeSpent;//考试用时
    private Integer attemptCount;//考试次数
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
