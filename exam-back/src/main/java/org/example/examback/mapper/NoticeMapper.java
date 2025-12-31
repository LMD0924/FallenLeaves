package org.example.examback.mapper;

import org.apache.ibatis.annotations.*;
import org.example.examback.entity.Notice;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/10/28
 * @Description:
 */
@Mapper
public interface NoticeMapper {
    //发布消息
    @Insert("insert into notice(title,content,type,priority,receive,time,userId) values(#{title},#{content},#{type},#{priority},#{receive},#{time},#{userId})")
    void InsertNotice(Notice notice);
    //查询自己发布的消息
    @Select("select * from notice where userId=#{userId")
    List<Notice> SelectNoticeById(Integer userId);
    //查询全部消息
    @Select("select * from notice")
    List<Notice> SelectAllNotice();
    //修改消息
    @Update("update notice set title=#{title},content=#{content},type=#{type},priority=#{priority},receive=#{receive},time=#{time} where id=#{id}")
    void UpdateNotice(Notice notice);
    //删除消息
    @Delete("delete from notice where id=#{id}")
    void DeleteNotice(Integer id);
}
