<!-- login page -->
<template>
  <div class="login">
    <div class="login-overlay"></div>
    <el-button 
      icon="el-icon-back" 
      circle 
      class="back-to-home-btn"
      @click="backToHome"
      title="Back to Home"
    ></el-button>
    <div class="login-shell">
      <div class="login-intro">
        <div class="eyebrow">Zac Blog</div>
        <h1>Welcome back</h1>
        <p>
          Sign in to manage posts, notes, comments, and the rest of the publishing
          workflow.
        </p>
        <div class="intro-points">
          <div class="intro-point">
            <span class="point-label">Writing</span>
            <strong>Posts, essays, updates</strong>
          </div>
          <div class="intro-point">
            <span class="point-label">Moderation</span>
            <strong>Comments and messages</strong>
          </div>
          <div class="intro-point">
            <span class="point-label">Control</span>
            <strong>Admin access for the full site</strong>
          </div>
        </div>
      </div>

      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="form-head">
          <div class="eyebrow">Admin Access</div>
          <h3 class="title">Login</h3>
          <p class="subtitle">Use your account to continue into the dashboard.</p>
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="Username"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="Password"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>

        <el-form-item prop="code" v-if="captchaOnOff" class="captcha-item">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="Captcha"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img"/>
            </div>
          </div>
        </el-form-item>

        <div class="form-meta">
          <el-checkbox v-model="loginForm.rememberMe">Remember Me</el-checkbox>
          <router-link v-if="register" class="link-type" :to="'/cmsRegister'">Register Now</router-link>
        </div>

        <el-form-item class="submit-item">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            class="login-submit"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">Login</span>
            <span v-else>Logging in...</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2023-2025 Zac All Rights Reserved.</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'

export default {
  name: "cmsLogin",
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "Please enter your username" }
        ],
        password: [
          { required: true, trigger: "blur", message: "Please enter your password" }
        ],
        code: [{ required: true, trigger: "change", message: "Please enter the captcha" }]
      },
      loading: false,
      // 验证码开关
      captchaOnOff: true,
      // 注册开关
      register: true,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    backToHome() {
      this.$router.push({ path: "/cms/main/cmsIndex" });
    },
    getCode() {
      getCodeImg().then(res => {
        this.captchaOnOff = res.captchaOnOff === undefined ? true : res.captchaOnOff;
        if (this.captchaOnOff) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: "/cms/main/cmsIndex" });
          }).catch(() => {
            this.loading = false;
            if (this.captchaOnOff) {
              this.getCode();
            }
          });
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-image: url("../../assets/images/login.jpg");
  background-size: cover;
  background-position: center;
  overflow: hidden;
  padding: 32px 20px 80px;
}

.login-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.18), transparent 32%),
    linear-gradient(180deg, rgba(10, 13, 20, 0.28), rgba(10, 13, 20, 0.48));
}

.login-shell {
  position: relative;
  z-index: 2;
  width: min(980px, calc(100vw - 40px));
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 420px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.16);
  backdrop-filter: blur(18px);
  box-shadow: 0 28px 90px rgba(8, 12, 20, 0.28);
  overflow: hidden;
}

.login-intro {
  padding: 48px 42px;
  color: #ffffff;
  background: linear-gradient(160deg, rgba(16, 20, 28, 0.38), rgba(16, 20, 28, 0.12));
}

.eyebrow {
  display: inline-block;
  margin-bottom: 16px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.login-intro h1 {
  margin: 0 0 14px;
  font-size: clamp(2rem, 4vw, 3.25rem);
  line-height: 1.04;
  font-weight: 600;
}

.login-intro p {
  max-width: 420px;
  margin: 0;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.82);
}

.intro-points {
  margin-top: 38px;
  display: grid;
  gap: 14px;
}

.intro-point {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
}

.point-label {
  display: block;
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.62);
}

.intro-point strong {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
}

.title {
  margin: 0;
  color: #1d2433;
  font-size: 2rem;
  font-weight: 600;
}

.subtitle {
  margin: 10px 0 0;
  line-height: 1.7;
  color: rgba(29, 36, 51, 0.62);
}

.login-form {
  padding: 40px 34px 30px;
  background: rgba(255, 255, 255, 0.92);
}

.form-head {
  margin-bottom: 28px;
}

.login-form .el-form-item {
  margin-bottom: 16px;
}

.login-form .el-input {
  height: 50px;
}

.login-form .el-input ::v-deep .el-input__inner {
  height: 50px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 14px;
  background: rgba(248, 249, 251, 0.96);
  box-shadow: none;
}

.login-form .el-input ::v-deep .el-input__inner:focus {
  border-color: rgba(29, 36, 51, 0.18);
}

.input-icon {
  height: 50px;
  width: 16px;
  margin-left: 4px;
  color: rgba(29, 36, 51, 0.52);
}

.captcha-item {
  margin-bottom: 12px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 96px;
  gap: 10px;
}

.login-code {
  height: 50px;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.86);
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 0.08em;
  z-index: 2;
}

.login-code-img {
  width: 100%;
  height: 50px;
  display: block;
  border-radius: 14px;
  border: 1px solid rgba(29, 36, 51, 0.08);
}

.form-meta {
  margin: 8px 0 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: rgba(29, 36, 51, 0.68);
}

.link-type {
  color: #4b78b9;
  font-weight: 500;
}

.submit-item {
  margin-bottom: 0 !important;
}

.login-submit {
  width: 100%;
  height: 52px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 14px;
  background: #1d2433;
  color: #ffffff;
  box-shadow: 0 14px 28px rgba(29, 36, 51, 0.18);
}

.back-to-home-btn {
  position: fixed;
  top: 20px;
  left: 20px;
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.9);
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  z-index: 3;
}

.back-to-home-btn:hover {
  background: rgba(255, 255, 255, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.back-to-home-btn:active {
  transform: translateY(0);
}

@media screen and (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-intro {
    padding: 34px 28px 18px;
  }

  .intro-points {
    grid-template-columns: 1fr;
    margin-top: 26px;
  }
}

@media screen and (max-width: 600px) {
  .login {
    padding: 20px 14px 72px;
    align-items: flex-start;
  }

  .login-shell {
    width: calc(100vw - 28px);
    margin-top: 72px;
    border-radius: 24px;
  }

  .login-intro {
    padding: 28px 22px 16px;
  }

  .login-form {
    padding: 26px 20px 22px;
  }

  .title {
    font-size: 1.7rem;
  }

  .captcha-row {
    grid-template-columns: 1fr;
  }

  .form-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .el-login-footer {
    font-size: 11px;
    letter-spacing: 0.04em;
  }
}
</style>
