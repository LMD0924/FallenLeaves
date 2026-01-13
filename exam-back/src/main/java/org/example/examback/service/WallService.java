package org.example.examback.service;

import org.example.examback.entity.Wall;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/12/31
 * @Description:
 */
public interface WallService {
    List<Wall> getWallByUserId(Integer userId);
    List<Wall> getWallByRecipientId(Integer recipientId);
    Integer addWall(Wall wall);
    Integer deleteWall(Integer userId,Integer id);
    Integer updateWall(String content,Integer userId,Integer id);
}
