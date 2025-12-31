package org.example.examback.JWT;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import org.example.examback.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

public class JWTUtil {
    private static final Logger logger = LoggerFactory.getLogger(JWTUtil.class);
    private static final String SECRET = "lmdzhly"; // 加密密钥
    private static final long EXPIRATION = 86400000; // 1天有效期

    // 创建Token
    public static String createToken(User user) {
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRATION * 7);
        return JWT.create()
                .withHeader(Map.of("alg", "HS256", "typ", "JWT"))
                .withClaim("id", user.getId())
                .withClaim("account", user.getAccount())
                .withClaim("username", user.getUsername())
                .withClaim("password", "刘梦达")
                .withExpiresAt(expireDate)
                .withIssuedAt(new Date())
                .sign(Algorithm.HMAC256(SECRET));
    }

    // 验证Token
    public static Map<String, Claim> verifyToken(String token) {
        try {
            return JWT.require(Algorithm.HMAC256(SECRET))
                    .build()
                    .verify(token)
                    .getClaims();
        } catch (Exception e) {
            logger.error("Token验证失败", e);
            return null;
        }
    }
}