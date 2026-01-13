package org.example.examback.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.examback.entity.RestBean;
import org.example.examback.entity.Wall;
import org.example.examback.service.WallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * @Author:总会落叶
 * @Date:2025/12/31
 * @Description:
 */
@RestController
@RequestMapping("api/wall")
public class WallController {
    @Autowired
    private WallService wallService;
    @GetMapping("/getWallByUserId")
    public RestBean<List<Wall>> getWallByUserId(HttpServletRequest request){
        Integer userId=(Integer)request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        System.out.println("用户Id:"+userId);
        List<Wall> result=wallService.getWallByUserId(userId);
        if(result==null){
            return RestBean.failure(404,"获取失败");
        }
        return RestBean.success("获取成功", result);
    }
    @GetMapping("/getWallByRecipientId")
    public RestBean<List<Wall>> getWallByRecipientId(HttpServletRequest request){
        Integer recipientId=(Integer)request.getAttribute("id");
        if(recipientId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        System.out.println("用户的Id:"+recipientId);
        List<Wall> result=wallService.getWallByRecipientId(recipientId);
        if(result==null){
            return RestBean.failure(404,"获取失败");
        }
        return RestBean.success("获取成功", result);
    }
    @PostMapping("/addWall")
    public RestBean<Integer> addWall(Wall wall, HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        if(wall == null) {
            return RestBean.failure(400,"参数错误：wall不能为空");
        }
        if(wall.getContent() == null || wall.getContent().trim().isEmpty()) {
            return RestBean.failure(400,"参数错误：内容不能为空");
        }
        // 设置发送者ID - 需要根据Wall实体类的实际字段名调整
        // 如果Wall实体类中userId是String类型，需要转换
        if(wall.getUserId() == null) {
            wall.setUserId(String.valueOf(userId));
        }
        Integer result=wallService.addWall(wall);
        if(result==0){
            return RestBean.failure(500,"添加失败");
        }
        return RestBean.success("添加成功", result);
    }
    @DeleteMapping("/deleteWall")
    public RestBean<Integer> deleteWall(HttpServletRequest request,Integer id){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        if(id == null) {
            return RestBean.failure(400,"参数错误：id不能为空");
        }
        Integer result=wallService.deleteWall(userId,id);
        if(result==0){
            return RestBean.failure(404,"删除失败");
        }
        return RestBean.success("删除成功", result);
    }
    @PostMapping("/updateWall")
    public RestBean<Integer> updateWall(String content,Integer id,HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        if(id == null) {
            return RestBean.failure(400,"参数错误：id不能为空");
        }
        if(content == null || content.trim().isEmpty()) {
            return RestBean.failure(400,"参数错误：内容不能为空");
        }
        Integer result=wallService.updateWall(content,userId,id);
        if(result==0){
            return RestBean.failure(404,"更新失败");
        }
        return RestBean.success("更新成功", result);
    }
}
