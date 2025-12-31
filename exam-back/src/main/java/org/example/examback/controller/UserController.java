package org.example.examback.controller;



import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.example.examback.entity.RestBean;
import org.example.examback.entity.User;
import org.example.examback.service.UserService;
import org.example.examback.util.FileUploadUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    UserService userService;
    @Resource
    FileUploadUtil fileUploadUtil;
    @GetMapping("/information")
    public RestBean<User> getUserInfo(HttpServletRequest request){
        System.out.println("id:"+request.getAttribute("id"));
        Integer userId=(Integer) request.getAttribute("id");
        User user=userService.getUserById(userId);
        return RestBean.success("成功",user);
    }
//获取个人信息
    @GetMapping("/current")
    public RestBean<User> getCurrentUser(HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        User user=userService.getUserById(userId);
        return RestBean.success("获取成功",user);
    }
    @GetMapping("/AllUser")
    public RestBean<List<User>> getAllUser(){
        List<User> users=userService.getAllUser();
        return RestBean.success("获取成功",users);
    }

    @GetMapping("/getAvatarByUserId")
    public RestBean<String> getAvatarById(@RequestParam("id")Integer id){
        String avatar=userService.getAvatar(id);
        return RestBean.success("获取成功",avatar);
    }
    //更换头像
    @PostMapping("/updateAvatar")
    public RestBean<String> updateAvatar(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request){
        try{
            //验证文件类型
            if(!fileUploadUtil.isImageFile(file)){
                return RestBean.failure(400,"只能上传图片文件");
            }
            //验证文件大小
            if(file.getSize()>10*1024*1024){
                return RestBean.failure(400,"文件大小不能超过10MB");
            }
            //上传文件并获取URL
            String avatarUrl=fileUploadUtil.uploadFile(file);
            //更新用户头像
            Integer userId=(Integer) request.getAttribute("id");
            User user=userService.getUserById(userId);
            user.setAvatar(avatarUrl);
            userService.updateAvatar(avatarUrl,userId);
            return RestBean.success("上传成功",avatarUrl);
        }catch (Exception e){
            return RestBean.failure(500,"上传失败");
        }
    }
    //更改个人信息
        @PostMapping("/updateUser")
        public RestBean<String> updateUser(@ModelAttribute User user,
                                           HttpServletRequest request){
            Integer userId=(Integer) request.getAttribute("id");
            user.setId(userId);
            userService.updateUser(user);
            return RestBean.success("更新成功");
        }
    //获取指定用户信息
    @GetMapping("/getUserById")
    public RestBean<User> getUserById(@RequestParam("id") Integer id) {
        User user = userService.getUserById(id);
        if (user != null) {
            return RestBean.success("获取成功", user);
        } else {
            return RestBean.failure(404, "用户不存在");
        }
    }
    //是否在线
    @PostMapping("/UpdateOnline")
    public RestBean<String> updateOnlineStatus(@RequestParam("is_online") Boolean is_online,
                                               HttpServletRequest request){
        int userId=(Integer) request.getAttribute("id");
        userService.updateOnlineStatus(userId, is_online);
        return RestBean.success("更新成功");
    }
}
