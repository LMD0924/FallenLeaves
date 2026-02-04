package org.example.examback.service;


import org.example.examback.entity.User;

import java.util.Date;
import java.util.List;

public interface UserService {
    //登录注册
    int Register(User user);
    User Login(String username, String password, String role);
    //手机号登录
    User phoneLogin(String phone);
    //根据id查询信息
    User getUserById(Integer id);
    //获取所有用户
    List<User> getAllUser();
    //获取所有教师
    List<User> AllTeacher();
    //获取所有学生
    List<User> AllStudent();
    //更新信息
    int updateUserInfo(User user);
    //选择专业
    int UpdateUserProfessional(String professional,Integer id);
    //更新头像
    int UpdateUserAvatar(String avatar,Integer id);
    String getAvatar(int id);
    //是否在线
    void updateOnlineStatus(Integer id,Boolean is_online);

    /*
    * 管理员审核
    * */
    int updateStatus(String status,Integer id);
}
