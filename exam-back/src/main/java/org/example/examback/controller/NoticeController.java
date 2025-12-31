package org.example.examback.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.examback.entity.Notice;
import org.example.examback.entity.RestBean;
import org.example.examback.entity.User;
import org.example.examback.service.NoticeService;
import org.example.examback.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/10/28
 * @Description:
 */
@RestController
@RequestMapping("api/notice")
public class NoticeController {
    @Resource
    NoticeService noticeService;
    @Resource
    UserService userService;
    //发布消息
    @PostMapping("/InsertNotice")
    public RestBean<String> InsertNotice(Notice notice,
                                         HttpServletRequest request){
        if(notice.getUserId()==null) return RestBean.failure(404,"请先登录");
        int userId=(int) request.getAttribute("id");
        User user=userService.getUserById(userId);
        if(user.getRole().equals("管理员")){
            noticeService.InsertNotice(notice);
            return RestBean.success("发布成功",null);
        }else return RestBean.failure(404,"暂无权限发布消息");
    }
    //获取自己发布的消息
    @GetMapping("/SelectNoticeById")
    public RestBean<List<Notice>> SelectNoticeById(@RequestParam("id") Integer id,
                                                   HttpServletRequest request){
        int userId=(int) request.getAttribute("id");
        if(userId==id) return RestBean.success("获取成功",noticeService.SelectNoticeById(id));
        else return RestBean.failure(404,"暂无权限获取消息");
    }
    //获取所有消息
    @GetMapping("SelectAllNotice")
    public RestBean<List<Notice>> SelectAllNotice(){
        return RestBean.success("获取成功",noticeService.SelectAllNotice());
    }
    //修改消息
    @PostMapping("UpdateNotice")
    public RestBean<String> UpdateNotice(Notice notice,
                                         HttpServletRequest request){
        int userId=(int) request.getAttribute("id");
        User user=userService.getUserById(userId);
        if(user.getRole().equals("管理员")){
            noticeService.UpdateNotice(notice);
            return RestBean.success("修改成功",null);
        }else return RestBean.failure(404,"暂无权限修改消息");
    }
}
