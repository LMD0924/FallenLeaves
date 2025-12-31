package org.example.examback.service;


import org.example.examback.entity.User;

import java.util.Date;
import java.util.List;

public interface UserService {
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
