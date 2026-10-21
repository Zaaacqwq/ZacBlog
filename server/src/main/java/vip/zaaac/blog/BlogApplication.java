package vip.zaaac.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// 不用默认的内存用户（会打印随机密码）；认证由 T3 的 JWT 实现
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
