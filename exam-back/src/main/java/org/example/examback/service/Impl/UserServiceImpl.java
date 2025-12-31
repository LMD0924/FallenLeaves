package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import org.example.examback.entity.User;
import org.example.examback.mapper.UserMapper;
import org.example.examback.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    UserMapper userMapper;

    @Override
    public User getUserById(int id) {
        return userMapper.getUserById(id);
    }

    @Override
    public List<User> getAllUser() {
        return userMapper.getAllUser();
    }

    @Override
    public String getAvatar(int id) {
        return userMapper.getAvatar(id);
    }
//    @Override
//    public boolean register(User user){
//        userMapper.register(user.getAccount(),user.getUsername(),user.getPassword());
//        return true;
//    }
    //更换头像
    @Override
    public void updateAvatar(String avatar,int id){
        userMapper.updateAvatar(avatar,id);
    }
    //修改个人信息
    @Override
    public void updateUser(User user){
        userMapper.updateUser(user);
    }

    @Override
    public void updateOnlineStatus(Integer id, Boolean is_online) {
        userMapper.updateOnlineStatus(id,is_online);
    }
}
