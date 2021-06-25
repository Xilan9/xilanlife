package com.tanhua.sso.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@PropertySource("classpath:huyi.properties")
@Configuration
@Data
public class LoginConfig  {
    @Value("${sso.account}")
    private  String account;
    @Value("${sso.password}")
    private  String password;
}
