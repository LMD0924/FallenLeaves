package org.example.examback.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.examback.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
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
    @Update("update user set username=#{username},account=#{account},sex=#{sex},locality=#{locality},general=#{general} where id=#{id}")
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
    @Update("update user set is_online=#{is_online} where id=#{id}")
    void updateOnlineStatus(Integer id,Boolean is_online);
}
