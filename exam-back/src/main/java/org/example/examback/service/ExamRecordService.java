package org.example.examback.service;

import org.example.examback.entity.ExamRecord;

import java.util.List;
import java.util.Map;

/**
 * 考试记录服务接口
 */
public interface ExamRecordService {
    /**
     * 根据考试ID和学生ID查找考试记录
     * @param examId 考试ID
     * @param studentId 学生ID
     * @return 考试记录
     */
    ExamRecord selectByExamAndStudent(Integer examId, Integer studentId);

    /**
     * 更新考试记录状态和分数
     * @param id 记录ID
     * @param status 状态
     * @param totalScore 总分
     * @return 更新成功的数量
     */
    int updateStatusAndScore(Integer id, String status, Integer totalScore);

    /**
     * 插入考试记录
     * @param examRecord 考试记录对象
     * @return 插入成功的数量
     */
    int insert(ExamRecord examRecord);

    /**
     * 根据ID查询考试记录
     * @param id 记录ID
     * @return 考试记录
     */
    ExamRecord selectById(Integer id);

    /**
     * 更新考试时间
     * @param id 记录ID
     * @param endTime 结束时间
     * @param timeSpent 用时
     * @return 更新成功的数量
     */
    int updateTime(Integer id, String endTime, Integer timeSpent);

    /**
     * 提交试卷
     * @param paperId 试卷ID
     * @param userId 用户ID
     * @param answers 答案列表
     * @return 是否成功
     */
    boolean submitPaper(Integer paperId, Integer userId, List<Map<String, Object>> answers);

    /**
     * 获取考试记录列表
     * @param examId 考试ID
     * @param status 状态
     * @param search 搜索关键词
     * @param page 页码
     * @param pageSize 每页大小
     * @return 记录列表和分页信息
     */
    Map<String, Object> getExamRecords(Integer examId, String status, String search, Integer page, Integer pageSize);

    /**
     * 完成批改
     * @param examRecordId 考试记录ID
     * @param totalScore 总分
     * @return 是否成功
     */
    boolean completeGrading(Integer examRecordId, Integer totalScore);

    /**
     * 获取批改数据
     * @param examRecordId 考试记录ID
     * @param examId 考试ID
     * @return 批改数据
     */
    Map<String, Object> getGradingData(Integer examRecordId, Integer examId);

    /**
     * 导出答案
     * @param examRecordId 考试记录ID
     * @param studentId 学生ID
     * @param examId 考试ID
     * @return 导出数据
     */
    Map<String, Object> exportAnswers(Integer examRecordId, Integer studentId, Integer examId);
}