package org.example.examback.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/*
 * @Author:总会落叶
 * @Date:2025/12/31
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wall {
    private Integer id;
    private String userId;//发送者的id
    private String content;//内容
    private Date time;
    private String userName;//发送者的名字
    private String userAvatar;//发送者的头像
    private Integer recipientId;//接收者的id
}
