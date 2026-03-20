<!-- navigation bar -->
<template>
  <div class="no-caret blog-nav-shell">
    <el-header :style="'margin-bottom:' + headerBottom + 'px'">
      <div class="nav-left">
        <router-link class="brand-lockup" to="/cms/main/cmsIndex">
          <span class="brand-badge">
            <img
              class="brand-logo"
              src="@/assets/images/logo.png"
              alt="Zac Blog logo"
            />
          </span>
          <span class="brand-copy">
            <strong>Zac Blog</strong>
            <small>notes, builds, and essays</small>
          </span>
        </router-link>
      </div>

      <div class="nav-center hidden-sm-and-down">
        <el-menu
          :default-active="activeIndex"
          router
          class="nav-menu"
          mode="horizontal"
          background-color="transparent"
          text-color="#1d2433"
          active-text-color="#1d2433"
        >
          <el-menu-item index="/cms/main/cmsIndex">
            <svg-icon icon-class="home-line" class="menu-icon" />
            Home
          </el-menu-item>
          <el-menu-item
            v-for="item in menulist"
            :key="item.id"
            :index="item.path"
          >
            <svg-icon :icon-class="item.icon" class="menu-icon" />
            {{ item.authName }}
          </el-menu-item>
        </el-menu>
      </div>

      <div class="nav-right">
        <div v-if="searchInput" class="search_input hidden-xs-only">
          <el-input
            v-model="queryInfo.query"
            @focus="checkInput"
            @blur="notSearching()"
            class="search"
            placeholder="Search articles"
            prefix-icon="el-icon-search"
            size="large"
          />
          <ul v-if="searching" class="search-results">
            <li
              v-for="blog in searchList"
              :key="blog.id"
              class="search-blog"
              @click="getBlogInfo(blog.id)"
            >
              <a><span v-html="blog.highlightedTitle || blog.title"></span></a>
            </li>
          </ul>
        </div>

        <div v-if="islogin" class="account-area desktop-account">
          <el-dropdown class="avatar-container" trigger="click">
            <div class="avatar-wrapper">
              <el-avatar
                class="user-avatar"
                :src="avatar"
                @error="errorHandler"
              >
                <i class="el-icon-s-custom" />
              </el-avatar>
              <span class="avatar-name">{{ name }}</span>
            </div>
            <el-dropdown-menu slot="dropdown">
              <router-link target="_blank" to="/index">
                <el-dropdown-item>Open Admin</el-dropdown-item>
              </router-link>
              <el-dropdown-item divided @click.native="logout">
                <span>Logout</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div v-else class="account-area desktop-account">
          <el-button
            @click="tologin"
            type="primary"
            size="medium"
            round
            class="login-btn"
          >
            Login
          </el-button>
        </div>

        <button class="menu-expend" type="button" @click="menuExpend">
          <svg-icon icon-class="list" class="menu-expend-icon" />
        </button>
      </div>

      <transition name="mobile-nav">
        <div v-if="menuHiddenVisiable" class="mobile-panel">
          <div class="mobile-panel-inner">
            <el-menu
              :default-active="activeIndex"
              router
              class="mobile-menu"
              background-color="transparent"
              text-color="#1d2433"
              active-text-color="#1d2433"
            >
              <el-menu-item index="/cms/main/cmsIndex" @click="menuAway">
                <svg-icon icon-class="home-line" class="menu-icon" />
                Home
              </el-menu-item>
              <el-menu-item
                v-for="item in menulist"
                :key="item.id"
                :index="item.path"
                @click="menuAway"
              >
                <svg-icon :icon-class="item.icon" class="menu-icon" />
                {{ item.authName }}
              </el-menu-item>
            </el-menu>
            <div class="mobile-search">
              <el-input
                v-model="queryInfo.query"
                @focus="checkInput"
                @blur="notSearching()"
                placeholder="Search articles"
                prefix-icon="el-icon-search"
              />
            </div>
            <div class="mobile-panel-footer">
              <template v-if="islogin">
                <div class="mobile-user-chip">
                  <el-avatar
                    class="user-avatar"
                    :src="avatar"
                    @error="errorHandler"
                  >
                    <i class="el-icon-s-custom" />
                  </el-avatar>
                  <span>{{ name }}</span>
                </div>
                <div class="mobile-auth-actions">
                  <el-button class="mobile-ghost-btn" @click="$router.push('/index')">Admin</el-button>
                  <el-button class="mobile-primary-btn" @click="logout">Logout</el-button>
                </div>
              </template>
              <template v-else>
                <el-button class="mobile-primary-btn mobile-login-btn" @click="tologin">Login</el-button>
              </template>
            </div>
          </div>
        </div>
      </transition>
    </el-header>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import { getToken } from "@/utils/auth";
