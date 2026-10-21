package vip.zaaac.blog.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vip.zaaac.blog.common.ApiResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(
            @NotBlank @Size(max = 30) String username,
            @NotBlank @Size(max = 100) String password,
            @NotBlank String captchaId,
            @NotBlank String captchaCode) {
    }

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaService.Captcha> captcha() {
        return ApiResponse.ok(captchaService.create());
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.LoginResult> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return ApiResponse.ok(authService.login(req.username(), req.password(), req.captchaId(), req.captchaCode(),
                request.getRemoteAddr()));
    }
}
