package org.example.examback.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.examback.entity.ExamAnswer;

import java.util.List;
import java.util.Map;

/*
 * @Author:总会落叶
 * @Date:2026/1/13
 * @Description:
 */
@Mapper
public interface ExamAnswerMapper {
    /**
     * 批量插入答案
     */
    int insertBatch(@Param("list") List<ExamAnswer> list);

    /**
     * 获取客观题答案用于自动批改
     */
    List<Map<String, Object>> selectObjectiveAnswers(
            @Param("examRecordId") Integer examRecordId,
            @Param("examId") Integer examId);

    /**
     * 根据考试记录ID获取所有答案
     */
    List<ExamAnswer> selectByExamRecordId(@Param("examRecordId") Integer examRecordId);

    /**
     * 根据考试记录ID和题目ID获取答案
     */
    ExamAnswer selectByRecordAndQuestion(
            @Param("examRecordId") Integer examRecordId,
            @Param("questionId") Integer questionId);

    /**
     * 批量更新答案分数
     */
    int updateBatchScores(@Param("list") List<ExamAnswer> list);

    /**
     * 统计考试记录的总分
     */
    Integer sumScoreByExamRecordId(@Param("examRecordId") Integer examRecordId);

    /**
     * 根据学生ID和考试ID查询考试答案
     */
    List<ExamAnswer> selectByStudentAndExam(
            @Param("studentId") Integer studentId,
            @Param("examId") Integer examId);}
