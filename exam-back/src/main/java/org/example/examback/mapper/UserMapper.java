package org.example.examback.mapper;


import org.apache.ibatis.annotations.*;
import org.example.examback.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    //注册
    @Insert("insert into user(account,username,password,role,status) values(#{account},#{username},#{password},#{role},#{status})")
    int Register(User user);
    //登录
    @Select("select * from user where username=#{username} and password=#{password} and role=#{role} and status='审核通过'")
    User Login(String username, String password, String role);
    //手机号登录
    @Select("select * from user where phone=#{phone} and status='审核通过'")
    User phoneLogin(String phone);
    //根据id查询信息
    @Select("SELECT *FROM user WHERE id=#{id}")
    User getUserById(int id);
    //获取所有用户
    @Select("select * from user")
    List<User> getAllUser();
    //获取所有教师
    @Select("select * from user where role='教师'")
    List<User> AllTeacher();
    //获取所有学生
    @Select("select * from user where role='学生'")
    List<User> AllStudent();
    //选择专业
    @Update("update user set professional=#{professional} where id=#{id}")
    int UpdateUserProfessional(String professional,Integer id);
    //获取头像
    @Select("SELECT avatar FROM user WHERE id=#{id}")
    String getAvatar(int id);
    //更换头像
    @Update("update user set avatar=#{avatar} where id=#{id}")
    int updateAvatar(String avatar,int id);
    //更新信息
    @Update("update user set username=#{username}," +
            "account=#{account}," +
            "password=#{password}," +
            "sex=#{sex}," +
            "locality=#{locality}," +
            "general=#{general}," +
            "role=#{role}," +
            "phone=#{phone}," +
            "email=#{email}," +
            "status=#{status}," +
            "follow=#{follow}," +
            "fans=#{fans}," +
            "is_online=#{isOnline}," +
            "professional=#{professional}," +
            "college=#{college}," +
            "end_login_time=#{endLoginTime}" +
            " where id=#{id}")
    int updateUserInfo(User user);
    //关注
    @Update("update user set follow=COALESCE(follow,0)+1 where id=#{userId}")
    int addFollow(Integer userId);
    //增加粉丝
    @Update("update user set fans=COALESCE(fans,0)+1 where id=#{followId}")
    int addFans(Integer followId);
    //取消关注
    @Update("update user set follow=GREATEST(COALESCE(follow,0)-1,0) where id=#{userId}")
    int deleteFollow(Integer userId);
    //减少粉丝
    @Update("update user set fans=GREATEST(COALESCE(fans,0)-1,0) where id=#{followId}")
    int deleteFans(Integer followId);
    //是否在线
    @Update("update user set is_online=#{isOnline} where id=#{id}")
    void updateOnlineStatus(Integer id,Boolean is_online);

    /*
    * 管理员审核
    * */
    @Update("update user set status=#{status} where id=#{id}")
    int updateStatus(String status,Integer id);
}
