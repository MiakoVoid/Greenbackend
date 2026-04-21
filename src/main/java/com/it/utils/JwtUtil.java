package com.it.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    // 用于存储已注销的令牌的黑名单
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    
    // 生成JWT令牌
    public String generateToken(String username, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("userId", userId);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
    
    
    // 验证JWT令牌 - 简化版本
    public Boolean validateToken(String token) {
        try {
            // 检查令牌是否在黑名单中
            if (blacklistedTokens.contains(token)) {
                return false;
            }
            
            Jwts.parser()
                    .setSigningKey(secret)
                    .setAllowedClockSkewSeconds(3600) // 允许1小时时钟偏差
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 验证JWT令牌并检查是否过期
    public Boolean validateTokenAndCheckExpiration(String token) {
        try {
            // 检查令牌是否在黑名单中
            if (blacklistedTokens.contains(token)) {
                return false;
            }
            
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .setAllowedClockSkewSeconds(3600) // 允许1小时时钟偏差
                    .parseClaimsJws(token)
                    .getBody();
            
            // 检查令牌是否过期
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    // 从JWT令牌中获取用户名
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }
    
    // 从JWT令牌中获取用户ID
    public Long getUserIdFromToken(String token) {
        try {
            Object userIdObj = getClaimsFromToken(token).get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // 检查JWT令牌是否过期
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    // 从JWT令牌中获取过期时间
    public Date getExpirationDateFromToken(String token) {
        return getClaimsFromToken(token).getExpiration();
    }
    
    // 从JWT令牌中获取Claims
    private Claims getClaimsFromToken(String token) {
        try {
            // 检查令牌是否在黑名单中
            if (blacklistedTokens.contains(token)) {
                throw new ExpiredJwtException(null, null, "Token is blacklisted");
            }
            
            return Jwts.parser()
                    .setSigningKey(secret)
                    .setAllowedClockSkewSeconds(3600) // 允许1小时时钟偏差
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 返回过期令牌的claims以便可以获取其中的信息
            return e.getClaims();
        }
    }
    
    // 获取剩余有效时间（毫秒）
    public Long getRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0L;
        }
    }
    
    // 将令牌加入黑名单（注销）
    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
    }
    
    // 检查令牌是否在黑名单中
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}