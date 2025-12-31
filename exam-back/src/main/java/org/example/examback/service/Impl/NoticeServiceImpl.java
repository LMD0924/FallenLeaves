package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import org.example.examback.config.NotificationWebSocketHandler;
import org.example.examback.entity.Notice;
import org.example.examback.entity.User;
import org.example.examback.mapper.NoticeMapper;
import org.example.examback.mapper.UserMapper;
import org.example.examback.service.NoticeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/10/28
 * @Description:
 */
@Service
public class NoticeServiceImpl implements NoticeService {
    @Resource
    NoticeMapper noticeMapper;
    @Resource
    UserMapper userMapper;
    //发布消息
    @Override
    public void InsertNotice(Notice notice){
        noticeMapper.InsertNotice(notice);
        //发送websocket通知
        NotificationWebSocketHandler.broadcastNotice(notice);
    }
    //获取自己发布的消息
    @Override
    public List<Notice> SelectNoticeById(Integer id){
        return noticeMapper.SelectNoticeById(id);
    }
    //获取所有消息
    @Override
    public List<Notice> SelectAllNotice(){
        List<Notice> list=new ArrayList<>();
        for(Notice notice:noticeMapper.SelectAllNotice()){
            User user=userMapper.getUserById(notice.getUserId());
            user.setPassword("*");
            list.add(new Notice(user,notice.getId(),notice.getTitle(),notice.getContent(),notice.getTime(),notice.getUserId(),notice.getType(),notice.getPriority(),notice.getReceive()));
        }
        return list;
    }
    //修改消息
    @Override
    public void UpdateNotice(Notice notice){
        noticeMapper.UpdateNotice(notice);
    }
}
