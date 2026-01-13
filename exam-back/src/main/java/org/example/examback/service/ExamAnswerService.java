package org.example.examback.service;

import org.example.examback.entity.ExamAnswer;

import java.util.List;
import java.util.Map;

/**
 * 考试答案服务接口
 */
public interface ExamAnswerService {
    /**
     * 批量插入答案
     * @param list 答案列表
     * @return 插入成功的数量
     */
    int insertBatch(List<ExamAnswer> list);

    /**
     * 获取客观题答案用于自动批改
     * @param examRecordId 考试记录ID
     * @param examId 考试ID
     * @return 客观题答案列表
     */
    List<Map<String, Object>> selectObjectiveAnswers(Integer examRecordId, Integer examId);

    /**
     * 根据考试记录ID获取所有答案
     * @param examRecordId 考试记录ID
     * @return 答案列表
     */
    List<ExamAnswer> selectByExamRecordId(Integer examRecordId);

    /**
     * 根据考试记录ID和题目ID获取答案
     * @param examRecordId 考试记录ID
     * @param questionId 题目ID
     * @return 答案对象
     */
    ExamAnswer selectByRecordAndQuestion(Integer examRecordId, Integer questionId);

    /**
     * 批量更新答案分数
     * @param list 答案列表
     * @return 更新成功的数量
     */
    int updateBatchScores(List<ExamAnswer> list);

    /**
     * 统计考试记录的总分
     * @param examRecordId 考试记录ID
     * @return 总分
     */
    Integer sumScoreByExamRecordId(Integer examRecordId);

    /**
     * 根据学生ID和考试ID查询考试答案
     * @param studentId 学生ID
     * @param examId 考试ID
     * @return 答案列表
     */
    List<ExamAnswer> selectByStudentAndExam(Integer studentId, Integer examId);

    /**
     * 保存单个题目分数
     * @param examRecordId 考试记录ID
     * @param questionId 题目ID
     * @param score 分数
     * @param comment 评语
     * @return 是否成功
     */
    boolean saveQuestionScore(Integer examRecordId, Integer questionId, Integer score, String comment);

    /**
     * 保存所有题目分数
     * @param examRecordId 考试记录ID
     * @param scores 分数列表
     * @return 是否成功
     */
    boolean saveAllScores(Integer examRecordId, List<Map<String, Object>> scores);
}