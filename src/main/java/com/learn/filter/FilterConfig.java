package com.learn.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<InitializeCurrentUser> googleAuthFilter() {
        FilterRegistrationBean<InitializeCurrentUser> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new InitializeCurrentUser());
        registrationBean.addUrlPatterns("/*"); // Apply filter to all URLs or specify the URL patterns you need
        return registrationBean;
    }
}