import "element-ui/lib/theme-chalk/display.css";
import { cmsListBlog } from "@/api/cms/blog";

export default {
  name: "cmsNavBar",
  data() {
    return {
      islogin: false,
      searchInput: true,
      menuHiddenVisiable: false,
      headerBottom: 0,
      queryInfo: {
        query: "",
        timer: null,
      },
      searchList: [],
      searching: false,
      menulist: [
        {
          id: 1,
          authName: "Notes",
          path: "/cms/main/essay",
          icon: "edit-line",
        },
        {
          id: 2,
          authName: "Comments",
          path: "/cms/main/message",
          icon: "chat-line",
        },
        {
          id: 3,
          authName: "About",
          path: "/cms/doucument",
          icon: "user-line",
        },
      ],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        title: null,
        type: 1,
        content: null,
        top: null,
        views: null,
        status: null,
      },
    };
  },
  mounted() {
    window.addEventListener("resize", this.handleResize);
    document.addEventListener("click", this.handleClickOutside);
    document.addEventListener("touchstart", this.handleClickOutside);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    document.removeEventListener("click", this.handleClickOutside);
    document.removeEventListener("touchstart", this.handleClickOutside);
  },
  computed: {
    ...mapGetters(["avatar", "name"]),
    activeIndex() {
      return this.$route.path;
    },
  },
  watch: {
    "queryInfo.query": {
      handler() {
        if (this.timer) {
          clearTimeout(this.timer);
        }
        this.timer = setTimeout(() => {
          this.searchBlog();
        }, 300);
      },
    },
  },
  created() {
    this.login();
  },
  methods: {
    handleClickOutside(e) {
      if (this.menuHiddenVisiable) {
        const menu = this.$el.querySelector(".mobile-panel");
        const toggleBtn = this.$el.querySelector(".menu-expend");

        if (
          menu &&
          !menu.contains(e.target) &&
          toggleBtn &&
          !toggleBtn.contains(e.target)
        ) {
          this.menuAway();
        }
      }
    },
    handleResize() {
      if (window.innerWidth > 1200) {
        this.menuHiddenVisiable = false;
        this.headerBottom = 0;
      }
    },
    menuExpend() {
      this.menuHiddenVisiable = !this.menuHiddenVisiable;
      this.headerBottom = this.menuHiddenVisiable && window.innerWidth > 768 ? 320 : 0;
    },
    menuAway() {
      this.menuHiddenVisiable = false;
      this.headerBottom = 0;
    },
    notSearching() {
      setTimeout(() => {
        this.searching = false;
      }, 100);
    },
    checkInput() {
      this.searching = this.queryInfo.query !== "";
    },
    tologin() {
      this.$router.push({
        path: "/cmsLogin",
      });
    },
    login() {
      const token = getToken();
      this.islogin = !(token == null || token === "");
    },
    errorHandler() {
      return true;
    },
    async searchBlog() {
      if (this.queryInfo.query === "") {
        this.searching = false;
        return;
      }
      this.queryParams.title = this.queryInfo.query;
      cmsListBlog(this.queryParams).then((response) => {
        const listSize = response.rows.length;
        if (listSize > 0) {
          for (let i = 0; i < listSize; i++) {
            const highlightedTitle = this.brightenKeyword(
              response.rows[i].title,
              this.queryInfo.query
            );
            response.rows[i].highlightedTitle = highlightedTitle;
          }
        }
        this.searchList = response.rows;
        if (this.searchList.length !== 0) {
          this.searching = true;
        }
      });
    },
    escapeHtml(value) {
      return String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
    },
    escapeRegExp(value) {
      return String(value).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    },
    brightenKeyword(val, keyword) {
      if (!val) return "";
      const safeTitle = this.escapeHtml(val);
      const safeKeyword = this.escapeRegExp(keyword);
      const reg = new RegExp(`(${safeKeyword})`, "ig");
      return safeTitle.replace(
        reg,
        '<span class="search-highlight">$1</span>'
      );
    },
    getBlogInfo(blogId) {
      const routeUrl = this.$router.resolve({
        path: "/cms/main/blog",
        query: {
          id: blogId,
        },
      });
      window.open(routeUrl.href, "_blank");
    },
    async logout() {
      this.$confirm(
        "Are you sure you want to log out and exit the system?",
        "Notice",
        {
          confirmButtonText: "Confirm",
          cancelButtonText: "Cancel",
          type: "warning",
        }
      )
        .then(() => {
          this.$store.dispatch("LogOut").then(() => {
            location.href = "/cms/main/cmsIndex";
          });
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.blog-nav-shell {
  position: sticky;
  top: 0;
  z-index: 1000;
}

.el-header {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  width: min(1280px, calc(100vw - 28px));
  min-height: 84px;
  z-index: 1000;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 18px;
  padding: 14px 18px;
  border: 1px solid rgba(29, 36, 51, 0.1);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  box-shadow: 0 18px 48px rgba(18, 27, 43, 0.08);
  transition: 0.25s ease;
}

.brand-lockup {
  display: inline-flex;
  align-items: center;
  gap: 14px;
}

.brand-badge {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: #ffffff;
  color: #1d2433;
  box-shadow: 0 10px 22px rgba(18, 27, 43, 0.1);
}

.brand-logo {
  width: 44px;
  height: 44px;
  display: block;
  object-fit: contain;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 1rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #1d2433;
}

.brand-copy small {
  color: rgba(29, 36, 51, 0.56);
  letter-spacing: 0.04em;
}

.nav-center {
  display: flex;
  justify-content: center;
}

.nav-menu {
  display: inline-flex;
  padding: 6px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.52);
}

.nav-menu /deep/ .el-menu-item {
  height: 42px;
  line-height: 42px;
  margin: 0 4px;
  padding: 0 16px;
  border-radius: 999px;
  border-bottom: 0 !important;
  font-size: 14px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  transition: 0.18s ease;
}

.nav-menu /deep/ .el-menu-item:hover,
.nav-menu /deep/ .el-menu-item.is-active {
  background: #dedede !important;
  color: #1d2433 !important;
  box-shadow: 0 10px 22px rgba(18, 27, 43, 0.08);
}

.menu-icon {
  margin-right: 8px;
}

.nav-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.desktop-account {
  display: flex;
}

.search_input {
  position: relative;
  width: 260px;
}

.search_input /deep/ .el-input__inner {
  height: 46px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
}

.search-results {
  position: absolute;
  top: 56px;
  left: 0;
  width: 100%;
  padding: 10px;
  margin: 0;
  list-style: none;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 22px;
  background: rgba(255, 252, 245, 0.96);
  box-shadow: 0 18px 42px rgba(18, 27, 43, 0.12);
}

.search-blog {
  padding: 10px 12px;
  border-radius: 14px;
  color: #1d2433;
  transition: 0.18s ease;
}

.search-blog /deep/ .search-highlight {
  color: #111827;
  font-weight: 700;
}

.search-blog:hover {
  background: rgba(17, 17, 17, 0.08);
  cursor: pointer;
}

.avatar-wrapper {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px 4px 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(29, 36, 51, 0.08);
}

.user-avatar {
  border: 2px solid rgba(17, 17, 17, 0.14);
}

.avatar-name {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #1d2433;
}

.login-btn {
  border: 1px solid rgba(29, 36, 51, 0.08);
  background: #dedede;
  color: #1d2433;
  box-shadow: 0 10px 22px rgba(18, 27, 43, 0.08);
}

.menu-expend {
  width: 44px;
  height: 44px;
  display: none;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.58);
  color: #1d2433;
}

.mobile-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 1001;
}

.mobile-panel-inner {
  padding: 12px;
  border-radius: 20px;
  border: 1px solid rgba(29, 36, 51, 0.09);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(20px);
  box-shadow: 0 20px 44px rgba(18, 27, 43, 0.12);
}

.mobile-search {
  margin-top: 10px;
}

.mobile-menu {
  border-right: 0;
}

.mobile-menu /deep/ .el-menu-item {
  height: 48px;
  line-height: 48px;
  margin-bottom: 4px;
  padding: 0 14px !important;
  border-radius: 12px;
  border-bottom: 0 !important;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  position: relative;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.mobile-menu /deep/ .el-menu-item.is-active,
.mobile-menu /deep/ .el-menu-item:hover {
  background: rgba(17, 17, 17, 0.045) !important;
  color: #111827 !important;
}

.mobile-menu /deep/ .el-menu-item.is-active {
  font-weight: 600;
}

.mobile-menu /deep/ .el-menu-item.is-active::before {
  content: "";
  position: absolute;
  left: 10px;
  top: 50%;
  width: 2px;
  height: 18px;
  border-radius: 999px;
  background: rgba(29, 36, 51, 0.7);
  transform: translateY(-50%);
}

.mobile-search /deep/ .el-input__inner {
  height: 44px;
  border: 1px solid rgba(29, 36, 51, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
}

.mobile-panel-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(29, 36, 51, 0.08);
}

.mobile-user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: rgba(17, 17, 17, 0.045);
  color: #1d2433;
  font-size: 13px;
  font-weight: 600;
}

.mobile-auth-actions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.mobile-ghost-btn,
.mobile-primary-btn {
  min-height: 42px;
  border-radius: 999px;
  font-weight: 600;
}

.mobile-ghost-btn {
  border: 1px solid rgba(29, 36, 51, 0.08);
  background: rgba(255, 255, 255, 0.72);
  color: #1d2433;
}

.mobile-primary-btn {
  border: 1px solid rgba(29, 36, 51, 0.08);
  background: #dedede;
  color: #1d2433;
}

.mobile-login-btn {
  width: 100%;
}

.mobile-nav-enter-active,
.mobile-nav-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.mobile-nav-enter,
.mobile-nav-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media screen and (max-width: 1200px) {
  .el-header {
    width: calc(100vw - 20px);
    grid-template-columns: auto 1fr auto;
  }

  .nav-center {
    display: none;
  }

  .menu-expend {
    display: inline-flex;
  }
}

@media screen and (max-width: 768px) {
  .el-header {
    top: 10px;
    min-height: 74px;
    padding: 12px;
    border-radius: 22px;
    width: calc(100vw - 18px);
  }

  .brand-copy small,
  .avatar-name,
  .desktop-account {
    display: none;
  }

  .brand-copy strong {
    font-size: 0.9rem;
    letter-spacing: 0.14em;
  }

  .nav-right {
    gap: 8px;
  }

  .menu-expend {
    width: 42px;
    height: 42px;
    border-radius: 12px;
  }

  .brand-lockup {
    gap: 10px;
  }

  .brand-badge {
    width: 44px;
    height: 44px;
    border-radius: 14px;
  }

  .brand-logo {
    width: 40px;
    height: 40px;
  }
}
</style>
