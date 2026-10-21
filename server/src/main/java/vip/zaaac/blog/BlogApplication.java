package vip.zaaac.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// 不用默认的内存用户（会打印随机密码）；/admin/** 用 JWT 认证，见 SecurityConfig
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
