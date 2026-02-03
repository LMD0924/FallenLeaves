package org.example.examback.service;


import org.example.examback.entity.User;

import java.util.Date;
import java.util.List;

public interface UserService {
    //登录注册
    int InsertUser(String account,String username,String password,String role,String status);
    User ExamLogin(String username, String password, String role);
    //手机号登录
    User phoneLogin(String phone);
    //根据id查询信息
    User SelectById(Integer id);
    //获取所有用户
    List<User> AllUser();
    //获取所有教师
    List<User> AllTeacher();
    //获取所有学生
    List<User> AllStudent();
    //更新信息
    int UpdateUserInfo(User user);
    //选择专业
    int UpdateUserProfessional(String professional,Integer id);
    //更新头像
    int UpdateUserAvatar(String avatar,Integer id);
  //  User login(String account , String password);
    User getUserById(int id);
    List<User> getAllUser();
    String getAvatar(int id);
   // boolean register(User user);
    //更换头像
    void updateAvatar(String avatar,int id);
    //更改个人信息
    void updateUser(User user);
    //是否在线
    void updateOnlineStatus(Integer id,Boolean is_online);
}
