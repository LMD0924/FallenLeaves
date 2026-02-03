package org.example.examback.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/*
 * @Author:总会落叶
 * @Date:2026/2/2
 * @Description:
 */
@Component
public class SmsCodeUtil {
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    private final int codeLength =6; //验证码长度
    private final int expireMinutes = 2; //验证码有效期（分钟）
    private String redisPrefix = "sms"; //验证码在Redis中的前缀
    private final int resendInterval = 2; //重新发送验证码的间隔时间（分钟）

    /*
    * 生成短信验证码
    * */
    public String generateCode(){
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for(int i=0;i<codeLength;i++){
            code.append(random.nextInt(10)); //生成0-9的随机数
        }
        return code.toString();
    }

    /*
    * 发送短信验证码
    * */
    public String sendSmsCode(String phone){
        //1.检查是否可以重发
        String resendKey = redisPrefix + ":resend:" + phone;
        if(Boolean.TRUE.equals(redisTemplate.hasKey(resendKey))){
            return null;
        }

        //2.生成验证码
        String code = generateCode();
        //3.存储到redis中
        String codeKey = redisPrefix + ":code:" + phone;
        redisTemplate.opsForValue().set(codeKey,code,expireMinutes, TimeUnit.MINUTES);

        //4.设置重发限制
        redisTemplate.opsForValue().set(resendKey,"1",resendInterval,TimeUnit.MINUTES);

        //5.实际发送短信,直接返回给前端
        System.out.println("【模拟短信】发送给 " + phone + "，验证码：" + code);
        System.out.println("验证码有效期：" + expireMinutes + "分钟");
        return code;
    }

    /*
    * 验证短信验证码
    * */
    public boolean verifyCode(String phone,String code){
        String key = redisPrefix + ":code:" + phone;
        String storedCode = (String) redisTemplate.opsForValue().get(key);
        if(storedCode == null){
            return false; //验证码不存在或者已经过期
        }
        boolean isValid = storedCode.equals(code); //验证码是否匹配
        if(isValid){
            redisTemplate.delete(key); //验证成功后删除验证码
        }
        return isValid;
    }

    /*
    * 获取剩余过期时间
    * */
    public Long getExpireTime(String phone){
        String key = redisPrefix + ":code:" + phone;
        return redisTemplate.getExpire(key,TimeUnit.SECONDS);
    }

    /*
    * 检查是否可以重发
    * */
    public boolean canResend(String phone){
        String resendKey = redisPrefix + ":resend:" + phone;
        return !redisTemplate.hasKey(resendKey);
    }

    /*
    * 获取重发等待的时间
    * */
    public Long getResendWaitTime(String phone){
        String resendKey = redisPrefix + ":resend:" + phone;
        return redisTemplate.getExpire(resendKey,TimeUnit.SECONDS);
    }
}
