package org.example.examback.mapper;


import org.apache.ibatis.annotations.*;
import org.example.examback.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    //登录注册
    @Insert("insert into user(account,username,password,role,status) values(#{account},#{username},#{password},#{role},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int InsertUser(User user);

    @Insert("<script>" +
            "INSERT INTO " +
            "<if test='role == \"管理员\"'>xm_admin</if>" +
            "<if test='role == \"教师\"'>xm_teacher</if>" +
            "<if test='role == \"学生\"'>xm_student</if>" +
            " (user_id, account, username, password, role, status) " +
            "VALUES (#{user_id}, #{account}, #{username}, #{password}, #{role}, #{status})" +
            "</script>")
    int InsertXm(String account, String username, String password, String role, String status, Integer user_id);

    //登录
    @Select("select * from user where username=#{username} and password=#{password} and role=#{role} and status='审核通过'")
    User ExamLogin(String username, String password, String role);
    //根据id查询信息
    @Select("select * from user where id=#{id}")
    User SelectById(Integer id);
    //获取所有用户
    @Select("select * from user")
    List<User> AllUser();
    //获取所有教师
    @Select("select * from xm_teacher")
    List<User> AllTeacher();
    //获取所有学生
    @Select("select * from xm_student")
    List<User> AllStudent();
    //更新信息
    @Update("update user set account=#{account},username=#{username},password=#{password},role=#{role},status=#{status},phone=#{phone},email=#{email},sex=#{sex},locality=#{locality},general=#{general},professional=#{professional} where id=#{id}")
    int UpdateUserInfo(User user);
    @Update("<script>" +
            "UPDATE " +
            "<choose>" +
            "  <when test='role == &quot;管理员&quot;'>xm_admin</when>" +
            "  <when test='role == &quot;教师&quot;'>xm_teacher</when>" +
            "  <when test='role == &quot;学生&quot;'>xm_student</when>" +
            "</choose> " +
            "SET account=#{account}, " +
            "username=#{username}, " +
            "password=#{password}, " +
            "role=#{role}, " +
            "status=#{status}, " +
            "phone=#{phone}, " +
            "email=#{email}, " +
            "professional=#{professional} " +
            "WHERE user_id=#{user_id}" +
            "</script>")
    int UpdateXm(User user);
    //选择专业
    @Update("update user set professional=#{professional} where id=#{id}")
    int UpdateUserProfessional(String professional,Integer id);
    //更新头像
    @Update("update user set avatar=#{avatar} where id=#{id}")
    int UpdateUserAvatar(String avatar,Integer id);
    @Update("<script>" +
            "update " +
            "<choose>" +
            "<when test='role == &quot;管理员&quot;'>xm_admin</when>" +
            "<when test='role == &quot;教师&quot;'>xm_teacher</when>" +
            "<when test='role == &quot;学生&quot;'>xm_student</when>" +
            "</choose>" +
            " set avatar=#{avatar} where user_id=#{user_id}" +
            "</script>")
    int UpdateXmAvatar(String avatar, Integer user_id, String role);
//    @Select("select * from user where account = #{account} and password = #{password}")
//    User login(String account, String password);
    @Select("SELECT *FROM user WHERE id=#{id}")
    User getUserById(int id);

    @Select("SELECT *FROM user ")
    List<User> getAllUser();

    @Select("SELECT avatar FROM user WHERE id=#{id}")
    String getAvatar(int id);
//    @Insert("insert into user (account,username,password) values (#{account},#{username},#{password})")
//    void register(String account,String username,String password);
    //更换头像
    @Update("update user set avatar=#{avatar} where id=#{id}")
    void updateAvatar(String avatar,int id);
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
    void updateUser(User user);
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
}
