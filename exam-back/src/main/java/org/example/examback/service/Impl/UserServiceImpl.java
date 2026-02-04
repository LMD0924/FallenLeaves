package org.example.examback.service.Impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.User;
import org.example.examback.mapper.UserMapper;
import org.example.examback.service.UserService;
import org.example.examback.util.UserMergeUtil;
import org.example.examback.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.example.examback.util.ValidationUtil.PasswordStrength.MEDIUM;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    UserMapper userMapper;
    @Autowired
    private ValidationUtil validationUtil;
    /*
    * 注册，先添加账号，用户名，密码，身份，状态
    * */
    @Override
    public int Register(User user){
        //先验证密码强度
        ValidationUtil.ValidationResult result = validationUtil.validatePassword(user.getPassword(),MEDIUM);
        if (!result.isSuccess()) {
            throw  new RuntimeException(result.getMessage());
        }
        return userMapper.Register(user);
    }
    @Override
    public User Login(String username,String password,String role){

        return userMapper.Login(username, password, role);
    }

    /*
    * 手机号登录
    * */
    public User phoneLogin(String phone){
        return userMapper.phoneLogin(phone);
    }

    //根据id查询信息
    @Override
    public User getUserById(Integer id){
        return userMapper.getUserById(id);
    }
    //获取所有用户
    @Override
    public List<User> getAllUser(){
        return userMapper.getAllUser();
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
    public int updateUserInfo(User user){
        //先查询以前的个人信息
        User oldUser=getUserById(user.getId());
        //验证更新的个人信息是否符合要求(目前先验证个用户名)
        if(user.getUsername()!=null){
            ValidationUtil.ValidationResult result = validationUtil.validateUsername(user.getUsername());
            if(!result.isSuccess()){
                throw new RuntimeException(result.getMessage());
            }
        }
        //合并用户信息
        User mergerUser= UserMergeUtil.merge(user,oldUser);
        return userMapper.updateUserInfo(mergerUser);
    }
    //选择专业
    @Override
    public int UpdateUserProfessional(String professional,Integer id){
        return userMapper.UpdateUserProfessional(professional,id);
    }
    //更新头像
    @Override
    public int UpdateUserAvatar(String avatar,Integer id){
        return userMapper.updateAvatar(avatar,id);
    }

    @Override
    public String getAvatar(int id) {
        return userMapper.getAvatar(id);
    }

    @Override
    public void updateOnlineStatus(Integer id, Boolean is_online) {
        userMapper.updateOnlineStatus(id,is_online);
    }

    /*
    * 管理员审核
    * */
    @Override
    public int updateStatus(String status,Integer id){
        return userMapper.updateStatus(status,id);
    }
}
