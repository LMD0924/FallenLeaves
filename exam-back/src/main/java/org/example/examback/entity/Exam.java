package org.example.examback.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class Exam {
    private Integer id,course_id,teacher_id,total_score,duration;//id 课程id 教师id 总分数 时长
    private String title,description;//标题 描述
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date start_time;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date end_time;
    private Integer pass_score,attempts_allowed;//通过分数 允许尝试次数
    @JsonProperty("is_public")
    private Boolean is_public;
    private String status;//状态
    private Date created_time,update_time;//创建时间 更新时间
}
