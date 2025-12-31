package org.example.examback.controller.auth;

import jakarta.annotation.Resource;
import org.example.examback.entity.RestBean;
import org.example.examback.service.ExamService;
import org.example.examback.service.UserService;
import org.example.examback.entity.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.examback.JWT.JWTUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource
    UserService userService;
    @Resource
    ExamService examService;
//    @RequestMapping("/login")
//    public RestBean<String> login(@RequestParam("account") String account,
//                                  @RequestParam("password") String password) {
//        User user=userService.login(account,password);
//        if(user==null) return RestBean.failure(401,"账号或密码错误");
//        else
//        {
//            user.setPassword("*");
//            return RestBean.success("登录成功", JWTUtil.createToken(user));
//        }
//    }
//    @PostMapping("register")
//    public RestBean<String> register(@RequestParam("account") String account,
//                                     @RequestParam("username") String username,
//                                     @RequestParam("password") String password){
//       User user=new User();
//       user.setAccount(account);
//       user.setUsername(username);
//       user.setPassword(password);
//      if(userService.register(user)) return RestBean.success("注册成功");
//      else return RestBean.failure(400,"账号已存在");
//    }

    //登陆注册
    @PostMapping("/ExamRegister")
    public RestBean<Integer> ExamRegister(@RequestParam("account") String account,
                                          @RequestParam("username") String username,
                                          @RequestParam("password") String password,
                                          @RequestParam("role") String role,
                                          @RequestParam("status") String status){
        int result=examService.InsertUser(account,username,password,role,status);
        if(result==1){
            return RestBean.success("注册成功，等待管理员审核",result);
        }else return RestBean.failure(404,"注册失败");
    }
    @PostMapping("/ExamLogin")
    public RestBean<String> ExamLogin(@RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      @RequestParam("role") String role){
        User user=examService.ExamLogin(username,password,role);
        if(user==null){
            return RestBean.failure(404,"登录失败，审核未通过或账号密码有误，请联系管理员");
        }else{
            user.setPassword("不给看");
            return RestBean.success("登录成功", JWTUtil.createToken(user));

        }
    }
}
