package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.User;
import org.example.examback.mapper.UserMapper;
import org.example.examback.service.UserService;
import org.example.examback.util.UserMergeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserMapper userMapper;
    //登陆注册
    @Override
    @Transactional
    public int InsertUser(String account,String username,String password,String role,String status){
        try {
            //先插入User表，获取自动生成的id
            User user = new User();
            user.setAccount(account);
            user.setUsername(username);
            user.setPassword(password);
            user.setRole(role);
            user.setStatus(status);

            int result = userMapper.InsertUser(user);
            if (result != 1) throw new RuntimeException("插入User表失败");

            // 获取自动生成的id
            Integer userId = user.getId();
            if (userId == null) throw new RuntimeException("获取用户ID失败");

            //再插入对应角色表
            result = userMapper.InsertXm(account, username, password, role, status, userId);
            if (result != 1) throw new RuntimeException("插入对应表失败");
            return 1;
        }catch(Exception e){
            log.error("注册失败：",e);
            throw e;
        }
    }
    @Override
    public User ExamLogin(String username,String password,String role){
        return userMapper.ExamLogin(username, password, role);
    }
    //根据id查询信息
    @Override
    public User SelectById(Integer id){
        return userMapper.SelectById(id);
    }
    //获取所有用户
    @Override
    public List<User> AllUser(){
        return userMapper.AllUser();
    }
    //获取所有教师
    @Override
    public List<User> AllTeacher(){
        return userMapper.AllTeacher();
    }
    //获取所有学生
    @Override
    public List<User> AllStudent(){
        return userMapper.AllStudent();
    }
    //更新信息
    @Override
    @Transactional
    public int UpdateUserInfo(User user){
        try{
            if("**不给看**".equals(user.getPassword())) {
                user.setPassword(SelectById(user.getId()).getPassword());
            }
            //先更新User表
            int result=userMapper.UpdateUserInfo(user);
            if(result==0) throw new RuntimeException("更新user表失败");
            //在更新对应角色表
            result=userMapper.UpdateXm(user);
            if(result==0) throw new RuntimeException("更新对应角色表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("更新失败",e.getCause());
        }
    }
    //选择专业
    @Override
    public int UpdateUserProfessional(String professional,Integer id){
        return userMapper.UpdateUserProfessional(professional,id);
    }
    //更新头像
    @Override
    @Transactional
    public int UpdateUserAvatar(String avatar,Integer id){
        try{
            // 先获取用户信息以获取角色
            User user = userMapper.SelectById(id);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            int result=userMapper.UpdateUserAvatar(avatar,id);
            if(result==0) throw new RuntimeException("更新user表失败");

            result=userMapper.UpdateXmAvatar(avatar,id,user.getRole());
            if(result==0) throw new RuntimeException("更新对应角色表失败");
            return 1;
        }catch(Exception e){
            throw new RuntimeException("更新失败",e.getCause());
        }
    }
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
    public void updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        // 获取当前用户
        User currentUser = userMapper.getUserById(user.getId());
        if (currentUser == null) {
            throw new RuntimeException("用户不存在");
        }
    User mergerUser= UserMergeUtil.merge(user,currentUser);

        // 更新合并后的用户
        userMapper.updateUser(mergerUser);
    }

    @Override
    public void updateOnlineStatus(Integer id, Boolean is_online) {
        userMapper.updateOnlineStatus(id,is_online);
    }
}
