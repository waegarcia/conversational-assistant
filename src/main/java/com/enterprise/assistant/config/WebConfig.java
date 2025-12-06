package com.enterprise.assistant.config;

import com.enterprise.assistant.infrastructure.security.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/conversations/**", "/api/metrics/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/**",
                        "/h2-console/**"
                );
    }
}
