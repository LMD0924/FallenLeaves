package org.example.examback.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.examback.entity.ExamRecord;
import org.example.examback.entity.RestBean;
import org.example.examback.service.ExamRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/exam/record")
public class ExamRecordController {
    
    @Resource
    private ExamRecordService examRecordService;
    
    /**
     * 根据考试ID和学生ID获取考试记录
     */
    @GetMapping("/find")
    public RestBean<ExamRecord> selectByExamAndStudent(
            @RequestParam Integer examId,
            HttpServletRequest request) {
        try {
            Integer studentId = (Integer) request.getAttribute("id");
            ExamRecord record = examRecordService.selectByExamAndStudent(examId, studentId);
            if (record != null) {
                return RestBean.success("获取考试记录成功", record);
            } else {
                return RestBean.failure(404, "考试记录不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "获取考试记录异常: " + e.getMessage());
        }
    }
    
    /**
     * 更新考试记录状态和分数
     */
    @PostMapping("/update-status-score")
    public RestBean<Integer> updateStatusAndScore(
            @RequestParam Integer id,
            @RequestParam String status,
            @RequestParam(required = false) Integer totalScore) {
        try {
            int result = examRecordService.updateStatusAndScore(id, status, totalScore);
            if (result > 0) {
                return RestBean.success("更新考试记录状态和分数成功", result);
            } else {
                return RestBean.failure(500, "更新考试记录状态和分数失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "更新考试记录状态和分数异常: " + e.getMessage());
        }
    }
    
    /**
     * 插入考试记录
     */
    @PostMapping("/insert")
    public RestBean<Integer> insert(@ModelAttribute ExamRecord examRecord) {
        try {
            int result = examRecordService.insert(examRecord);
            if (result > 0) {
                return RestBean.success("插入考试记录成功", result);
            } else {
                return RestBean.failure(500, "插入考试记录失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "插入考试记录异常: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID查询考试记录
     */
    @GetMapping("/detail")
    public RestBean<ExamRecord> selectById(@RequestParam Integer id) {
        try {
            ExamRecord record = examRecordService.selectById(id);
            if (record != null) {
                return RestBean.success("获取考试记录详情成功", record);
            } else {
                return RestBean.failure(404, "考试记录不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "获取考试记录详情异常: " + e.getMessage());
        }
    }
    
    /**
     * 更新考试时间
     */
    @PostMapping("/update-time")
    public RestBean<Integer> updateTime(
            @RequestParam Integer id,
            @RequestParam String endTime,
            @RequestParam Integer timeSpent) {
        try {
            int result = examRecordService.updateTime(id, endTime, timeSpent);
            if (result > 0) {
                return RestBean.success("更新考试时间成功", result);
            } else {
                return RestBean.failure(500, "更新考试时间失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "更新考试时间异常: " + e.getMessage());
        }
    }
    
    /**
     * 提交试卷
     */
    @PostMapping("/submit-paper")
    public RestBean<Boolean> submitPaper(
            @RequestParam Integer paperId,
            HttpServletRequest request) {
        try {
            Integer userId = (Integer) request.getAttribute("id");
            
            // 获取所有请求参数
            Map<String, String[]> parameterMap = request.getParameterMap();
            
            // 解析answers参数
            List<Map<String, Object>> answers = new ArrayList<>();
            Map<Integer, Map<String, Object>> tempAnswers = new HashMap<>();
            
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                
                // 匹配answers[0][questionId]这样的格式
                if (key.startsWith("answers[")) {
                    String pattern = "answers\\[(\\d+)\\]\\[(\\w+)\\]";
                    java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
                    java.util.regex.Matcher m = r.matcher(key);
                    
                    if (m.matches()) {
                        int index = Integer.parseInt(m.group(1));
                        String field = m.group(2);
                        String value = values[0];
                        
                        // 确保该索引的Map存在
                        tempAnswers.putIfAbsent(index, new HashMap<>());
                        Map<String, Object> answerMap = tempAnswers.get(index);
                        
                        // 处理questionId为数字
                        if (field.equals("questionId")) {
                            answerMap.put(field, Integer.parseInt(value));
                        } else {
                            answerMap.put(field, value);
                        }
                    }
                }
            }
            
            // 将tempAnswers转换为List
            for (int i = 0; i < tempAnswers.size(); i++) {
                if (tempAnswers.containsKey(i)) {
                    answers.add(tempAnswers.get(i));
                }
            }
            
            boolean result = examRecordService.submitPaper(paperId, userId, answers);
            if (result) {
                return RestBean.success("提交试卷成功", true);
            } else {
                return RestBean.failure(500, "提交试卷失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "提交试卷异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取考试记录列表
     */
    @GetMapping("/list")
    public RestBean<Map<String, Object>> getExamRecords(
            @RequestParam(required = false) Integer examId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> records = examRecordService.getExamRecords(examId, status, search, page, pageSize);
            return RestBean.success("获取考试记录列表成功", records);
        } catch (Exception e) {
            return RestBean.failure(500, "获取考试记录列表异常: " + e.getMessage());
        }
    }
    
    /**
     * 完成批改
     */
    @PostMapping("/complete-grading")
    public RestBean<Boolean> completeGrading(
            @RequestParam Integer examRecordId,
            @RequestParam Integer totalScore) {
        try {
            boolean result = examRecordService.completeGrading(examRecordId, totalScore);
            if (result) {
                return RestBean.success("完成批改成功", true);
            } else {
                return RestBean.failure(500, "完成批改失败");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "完成批改异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取批改数据
     */
    @GetMapping("/grading-data")
    public RestBean<Map<String, Object>> getGradingData(
            @RequestParam Integer examRecordId,
            @RequestParam Integer examId) {
        try {
            Map<String, Object> data = examRecordService.getGradingData(examRecordId, examId);
            if (data != null) {
                return RestBean.success("获取批改数据成功", data);
            } else {
                return RestBean.failure(404, "批改数据不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "获取批改数据异常: " + e.getMessage());
        }
    }
    
    /**
     * 导出答案
     */
    @GetMapping("/export-answers")
    public RestBean<Map<String, Object>> exportAnswers(
            @RequestParam Integer examRecordId,
            @RequestParam Integer studentId,
            @RequestParam Integer examId) {
        try {
            Map<String, Object> data = examRecordService.exportAnswers(examRecordId, studentId, examId);
            if (data != null) {
                return RestBean.success("导出答案成功", data);
            } else {
                return RestBean.failure(404, "导出数据不存在");
            }
        } catch (Exception e) {
            return RestBean.failure(500, "导出答案异常: " + e.getMessage());
        }
    }
}