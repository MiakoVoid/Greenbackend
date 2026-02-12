package com.it.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.it.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("#{'${jwt.ignore-paths}'.split(',')}")
    public List<String> ignorePaths;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 跳过公开路径的JWT验证
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String token = getTokenFromRequest(request);
        
        if (token != null) {
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    if (jwtUtil.validateToken(token)) {
                        // 设置Spring Security上下文
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        Collections.emptyList()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 在响应头中添加令牌状态信息
                        Long remainingTime = jwtUtil.getRemainingTime(token);
                        response.setHeader("X-Token-Expiry", String.valueOf(remainingTime));
                        
                        // 如果令牌即将过期（少于1天），通知客户端需要刷新
                        if (remainingTime < 86400000) { // 1天 = 86400000毫秒
                            response.setHeader("X-Token-Expiring", "true");
                        }
                    }
                }
            } catch (ExpiredJwtException e) {
                // 移动端友好的错误响应
                sendErrorResponse(response,
                        "TOKEN_EXPIRED", "令牌已过期，请重新登录");
                return;
            } catch (Exception e) {
                // 移动端友好的错误响应
                sendErrorResponse(response,
                        "INVALID_TOKEN", "无效的令牌，请重新登录");
                return;
            }
        } else {
            // 对于需要认证的接口但没有token的情况
            if (!isPublicPath(path)) {
                sendErrorResponse(response,
                        "MISSING_TOKEN", "缺少JWT令牌");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicPath(String path) {
        // 检查是否为公开路径
        for (String ignorePath : ignorePaths) {
            // 去除可能存在的空格
            String trimmedPath = ignorePath.trim();
            
            // 处理通配符路径
            if (trimmedPath.endsWith("/**")) {
                String prefix = trimmedPath.substring(0, trimmedPath.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.endsWith(trimmedPath)) {
                // 使用endsWith而不是equals，以处理context-path的情况
                return true;
            }
        }
        return false;
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        // 支持多种方式获取token
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 移动端可能使用其他header
        String tokenHeader = request.getHeader("X-Auth-Token");
        if (tokenHeader != null) {
            return tokenHeader;
        }
        
        // 或者使用query parameter（不推荐，仅作兼容）
        return request.getParameter("token");
    }
    
    private void sendErrorResponse(HttpServletResponse response, String errorCode, String message)
            throws IOException {
        logger.info("JWT validation failed: " + message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", HttpServletResponse.SC_UNAUTHORIZED);
        responseBody.put("error", errorCode);
        responseBody.put("message", message);
        responseBody.put("timestamp", System.currentTimeMillis());
        responseBody.put("data", null);
        
        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}