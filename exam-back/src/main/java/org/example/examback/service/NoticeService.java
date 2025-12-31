package org.example.examback.service;

import org.example.examback.entity.Notice;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/10/28
 * @Description:
 */
public interface NoticeService {
    //发布消息
    public void InsertNotice(Notice notice);
    //获取消息
    public List<Notice> SelectNoticeById(Integer id);
    //获取消息
    public List<Notice> SelectAllNotice();
    //修改消息
    public void UpdateNotice(Notice notice);
}
