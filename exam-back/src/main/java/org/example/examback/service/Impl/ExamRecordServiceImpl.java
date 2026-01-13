package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import org.example.examback.entity.ExamAnswer;
import org.example.examback.entity.ExamRecord;
import org.example.examback.mapper.ExamRecordMapper;
import org.example.examback.service.ExamAnswerService;
import org.example.examback.service.ExamRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
@Transactional
public class ExamRecordServiceImpl implements ExamRecordService {

    private static final Logger logger = Logger.getLogger(ExamRecordServiceImpl.class.getName());
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private ExamRecordMapper examRecordMapper;
    
    @Resource
    private ExamAnswerService examAnswerService;

    @Override
    public ExamRecord selectByExamAndStudent(Integer examId, Integer studentId) {
        logger.info("调用selectByExamAndStudent方法，examId: " + examId + " studentId: " + studentId);
        return examRecordMapper.selectByExamAndStudent(examId, studentId);
    }

    @Override
    public int updateStatusAndScore(Integer id, String status, Integer totalScore) {
        logger.info("调用updateStatusAndScore方法，id: " + id + " status: " + status + " totalScore: " + totalScore);
        return examRecordMapper.updateStatusAndScore(id, status, totalScore);
    }

    @Override
    public int insert(ExamRecord examRecord) {
        logger.info("调用insert方法，examRecord: " + examRecord);
        return examRecordMapper.insert(examRecord);
    }

    @Override
    public ExamRecord selectById(Integer id) {
        logger.info("调用selectById方法，id: " + id);
        return examRecordMapper.selectById(id);
    }

    @Override
    public int updateTime(Integer id, String endTime, Integer timeSpent) {
        logger.info("调用updateTime方法，id: " + id + " endTime: " + endTime + " timeSpent: " + timeSpent);
        LocalDateTime formattedEndTime = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
        return examRecordMapper.updateTime(id, formattedEndTime, timeSpent);
    }

    @Override
    public boolean submitPaper(Integer paperId, Integer userId, List<Map<String, Object>> answers) {
        logger.info("调用submitPaper方法，paperId: " + paperId + " userId: " + userId + " answers: " + answers);
        try {
            // 1. 查询当前考试记录
            ExamRecord record = examRecordMapper.selectByExamAndStudent(paperId, userId);
            if (record == null) {
                logger.severe("提交试卷失败: 未找到考试记录");
                return false;
            }
            
            // 2. 保存学生答案
            if (answers != null && !answers.isEmpty()) {
                List<ExamAnswer> examAnswerList = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();
                
                for (Map<String, Object> answerMap : answers) {
                    ExamAnswer examAnswer = new ExamAnswer();
                    examAnswer.setExamRecordId(record.getId());
                    examAnswer.setStudentId(userId);
                    examAnswer.setExamId(paperId);
                    
                    // 处理questionId，可能是字符串或整数
                    Object questionIdObj = answerMap.get("questionId");
                    if (questionIdObj != null) {
                        Integer questionId = questionIdObj instanceof Integer 
                            ? (Integer) questionIdObj 
                            : Integer.parseInt(questionIdObj.toString());
                        examAnswer.setQuestionId(questionId);
                    }
                    
                    // 处理答案，可能是字符串或数组（多选题）
                    Object answerObj = answerMap.get("answer");
                    String studentAnswer;
                    if (answerObj == null) {
                        studentAnswer = "";
                    } else if (answerObj instanceof List) {
                        // 多选题：将数组转换为逗号分隔的字符串
                        List<?> answerArray = (List<?>) answerObj;
                        studentAnswer = String.join(",", answerArray.stream()
                            .map(Object::toString)
                            .toArray(String[]::new));
                    } else {
                        studentAnswer = answerObj.toString();
                    }
                    examAnswer.setStudentAnswer(studentAnswer);
                    
                    examAnswer.setScore(0); // 初始分数为0，等待批改
                    examAnswer.setIsCorrect(0);
                    examAnswer.setCreatedAt(now);
                    examAnswer.setUpdatedAt(now);
                    
                    examAnswerList.add(examAnswer);
                }
                
                // 批量保存答案
                if (!examAnswerList.isEmpty()) {
                    examAnswerService.insertBatch(examAnswerList);
                    logger.info("保存了 " + examAnswerList.size() + " 条答案");
                }
            }
            
            // 3. 更新考试记录状态为已提交
            LocalDateTime now = LocalDateTime.now();
            int timeSpent = calculateTimeSpent(record.getStartTime(), now);
            
            // 4. 保存考试时间和用时
            examRecordMapper.updateTime(record.getId(), now, timeSpent);
            
            // 5. 更新考试状态
            examRecordMapper.updateStatusAndScore(record.getId(), "已提交", null);
            
            logger.info("提交试卷成功，recordId: " + record.getId());
            return true;
        } catch (Exception e) {
            logger.severe("提交试卷失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getExamRecords(Integer examId, String status, String search, Integer page, Integer pageSize) {
        logger.info("调用getExamRecords方法，examId: " + examId + " status: " + status + " search: " + search + " page: " + page + " pageSize: " + pageSize);
        try {
            // 计算偏移量
            int offset = (page - 1) * pageSize;
            
            // 查询记录总数
            int total = examRecordMapper.countExamRecords(examId, status, search);
            
            // 分页查询记录
            List<Map<String, Object>> records = examRecordMapper.selectExamRecordsByPage(examId, status, search, offset, pageSize);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", total);
            result.put("records", records);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", (int) Math.ceil((double) total / pageSize));
            
            logger.info("获取考试记录列表成功，总数: " + total + " 页数: " + (int) Math.ceil((double) total / pageSize));
            return result;
        } catch (Exception e) {
            logger.severe("获取考试记录列表失败: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public boolean completeGrading(Integer examRecordId, Integer totalScore) {
        logger.info("调用completeGrading方法，examRecordId: " + examRecordId + " totalScore: " + totalScore);
        try {
            return examRecordMapper.updateStatusAndScore(examRecordId, "已批改", totalScore) > 0;
        } catch (Exception e) {
            logger.severe("完成批改失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Object> getGradingData(Integer examRecordId, Integer examId) {
        logger.info("调用getGradingData方法，examRecordId: " + examRecordId + " examId: " + examId);
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 获取考试记录
            ExamRecord record = examRecordMapper.selectById(examRecordId);
            if (record == null) {
                logger.severe("获取批改数据失败: 未找到考试记录");
                return null;
            }
            
            // 2. 这里可以添加更多查询逻辑，如获取考试题目、学生答案等
            // ...
            
            result.put("record", record);
            // 可以根据需要添加其他数据
            
            logger.info("获取批改数据成功");
            return result;
        } catch (Exception e) {
            logger.severe("获取批改数据失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, Object> exportAnswers(Integer examRecordId, Integer studentId, Integer examId) {
        logger.info("调用exportAnswers方法，examRecordId: " + examRecordId + " studentId: " + studentId + " examId: " + examId);
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 获取考试记录
            ExamRecord record = examRecordMapper.selectById(examRecordId);
            if (record == null) {
                logger.severe("导出答案失败: 未找到考试记录");
                return null;
            }
            
            // 2. 这里可以添加更多查询逻辑，如获取学生答案详情等
            // ...
            
            result.put("record", record);
            // 可以根据需要添加其他数据
            
            logger.info("导出答案成功");
            return result;
        } catch (Exception e) {
            logger.severe("导出答案失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 计算考试用时（以秒为单位）
     */
    private int calculateTimeSpent(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) java.time.Duration.between(startTime, endTime).toSeconds();
    }
}