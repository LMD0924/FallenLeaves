package org.example.examback.mapper;

import org.apache.ibatis.annotations.*;
import org.example.examback.entity.ExamRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * @Author:总会落叶
 * @Date:2026/1/13
 * @Description:
 */
@Mapper
public interface ExamRecordMapper {
    // 根据考试ID和学生ID查找考试记录
    @Select("SELECT * FROM xm_exam_record WHERE exam_id = #{examId} AND student_id = #{studentId} ORDER BY id DESC LIMIT 1")
    ExamRecord selectByExamAndStudent(@Param("examId") Integer examId, @Param("studentId") Integer studentId);

    // 更新考试记录状态和分数
    @Update("UPDATE xm_exam_record SET status = #{status}, total_score = #{totalScore}, updated_at = NOW() WHERE id = #{id}")
    int updateStatusAndScore(@Param("id") Integer id, @Param("status") String status, @Param("totalScore") Integer totalScore);

    // 插入考试记录
    @Insert("INSERT INTO xm_exam_record (exam_id, student_id, start_time, end_time, total_score, status, time_spent, attempt_count, created_at, updated_at) " +
            "VALUES (#{examId}, #{studentId}, #{startTime}, #{endTime}, #{totalScore}, #{status}, #{timeSpent}, #{attemptCount}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamRecord examRecord);

    // 根据ID查询考试记录
    @Select("SELECT * FROM xm_exam_record WHERE id = #{id}")
    ExamRecord selectById(@Param("id") Integer id);

    // 更新考试时间和用时
    @Update("UPDATE xm_exam_record SET end_time = #{endTime}, time_spent = #{timeSpent}, updated_at = NOW() WHERE id = #{id}")
    int updateTime(@Param("id") Integer id, @Param("endTime") LocalDateTime endTime, @Param("timeSpent") Integer timeSpent);

    // 查询考试记录总数（用于分页）- 修复版
    @Select("<script>" +
            "SELECT COUNT(*) FROM xm_exam_record rer " +
            "LEFT JOIN xm_user u ON rer.student_id = u.id " +
            "<where>" +
            "  <if test='examId != null'>" +
            "    AND rer.exam_id = #{examId}" +
            "  </if>" +
            "  <if test='status != null and status != \"\"'>" +
            "    AND rer.status = #{status}" +
            "  </if>" +
            "  <if test='search != null and search != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{search}, '%') OR u.real_name LIKE CONCAT('%', #{search}, '%'))" +
            "  </if>" +
            "</where>" +
            "</script>")
    int countExamRecords(@Param("examId") Integer examId,
                         @Param("status") String status,
                         @Param("search") String search);

    // 分页查询考试记录 - 修复版
    @Select("<script>" +
            "SELECT rer.*, u.username, u.real_name, e.title AS exam_title " +
            "FROM xm_exam_record rer " +
            "LEFT JOIN xm_user u ON rer.student_id = u.id " +
            "LEFT JOIN xm_exam e ON rer.exam_id = e.id " +
            "<where>" +
            "  <if test='examId != null'>" +
            "    AND rer.exam_id = #{examId}" +
            "  </if>" +
            "  <if test='status != null and status != \"\"'>" +
            "    AND rer.status = #{status}" +
            "  </if>" +
            "  <if test='search != null and search != \"\"'>" +
            "    AND (u.username LIKE CONCAT('%', #{search}, '%') OR u.real_name LIKE CONCAT('%', #{search}, '%'))" +
            "  </if>" +
            "</where>" +
            "ORDER BY rer.created_at DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<Map<String, Object>> selectExamRecordsByPage(
            @Param("examId") Integer examId,
            @Param("status") String status,
            @Param("search") String search,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    // 根据考试ID查询所有考试记录
    @Select("SELECT * FROM xm_exam_record WHERE exam_id = #{examId} ORDER BY created_at DESC")
    List<ExamRecord> selectByExamId(@Param("examId") Integer examId);

    // 根据学生ID查询所有考试记录
    @Select("SELECT * FROM xm_exam_record WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<ExamRecord> selectByStudentId(@Param("studentId") Integer studentId);
}
