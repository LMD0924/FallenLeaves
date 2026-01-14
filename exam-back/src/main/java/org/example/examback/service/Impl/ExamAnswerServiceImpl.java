package org.example.examback.service.Impl;

import org.example.examback.entity.ExamAnswer;
import org.example.examback.mapper.ExamAnswerMapper;
import org.example.examback.service.ExamAnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 考试答案服务实现类
 */
@Service
public class ExamAnswerServiceImpl implements ExamAnswerService {

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Override
    public int insertBatch(List<ExamAnswer> list) {
        System.out.println("ExamAnswerServiceImpl.insertBatch: 插入" + list.size() + "条答案数据");
        return examAnswerMapper.insertBatch(list);
    }

    @Override
    public List<Map<String, Object>> selectObjectiveAnswers(Integer examRecordId, Integer examId) {
        return examAnswerMapper.selectObjectiveAnswers(examRecordId, examId);
    }

    @Override
    public List<ExamAnswer> selectByExamRecordId(Integer examRecordId) {
        return examAnswerMapper.selectByExamRecordId(examRecordId);
    }

    @Override
    public ExamAnswer selectByRecordAndQuestion(Integer examRecordId, Integer questionId) {
        return examAnswerMapper.selectByRecordAndQuestion(examRecordId, questionId);
    }

    @Override
    public int updateBatchScores(List<ExamAnswer> list) {
        // 更新时间
        LocalDateTime now = LocalDateTime.now();
        list.forEach(answer -> {
            answer.setUpdatedAt(now);
        });
        return examAnswerMapper.updateBatchScores(list);
    }

    @Override
    public Integer sumScoreByExamRecordId(Integer examRecordId) {
        return examAnswerMapper.sumScoreByExamRecordId(examRecordId);
    }

    @Override
    public List<ExamAnswer> selectByStudentAndExam(Integer studentId, Integer examId) {
        return examAnswerMapper.selectByStudentAndExam(studentId, examId);
    }

    @Override
    public boolean saveQuestionScore(Integer examRecordId, Integer questionId, Integer score, String comment) {
        ExamAnswer answer = examAnswerMapper.selectByRecordAndQuestion(examRecordId, questionId);
        if (answer != null) {
            answer.setScore(score);
            answer.setTeacherComment(comment);
            answer.setUpdatedAt(LocalDateTime.now());
            // 简单判断是否正确（分数等于题目分数即为正确）
            answer.setIsCorrect(score != null && score > 0 ? 1 : 0);
            List<ExamAnswer> list = List.of(answer);
            return updateBatchScores(list) > 0;
        }
        return false;
    }

    @Override
    public boolean saveAllScores(Integer examRecordId, List<Map<String, Object>> scores) {
        List<ExamAnswer> answers = examAnswerMapper.selectByExamRecordId(examRecordId);
        if (answers.isEmpty()) {
            return false;
        }

        // 构建更新列表
        List<ExamAnswer> updateList = answers.stream()
                .map(answer -> {
                    // 查找对应的分数
                    Map<String, Object> scoreMap = scores.stream()
                            .filter(map -> answer.getQuestionId().equals(map.get("question_id")))
                            .findFirst()
                            .orElse(null);

                    if (scoreMap != null) {
                        Integer score = (Integer) scoreMap.get("score");
                        String comment = (String) scoreMap.get("comment");
                        answer.setScore(score);
                        answer.setTeacherComment(comment);
                        answer.setUpdatedAt(LocalDateTime.now());
                        answer.setIsCorrect(score != null && score > 0 ? 1 : 0);
                    }
                    return answer;
                })
                .toList();

        return updateBatchScores(updateList) > 0;
    }
}