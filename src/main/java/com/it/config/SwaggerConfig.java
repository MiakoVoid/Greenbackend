package com.it.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.*;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Swagger配置类 - OpenAPI 3.0
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public Docket customDocket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .securitySchemes(Collections.singletonList(apiKey()))
                .securityContexts(Collections.singletonList(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.it.greenfinance.controller"))
                .paths(PathSelectors.any())
                .build();
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("绿芽记账(GreenFinance) 接口文档")
                .description(getDetailedDescription())
                .version("v1.0.0")
                .contact(new Contact("GreenFinance Team", "http://greenfinance.it.com", "support@greenfinance.com"))
                .build();
    }

    private ApiKey apiKey() {
        return new ApiKey("Authorization", "Authorization", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .operationSelector(o -> true)
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Collections.singletonList(new SecurityReference("Authorization", authorizationScopes));
    }

    private String getDetailedDescription() {
        return "## 1. 接口基础信息\n" +
               "- **Base URL**: `http://localhost:8080`\n" +
               "- **协议**: HTTP/1.1\n" +
               "- **数据格式**: JSON (Content-Type: application/json)\n" +
               "- **字符编码**: UTF-8\n" +
               "\n" +
               "## 2. 请求参数规范\n" +
               "- **路径参数**: 如 `/users/{id}`，需在URL中替换\n" +
               "- **查询参数**: 如 `?page=1&size=20`，用于筛选和分页\n" +
               "- **请求体**: POST/PUT 请求使用 JSON 格式\n" +
               "- **分页参数标准化**: `page` (默认1), `size` (默认20)\n" +
               "\n" +
               "## 3. 请求头要求\n" +
               "- **Content-Type**: `application/json` (必需)\n" +
               "- **Authorization**: `Bearer <token>` (登录后必需)\n" +
               "\n" +
               "## 4. 响应规范\n" +
               "所有接口统一返回 `Result` 对象：\n" +
               "```json\n" +
               "{\n" +
               "  \"success\": true,\n" +
               "  \"code\": 200,\n" +
               "  \"message\": \"操作成功\",\n" +
               "  \"data\": { ... }\n" +
               "}\n" +
               "```\n" +
               "- **200**: 成功\n" +
               "- **400**: 客户端参数错误\n" +
               "- **401**: 未登录或Token失效\n" +
               "- **403**: 无权限\n" +
               "- **404**: 资源不存在\n" +
               "- **500**: 服务器内部错误\n" +
               "\n" +
               "## 5. 前端调用示例 (Axios)\n" +
               "```javascript\n" +
               "// 建议配置拦截器自动添加Token\n" +
               "axios.interceptors.request.use(config => {\n" +
               "  const token = localStorage.getItem('token');\n" +
               "  if (token) {\n" +
               "    config.headers.Authorization = 'Bearer ' + token;\n" +
               "  }\n" +
               "  return config;\n" +
               "});\n" +
               "```\n" +
               "\n" +
               "## 6. 特殊处理规则\n" +
               "- **本地图片处理**:\n" +
               "  - 头像上传限制: JPG/PNG, Max 2MB\n" +
               "  - 路径: 返回相对路径 `files/greenfinance/avatars/...`，需拼接BaseURL\n" +
               "- **时间格式**: 统一使用 `yyyy-MM-dd HH:mm:ss` 或 ISO8601\n" +
               "\n" +
               "## 7. 权限控制\n" +
               "- **普通用户**: 仅能访问自己的数据\n" +
               "- **速率限制**: 单IP限制 60请求/分钟 (X-RateLimit-Limit)\n" +
               "\n" +
               "## 8. 文档维护\n" +
               "- **最后更新**: 2025-12-22\n" +
               "- **负责人**: AI Assistant\n";
    }
    
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(WebEndpointsSupplier webEndpointsSupplier,
                                                                         ServletEndpointsSupplier servletEndpointsSupplier, ControllerEndpointsSupplier controllerEndpointsSupplier,
                                                                         EndpointMediaTypes endpointMediaTypes, CorsEndpointProperties corsProperties,
                                                                         WebEndpointProperties webEndpointProperties, Environment environment) {
        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping =
                webEndpointProperties.getDiscovery().isEnabled() && (StringUtils.hasText(basePath)
                        || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
        return new WebMvcEndpointHandlerMapping(endpointMapping, webEndpoints, endpointMediaTypes,
                corsProperties.toCorsConfiguration(), new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping, null);
    }
}
