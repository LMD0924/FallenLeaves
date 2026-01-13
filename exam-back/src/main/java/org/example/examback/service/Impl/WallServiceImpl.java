package org.example.examback.service.Impl;

import org.example.examback.entity.Wall;
import org.example.examback.mapper.WallMapper;
import org.example.examback.service.WallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/12/31
 * @Description:
 */
@Service
public class WallServiceImpl implements WallService {
    @Autowired
    private WallMapper wallMapper;

    @Override
    public List<Wall> getWallByUserId(Integer userId) {
        return wallMapper.getWallByUserId(userId);
    }

    @Override
    public List<Wall> getWallByRecipientId(Integer recipientId) {
        return wallMapper.getWallByRecipientId(recipientId);
    }

    @Override
    @Transactional
    public Integer addWall(Wall wall) {
        try {
            System.out.println("【Service】开始添加祝福，数据：" + wall);

            // 调用Mapper
            Integer result = wallMapper.addWall(wall);

            System.out.println("【Service】Mapper返回结果：" + result);
            System.out.println("【Service】生成的主键ID：" + wall.getId());

            // 关键修复：确保不返回null
            if (result == null) {
                System.out.println("【Service】警告：Mapper返回null，返回0");
                return 0;
            }

            return result;
        } catch (Exception e) {
            System.err.println("【Service】添加祝福异常：" + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    @Transactional
    public Integer deleteWall(Integer userId, Integer id) {
        try {
            Integer result = wallMapper.deleteWall(userId, id);
            return result != null ? result : 0;
        } catch (Exception e) {
            System.err.println("删除祝福异常：" + e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional
    public Integer updateWall(String content, Integer userId, Integer id) {
        try {
            Integer result = wallMapper.updateWall(content, userId, id);
            return result != null ? result : 0;
        } catch (Exception e) {
            System.err.println("更新祝福异常：" + e.getMessage());
            return 0;
        }
    }
}
