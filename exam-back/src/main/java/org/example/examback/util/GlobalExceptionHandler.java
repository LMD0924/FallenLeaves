package org.example.examback.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BaseExecutor;
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
        // 获取根异常信息
        String message = ex.getMostSpecificCause().getMessage();
        //判断异常信息中是否包含"Duplicate entry"
        if(message.contains("Duplicate entry")){
            //截取异常信息中的用户名
            String[] split=message.split(" '");
            String username=split[1].split("'")[0];
            //返回异常信息，使用warning响应以便前端正确处理
            return RestBean.warning("用户名已存在："+username);
        }
        return RestBean.failure("系统错误："+ex.getMessage());
    }

    /**
     * 捕获所有异常
     */
//    @ExceptionHandler(Exception.class)
//    public RestBean<String> exceptionHandler(Exception ex) {
//        log.error("系统异常：", ex);  // 记录完整异常堆栈
//        return RestBean.failure("系统错误：" + ex.getMessage());
//    }

}
