package org.example.examback.controller;



import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.RestBean;
import org.example.examback.entity.User;
import org.example.examback.service.UserService;
import org.example.examback.util.FileUploadUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    UserService userService;
    @Resource
    FileUploadUtil fileUploadUtil;
    //获取所有教师和学生
    @GetMapping("/AllTeacher")
    public RestBean<List<User>> AllTeacher(){
        List<User> list=userService.AllTeacher();
        for(User user:list){
            user.setPassword("**不给看**");
        }
        return RestBean.success("获取成功",list);
    }
    @GetMapping("/AllStudent")
    public RestBean<List<User>> AllStudent(){
        List<User> list=userService.AllStudent();
        for(User user:list){
            user.setPassword("**不给看**");
        }
        return RestBean.success("获取成功",list);
    }
    //选择专业
    @PostMapping("UpdateUserProfessional")
    public RestBean<Integer> UpdateUserProfessional(@RequestParam("professional") String professional,
                                                    @RequestParam("id") Integer id){
        int result=userService.UpdateUserProfessional(professional,id);
        if(result!=0) return RestBean.success("选择专业成功",result);
        else return RestBean.failure(404,"选择专业失败");
    }
    //更换头像
    @PostMapping("/updateAvatar")
    public RestBean<String> updateAvatar(@RequestParam("file") MultipartFile file,
                                         HttpServletRequest request) {
        try {
            //验证文件类型
            if (!fileUploadUtil.isImageFile(file)) {
                return RestBean.failure(400, "只能上传图片文件");
            }
            //验证文件大小
            if (file.getSize() > 10 * 1024 * 1024) {
                return RestBean.failure(400, "文件大小不能超过10MB");
            }
            //上传文件并获取URL
            String avatarUrl = fileUploadUtil.uploadFile(file);
            //更新用户头像
            Integer userId = (Integer) request.getAttribute("id");
            User user = userService.getUserById(userId);
            user.setAvatar(avatarUrl);
            int result = userService.UpdateUserAvatar(avatarUrl, userId);
            if (result != 0) return RestBean.success("上传成功", avatarUrl);
            else return RestBean.failure(404, "上传失败");
        } catch (Exception e) {
            return RestBean.failure(500, "上传失败");
        }
    }
    @GetMapping("/information")
    public RestBean<User> getUserInfo(HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
        return RestBean.success("成功",user);
    }
//获取个人信息
    @GetMapping("/current")
    public RestBean<User> getCurrentUser(HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        User user=userService.getUserById(userId);
        if(user == null) {
            return RestBean.failure(404,"用户不存在");
        }
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

    //更改个人信息
    @PostMapping("/updateUserInfo")
    public RestBean<String> updateUserInfo(@ModelAttribute @Valid User user,
                                       HttpServletRequest request){
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        user.setId(userId);
        userService.updateUserInfo(user);
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
        Integer userId=(Integer) request.getAttribute("id");
        if(userId == null) {
            return RestBean.failure(401,"未登录或token无效");
        }
        userService.updateOnlineStatus(userId, is_online);
        return RestBean.success("更新成功");
    }
    @ExceptionHandler(Exception.class)
    public RestBean<String> exceptionHandler(Exception ex){
        log.error("系统异常：", ex);  // 记录完整异常堆栈
        return RestBean.failure("系统错误：" + (ex.getMessage() != null ? ex.getMessage() : "未知错误"));
    }

    /*
    * 管理员审核
    * */
    @PostMapping("/updateStatus")
    public RestBean<String> updateStatus(String status,Integer id){
        userService.updateStatus(status,id);
        return RestBean.success("更新成功");
    }
}
