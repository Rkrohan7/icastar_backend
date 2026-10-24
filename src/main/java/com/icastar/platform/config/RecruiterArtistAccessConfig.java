package com.icastar.platform.config;

import com.icastar.platform.service.RecruiterArtistAccessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class RecruiterArtistAccessConfig implements WebMvcConfigurer {

    private final RecruiterArtistAccessInterceptor recruiterArtistAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(recruiterArtistAccessInterceptor);
    }
}
