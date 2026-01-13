package org.example.examback.mapper;

import org.apache.ibatis.annotations.*;
import org.example.examback.entity.Wall;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/12/31
 * @Description:
 */
@Mapper
public interface WallMapper {
    //查询自己发送给别人的留言
    @Select("select * from wall where userId=#{userId}")
    List<Wall> getWallByUserId(Integer userId);
    //查询别人发送给自己的留言
    @Select("select * from wall where recipientId in (#{recipientId}) or recipientId=0")
    List<Wall> getWallByRecipientId(Integer recipientId);
    //留言
    @Insert("insert into wall(userId,userName,userAvatar,recipientId,content) values(#{userId},#{userName},#{userAvatar},#{recipientId},#{content})")
    Integer addWall(Wall wall);
    //删除留言
    @Delete("delete from wall where userId=#{userId} and id=#{id}")
    Integer deleteWall(Integer userId,Integer id);
    //修改
    @Update("update wall set content=#{content} where userId=#{userId} and id=#{id}")
    Integer updateWall(String content,Integer userId,Integer id);
}
