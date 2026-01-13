package org.example.examback.controller;

import jakarta.annotation.Resource;
import org.example.examback.entity.ExamAnswer;
import org.example.examback.entity.RestBean;
import org.example.examback.service.ExamAnswerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/exam/answer")
public class ExamAnswerController {
    
    @Resource
    private ExamAnswerService examAnswerService;
    
    /**
     * 批量保存考试答案
     */
    @PostMapping("/batch-save")
    public RestBean<Integer> insertBatch(@RequestBody List<ExamAnswer> answers) {
        try {
            int result = examAnswerService.insertBatch(answers);
            if (result > 0) {
                return RestBean.success("批量保存答案成功", result);
            } else {
                return RestBean.failure(500, "批量保存答案失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "批量保存答案异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取客观题答案用于自动批改
     */
    @GetMapping("/objective")
    public RestBean<List<Map<String, Object>>> selectObjectiveAnswers(@RequestParam Integer examRecordId, @RequestParam Integer examId) {
        try {
            List<Map<String, Object>> answers = examAnswerService.selectObjectiveAnswers(examRecordId, examId);
            return RestBean.success("获取客观题答案成功", answers);
        } catch (Exception e) {
            return RestBean.failure(500, "获取客观题答案异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据考试记录ID获取所有答案
     */
    @GetMapping("/record")
    public RestBean<List<ExamAnswer>> selectByExamRecordId(@RequestParam Integer examRecordId) {
        try {
            List<ExamAnswer> answers = examAnswerService.selectByExamRecordId(examRecordId);
            return RestBean.success("获取答案成功", answers);
        } catch (Exception e) {
            return RestBean.failure(500, "获取答案异常: " + e.getMessage());
        }
    }
    
    /**
     * 保存单个题目分数
     */
    @PostMapping("/save-score")
    public RestBean<Boolean> saveQuestionScore(@RequestBody Map<String, Object> params) {
        try {
            Integer examRecordId = (Integer) params.get("examRecordId");
            Integer questionId = (Integer) params.get("questionId");
            Integer score = (Integer) params.get("score");
            String comment = (String) params.get("comment");
            
            boolean result = examAnswerService.saveQuestionScore(examRecordId, questionId, score, comment);
            if (result) {
                return RestBean.success("保存分数成功", result);
            } else {
                return RestBean.failure(500, "保存分数失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "保存分数异常: " + e.getMessage());
        }
    }
    
    /**
     * 批量更新答案分数
     */
    @PostMapping("/batch-update-score")
    public RestBean<Integer> updateBatchScores(@RequestBody List<ExamAnswer> answers) {
        try {
            int result = examAnswerService.updateBatchScores(answers);
            if (result > 0) {
                return RestBean.success("批量更新分数成功", result);
            } else {
                return RestBean.failure(500, "批量更新分数失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "批量更新分数异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据考试记录ID和题目ID获取答案
     */
    @GetMapping("/record-question")
    public RestBean<ExamAnswer> selectByRecordAndQuestion(@RequestParam Integer examRecordId, @RequestParam Integer questionId) {
        try {
            ExamAnswer answer = examAnswerService.selectByRecordAndQuestion(examRecordId, questionId);
            if (answer != null) {
                return RestBean.success("获取答案详情成功", answer);
            } else {
                return RestBean.failure(404, "答案不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "获取答案详情异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据学生ID和考试ID查询考试答案
     */
    @GetMapping("/student-exam")
    public RestBean<List<ExamAnswer>> selectByStudentAndExam(@RequestParam Integer studentId, @RequestParam Integer examId) {
        try {
            List<ExamAnswer> answers = examAnswerService.selectByStudentAndExam(studentId, examId);
            if (answers != null) {
                return RestBean.success("获取答案详情成功", answers);
            } else {
                return RestBean.failure(404, "答案不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "获取答案详情异常: " + e.getMessage());
        }
    }
    
    /**
     * 保存所有题目分数
     */
    @PostMapping("/save-all-scores")
    public RestBean<Boolean> saveAllScores(@RequestBody Map<String, Object> params) {
        try {
            Integer examRecordId = (Integer) params.get("examRecordId");
            List<Map<String, Object>> scores = (List<Map<String, Object>>) params.get("scores");
            
            boolean result = examAnswerService.saveAllScores(examRecordId, scores);
            if (result) {
                return RestBean.success("保存所有分数成功", result);
            } else {
                return RestBean.failure(500, "保存所有分数失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "保存所有分数异常: " + e.getMessage());
        }
    }
    
    /**
     * 统计考试记录的总分
     */
    @GetMapping("/sum-score")
    public RestBean<Integer> sumScoreByExamRecordId(@RequestParam Integer examRecordId) {
        try {
            Integer totalScore = examAnswerService.sumScoreByExamRecordId(examRecordId);
            return RestBean.success("获取总分成功", totalScore);
        } catch (Exception e) {
            return RestBean.failure(500, "获取总分异常: " + e.getMessage());
        }
    }
}