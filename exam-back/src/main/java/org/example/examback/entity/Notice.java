package org.example.examback.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/*
 * @Author:总会落叶
 * @Date:2025/10/28
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notice {
    private User user;
    private Integer id,userId;
    private String title,content,type,priority,receive;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date time;

    public Notice(User user, Integer id, String title, String content, Date time, Integer userId, String type, String priority, String receive) {
        this.user = user;
        this.id = id;
        this.title = title;
        this.content = content;
        this.time = time;
        this.userId = userId;
        this.type = type;
        this.priority = priority;
        this.receive = receive;
    }
}
