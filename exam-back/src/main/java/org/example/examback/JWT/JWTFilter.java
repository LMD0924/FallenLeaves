package org.example.examback.JWT;

import com.alibaba.fastjson.JSON;
import com.auth0.jwt.interfaces.Claim;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.examback.entity.RestBean;

import java.io.IOException;
import java.util.Map;

@Slf4j
@WebFilter(filterName = "JWTFilter", urlPatterns = "/api/*")
public class JWTFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String requestURI = request.getRequestURI();

        // 放行认证相关路径
        if (requestURI.startsWith("/api/auth")) {
            chain.doFilter(request, response);
            return;
        }

        // 处理OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(request, response);
            return;
        }

        // 验证Token
        String token = request.getHeader("authorization");
        if (token == null) {
            response.getWriter().write(JSON.toJSONString(RestBean.failure(401,"未提供token")));
            return;
        }

        Map<String, Claim> userData = JWTUtil.verifyToken(token);
        if (userData == null) {
            response.getWriter().write(JSON.toJSONString(RestBean.failure(401,"token不合法")));
            return;
        }

        // 设置用户属性到请求中
        request.setAttribute("id", userData.get("id").asInt());
        request.setAttribute("account", userData.get("account").asString());
        request.setAttribute("username", userData.get("username").asString());
        request.setAttribute("password", userData.get("password").asString());

        chain.doFilter(req, res);
    }
}