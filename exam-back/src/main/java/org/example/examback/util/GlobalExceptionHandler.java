package org.example.examback.util;

import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.RestBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/*
 * @Author:总会落叶
 * @Date:2025/12/29
 * @Description:
 */
//全局异常处理，处理项目中抛出的业务异常
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //捕获业务异常

   //捕获用户名相同的异常信息
    @ExceptionHandler(DuplicateKeyException.class)
    public RestBean<String> handleUsernameDuplicateException(DuplicateKeyException ex){
        //Duplicate entry 'admin' for key 'user.uk_username'
        //获取异常信息
        String message = ex.getMessage();
        //判断异常信息中是否包含"Duplicate entry"
        if(message != null && message.contains("Duplicate entry")){
            try {
                //截取异常信息中的用户名
                String[] split=message.split(" '");
                if(split.length > 1) {
                    String username=split[1].split("'")[0];
                    //返回异常信息
                    return RestBean.info("用户名已存在："+username);
                }
            } catch (Exception e) {
                log.error("解析DuplicateKeyException异常信息失败", e);
            }
        }
        return RestBean.failure("系统错误："+ex.getMessage());
    }

    /**
     * 捕获SQL完整性约束违反异常
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public RestBean<String> handleSQLIntegrityConstraintViolationException(SQLIntegrityConstraintViolationException ex){
        log.error("SQL完整性约束违反异常：", ex);
        String message = ex.getMessage();
        if(message != null && message.contains("Duplicate entry")){
            try {
                String[] split=message.split(" '");
                if(split.length > 1) {
                    String value=split[1].split("'")[0];
                    return RestBean.info("数据已存在："+value);
                }
            } catch (Exception e) {
                log.error("解析SQLIntegrityConstraintViolationException异常信息失败", e);
            }
        }
        return RestBean.failure("数据操作失败："+ex.getMessage());
    }

    /**
     * 捕获空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public RestBean<String> handleNullPointerException(NullPointerException ex){
        log.error("空指针异常：", ex);
        return RestBean.failure("系统错误：数据为空，请检查输入参数");
    }

    /**
     * 捕获类型转换异常
     */
    @ExceptionHandler(ClassCastException.class)
    public RestBean<String> handleClassCastException(ClassCastException ex){
        log.error("类型转换异常：", ex);
        return RestBean.failure("系统错误：数据类型转换失败");
    }

    /**
     * 捕获非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RestBean<String> handleIllegalArgumentException(IllegalArgumentException ex){
        log.error("非法参数异常：", ex);
        return RestBean.failure("参数错误："+ex.getMessage());
    }

    /**
     * 捕获所有异常
     */
    @ExceptionHandler(Exception.class)
    public RestBean<String> exceptionHandler(Exception ex) {
        log.error("系统异常：", ex);  // 记录完整异常堆栈
        return RestBean.failure("系统错误：" + (ex.getMessage() != null ? ex.getMessage() : "未知错误"));
    }

}
