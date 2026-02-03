package org.example.examback.controller.auth;

import jakarta.annotation.Resource;
import org.example.examback.entity.RestBean;
import org.example.examback.service.ExamService;
import org.example.examback.service.UserService;
import org.example.examback.entity.User;
import org.example.examback.util.SmsCodeUtil;
import org.example.examback.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.examback.JWT.JWTUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource
    UserService userService;
    @Autowired
    private SmsCodeUtil smsCodeUtil;
    @Autowired
    private ValidationUtil validationUtil;

    //登陆注册
    @PostMapping("/ExamRegister")
    public RestBean<Integer> ExamRegister(@RequestParam("account") String account,
                                          @RequestParam("username") String username,
                                          @RequestParam("password") String password,
                                          @RequestParam("role") String role,
                                          @RequestParam("status") String status){
        int result=userService.InsertUser(account,username,password,role,status);
        if(result==1){
            return RestBean.success("注册成功，等待管理员审核",result);
        }else return RestBean.failure(404,"注册失败");
    }
    @PostMapping("/ExamLogin")
    public RestBean<String> ExamLogin(@RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      @RequestParam("role") String role){
        User user=userService.ExamLogin(username,password,role);
        if(user==null){
            return RestBean.failure(404,"登录失败，审核未通过或账号密码有误，请联系管理员");
        }else{
            user.setPassword("不给看");
            return RestBean.success("登录成功", JWTUtil.createToken(user));

        }
    }

    /*
    * 发送验证码
    * */
    @PostMapping("/sendCode")
    public RestBean<Map<String,Object>> sendCode(@RequestParam("phone") String phone){
        //验证手机号
        ValidationUtil.ValidationResult validationResult = validationUtil.validatePhone(phone);
        if(!validationResult.isSuccess()){
            //手机号格式不正确
            return RestBean.failure(404,validationResult.getMessage());
        }
        //验证成功，发送验证码
        //检查是否可以重发
        if(!smsCodeUtil.canResend(phone)){
            Long waitTime = smsCodeUtil.getResendWaitTime(phone);
            return RestBean.failure(404,"请等待"+waitTime+"秒后重发");
        }
        //发送验证码
        String code = smsCodeUtil.sendSmsCode(phone);
        if(code!=null){
            Map<String,Object> data = new HashMap<>();
            data.put("code",code);
            //设置验证码过期时间
            data.put("expire",smsCodeUtil.getExpireTime(phone));
            return RestBean.success("验证码发送成功", data);
        }else{
            return RestBean.failure(404,"验证码发送失败");
        }
    }

    /*
     * 获取验证码状态
     * */
    @GetMapping("/status/{phone}")
    public RestBean<Map<String,Object>> getCodeStaus(@PathVariable String phone){
        Long expireTime = smsCodeUtil.getExpireTime(phone);
        boolean canResend = smsCodeUtil.canResend(phone);

        Map<String,Object> data = new HashMap<>();
        data.put("hasCode",expireTime!=null && expireTime>0);
        data.put("expire",expireTime);
        data.put("canResend",canResend);
        if(!canResend){
            data.put("waitTime",smsCodeUtil.getResendWaitTime(phone));
        }
        return RestBean.success("获取验证码状态成功", data);
    }

    /*
    * 手机号，验证码登录
    * */
    @PostMapping("/phoneLogin")
    public RestBean<String> phoneLogin(@RequestParam("phone") String phone,
                                       @RequestParam("code") String code,
                                       @RequestParam("role") String role){
        //验证手机号
        ValidationUtil.ValidationResult validationResult = validationUtil.validatePhone(phone);
        if (!validationResult.isSuccess()) {
            //手机号格式不正确
            return RestBean.failure(404, validationResult.getMessage());
        }
        //验证验证码
        if (!smsCodeUtil.verifyCode(phone, code)) {
            return RestBean.failure(404, "验证码错误");
        }
        //验证成功，登录
        User user = userService.phoneLogin(phone);
        if (user == null) {
            return RestBean.failure(404, "登录失败，审核未通过或账号密码有误，请联系管理员");
        } else {
            user.setPassword("不给看");
            return RestBean.success("登录成功", JWTUtil.createToken(user));
        }
    }
}
