<template>
  <div class="no-caret">
    <el-header :style="'margin-bottom:' + headerBottom + 'px; height: 70px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);'">
      <!-- Left: logo and optional menu button -->
      <div class="nav-left">
        <h2 class="logo"><svg-icon icon-class="bird" /> ZacBlog!</h2>
        <div class="menu-expend" @click="menuExpend">
          <svg-icon icon-class="list" class="menu-expend-icon" />
        </div>
      </div>

      <!-- Center: main menu (centered) -->
      <div class="nav-center">
        <el-menu :default-active="activeIndex" router :class="['el-menu-demo', 'centered-menu', menuStyleClass]" mode="horizontal"
          style="border: none;" background-color="rgba(0,0,0,0)" text-color="#000" active-text-color="#000000">
          <el-menu-item index="/cms/main/cmsIndex">
            <svg-icon icon-class="home-line" class="menu-icon" />
            Home
          </el-menu-item>
          <el-menu-item :index="item.path" v-for="item in menulist" :key="item.id">
            <svg-icon :icon-class="item.icon" class="menu-icon" />
            {{ item.authName }}
          </el-menu-item>
        </el-menu>

        <div class="el-menu-hidden" v-if="menuHiddenVisiable">
          <el-menu :default-active="activeIndex" router background-color="rgba(84,92,100,0.5)" text-color="#000"
            active-text-color="#000000">
            <el-menu-item index="/cms/main/cmsIndex" @click="menuAway">
              <svg-icon icon-class="home-line" class="menu-icon" />
              Home
            </el-menu-item>
            <el-menu-item :index="item.path" v-for="item in menulist" :key="item.id" @click="menuAway">
              <svg-icon :icon-class="item.icon" class="menu-icon" />
              {{ item.authName }}
            </el-menu-item>
          </el-menu>
        </div>
      </div>

      <!-- Right: search and auth -->
      <div class="nav-right">
        <div v-if="searchInput" class="search_input">
          <el-input @focus="checkInput" @blur="notSearching()" class="search" placeholder="Search Blogs"
            prefix-icon="el-icon-search" v-model="queryInfo.query" size="large">
          </el-input>
          <ul v-if="searching">
            <li class="animate__animated animate__fadeInDown search-blog" v-for="blog in searchList" :key="blog.id"
              @click="getBlogInfo(blog.id)">
              <a><span v-html="blog.title"></span></a>
            </li>
          </ul>
        </div>

        <div v-if="islogin" class="bg-purple">
          <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
            <div class="avatar-wrapper">
              <el-avatar class="user-avatar" :src="avatar" @error="errorHandler">
                <i class="el-icon-s-custom" />
              </el-avatar>
              <p class="avatar-Name" style="color: black;">{{ name }}</p>
            </div>
            <el-dropdown-menu slot="dropdown">
              <router-link target="_blank" to="/index">
                <el-dropdown-item>Manage Blogs</el-dropdown-item>
              </router-link>
              <el-dropdown-item divided @click.native="logout">
                <span>Logout</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div v-else class="bg-purple">
          <el-button @click="tologin" type="primary" size="medium" round class="login-btn">
            Login
          </el-button>
        </div>
      </div>
    </el-header>
  </div>
</template>

<script>
import {
  mapGetters
} from 'vuex'
import {
  getToken
} from '@/utils/auth'
import 'element-ui/lib/theme-chalk/display.css'
import {
  cmsListBlog,
} from "@/api/cms/blog";
export default {
  name: 'cmsNavBar',
  data() {
    return {
      // menu style: 'underline' (default) or 'pill'
      menuStyle: 'underline',
      // default-active is driven by route path (see computed)
      islogin: false,
      searchInput: true,
      menuHiddenVisiable: false,
      headerBottom: 0,
      queryInfo: {
        query: '',
        timer: null
      },
      searchList: [],
      searching: false,
      menulist: [
        {
          id: 1,
          authName: 'Notes',
          path: '/cms/main/essay',
          icon: 'edit-line',
        },
        {
          id: 2,
          authName: 'Comments',
          path: '/cms/main/message',
          icon: 'chat-line',
        },
        {
          id: 3,
          authName: 'About me',
          path: '/cms/doucument',
          icon: 'user-line',
        },
      ],
      // 查询参数
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
    document.addEventListener("touchstart", this.handleClickOutside); // 手机触屏也要
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    document.removeEventListener("click", this.handleClickOutside);
    document.removeEventListener("touchstart", this.handleClickOutside);
  },
  computed: {
    ...mapGetters([
      'avatar',
      'name'
    ]),
    // keep el-menu active in sync with vue-router
    activeIndex() {
      return this.$route.path
    },
    menuStyleClass() {
      return this.menuStyle === 'pill' ? 'menu-style--pill' : 'menu-style--underline'
    }
  },
  watch: {
    'queryInfo.query': {
      handler(value) {
        if (this.timer) {
          clearTimeout(this.timer)
        }
        this.timer = setTimeout(() => {
          this.searchBlog()
        }, 300)
      }
    }
  },
  created() {
    this.login();
    // this.ResponsiveLayout();
  },
  methods: {
    handleClickOutside(e) {
      // 如果菜单显示，并且点击不在菜单区域和按钮区域，就关闭
      if (this.menuHiddenVisiable) {
        const menu = this.$el.querySelector(".el-menu-hidden");
        const toggleBtn = this.$el.querySelector(".menu-expend");

        if (menu && !menu.contains(e.target) && toggleBtn && !toggleBtn.contains(e.target)) {
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
    menulistAdd() {
      //push()方法一般是添加到数组的最后的位置；unshift()方法是往最前面的位置添加。
      // this.menulist.push({id:"",authName:""})
      this.menulist.unshift({
        id: "",
        authName: ""
      })
    },
    //响应式布局
    ResponsiveLayout() {
      //浏览器窗口的内部高度
      var w = window.innerWidth ||
        document.body.clientWidth;
      //浏览器窗口的内部宽度
      var h = window.innerHeight ||
        document.body.clientHeight;
      console.log(w, h);
    },
    // 展开菜单栏
    menuExpend() {
      this.menuHiddenVisiable = !this.menuHiddenVisiable
      if (this.menuHiddenVisiable === true) {
        this.headerBottom = (this.menulist.length + 1) * 56
      } else {
        this.headerBottom = 0
      }
    },
    //收起菜单
    menuAway() {
      this.menuHiddenVisiable = false;
      this.headerBottom = 0;
    },
    notSearching() {
      setTimeout(() => {
        this.searching = false
      }, 100)
    },
    checkInput() {
      this.searching = this.queryInfo.query !== '';
    },
    tologin() {
      this.$router.push({
        path: "/cmsLogin"
      });
    },
    login() {
      let token = getToken();
      if (token == null || token == '') {
        this.islogin = false;
      } else {
        this.islogin = true;
      }
    },
    errorHandler() {
      return true
    },
    async searchBlog() {
      if (this.queryInfo.query === '') {
        this.searching = false
        return
      }
      this.queryParams.title = this.queryInfo.query;
      cmsListBlog(this.queryParams).then(response => {
        let lsitSize = response.rows.length;
        if (lsitSize > 0) {
          for (let i = 0; i < lsitSize; i++) {
            let redTitle = this.brightenKeyword(response.rows[i].title, this.queryInfo.query);
            response.rows[i].title = redTitle;
          }
        };
        this.searchList = response.rows;
        if (this.searchList.length !== 0) {
          this.searching = true
        }
      });
    },
    //搜索关建字标红
    brightenKeyword(val, keyword) {
      const Reg = new RegExp(keyword, 'i');
      let res = '';
      if (val) {
        res = val.replace(Reg, `<span style="color: red;">${keyword}</span>`);
        return res;
      }
    },
    // 跳转到博客详情页
    getBlogInfo(blogId) {
      let routeUrl = this.$router.resolve({
        path: '/cms/main/blog',
        query: {
          id: blogId
        }
      });
      window.open(routeUrl.href, '_blank');
    },
    async logout() {
      this.$confirm('Are you sure you want to log out and exit the system?', 'Notice', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/cms/main/cmsIndex';
        })
      }).catch(() => { });
    }
  }
}
</script>

<style scoped>
.el-header {
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 1000;
  display: flex;
  justify-content: space-between;
  background-color: rgba(255, 255, 255, 0.8);
  align-items: center;
  padding: 0 20px;
  transition: .2s;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.2);
}

.el-header:hover {
  opacity: 1 !important;
}

.el-menu {
  flex-shrink: 0;
  background-color: rgba(0, 0, 0, 0) !important;
}

/* base menu item styles */
.el-menu /deep/ .el-menu-item {
  background-color: transparent;
  color: #111 !important; /* 默认黑色文字 */
  font-size: 20px;
  padding: 8px 14px;
  margin: 0 8px;
  border-radius: 12px; /* for pill style */
  border-bottom: none;
  transition: background-color 180ms ease, color 180ms ease, box-shadow 180ms ease;
  display: flex;
  align-items: center;
  position: relative;
}

.menu-icon { font-size: 20px; margin-right: 6px; line-height: 1; }
.el-menu /deep/ .el-menu-item i { color: currentColor; font-size: 20px; margin-right: 6px; transition: color 160ms ease, transform 160ms ease; }

/* Style A: Pill hover/active */
.menu-style--pill /deep/ .el-menu-item:hover,
.menu-style--pill /deep/ .el-menu-item:focus {
  background: #111;
  color: #fff !important;
  box-shadow: 0 6px 18px rgba(0,0,0,0.08);
}

.menu-style--pill /deep/ .el-menu-item:hover i,
.menu-style--pill /deep/ .el-menu-item:focus i {
  color: #fff;
  transform: translateY(-1px) scale(1.02);
}

.menu-style--pill /deep/ .el-menu-item.is-active {
  background: #111 !important;
  color: #fff !important;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
  font-weight: 600;
  transform: translateY(-1px);
}

.menu-style--pill /deep/ .el-menu-item.is-active i {
  color: #fff;
  transform: translateY(-1px) scale(1.02);
}

/* Style B: Underline slide */
.menu-style--underline /deep/ .el-menu-item {
  background: transparent !important;
}
.menu-style--underline /deep/ .el-menu-item::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 6px;
  height: 2px;
  background: currentColor;
  transform: scaleX(0);
  transform-origin: 50% 50%;
  transition: transform 200ms ease;
  opacity: .9;
}
.menu-style--underline /deep/ .el-menu-item:hover,
.menu-style--underline /deep/ .el-menu-item:focus {
  color: #000 !important;
}
.menu-style--underline /deep/ .el-menu-item:hover::after,
.menu-style--underline /deep/ .el-menu-item:focus::after {
  transform: scaleX(1);
}
.menu-style--underline /deep/ .el-menu-item.is-active {
  font-weight: 600;
}
.menu-style--underline /deep/ .el-menu-item.is-active::after {
  transform: scaleX(1);
}

/* keyboard focus */
.el-menu /deep/ .el-menu-item:focus { outline: none; }

.el-menu-hidden /deep/ .el-menu-item{
  height: 40px;
  line-height: 40px;
  margin: 4px 0;
  padding: 0 12px !important;
  border-radius: 12px;
  font-size: 15px;
  color: #111 !important;
  background: rgba(255,255,255,.55);
  transition: background .2s ease, transform .2s ease, box-shadow .2s ease;
  position: relative;
  display: flex;
  align-items: center;
}

.el-menu-hidden /deep/ .el-menu-item i{
  font-size: 18px; 
  color: #111 !important;
  margin-right: 8px;
  transition: transform .2s ease;
}

.el-menu-hidden /deep/ .el-menu-item:hover{
  background: rgba(0,0,0,.08) !important;
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
}

.el-menu-hidden /deep/ .el-menu-item:hover i {
  transform: scale(1.1);
}

.el-menu-hidden /deep/ .el-menu-item:active{
  transform: translateX(2px);
  background: rgba(0,0,0,.12) !important;
}

.el-menu-hidden /deep/ .el-menu-item.is-active {
  background: rgba(0,0,0,.06) !important;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0,0,0,.06);
}

.el-menu-hidden /deep/ .el-menu-item.is-active::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #111;
  border-radius: 0 2px 2px 0;
}

.no-caret ::v-deep .popper__arrow,
.no-caret ::v-deep .el-popper__arrow {
  display: none !important;
  border: 0 !important;
}

/* 适配更小屏（让卡片更窄、更贴边） */
@media (max-width: 380px){
  .el-menu-hidden{ max-width: 260px; padding: 6px; }
  .el-menu-hidden /deep/ .el-menu-item{ height: 38px; font-size: 14px; }
}

.search_input {
  display: inline-flex;
  position: relative;
  box-sizing: border-box;
  margin-right: 20px;
  width: auto;           /* allow it to size to content */
  min-width: 120px;     /* small minimum so placeholder fits */
  max-width: 260px;     /* reasonable cap for larger queries */
}

.search_input ul {
  position: absolute;
  top: 30px;
  width: 100%;
  border: 1px solid #e5e9ef;
  margin-top: 1px;
  background: #fff;
  z-index: 10000;
  border-radius: 2px;
  box-shadow: 0 2px 4px rgba(58, 118, 142, 0.16);
  padding: 10px 0;
  font-size: 14px;
  box-sizing: border-box;
}

.search_input .el-input {
  width: auto !important;       /* don't force full width */
  display: inline-block;
  min-width: 120px;
}

.search_input .el-input .el-input__inner {
  width: 100%;
  box-sizing: border-box;
}

.search_input ul li {
  height: 32px;
  line-height: 32px;
  cursor: pointer;
  word-wrap: break-word;
  word-break: break-all;
  display: block;
  color: #222;
  position: relative;
  transition: .2 ease;
  padding: 0 20px;
  font-size: 18px;
}

.search_input ul li:hover {
  background-color: #e8f3ff;
}


.search-blog {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  padding-left: 20px;
  padding-right: 20px;
}

.centered-menu {
  flex-grow: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto;
  width: fit-content;
}

.bg-purple-light {
  display: flex;
  align-items: center;
}

.bg-purple {
  display: flex;
  align-items: center;
}

.user-avatar {
  float: left;
  cursor: pointer;
  border: dashed rgba(0, 0, 0, 0.5);
}

.avatar-container {
  margin-right: 30px;
}

.avatar-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
}

.avatar-Name {
  margin-left: 10px;
  cursor: pointer;
  float: right;
  font-size: 18px;
  color: #ffffff;
}

.logo {
  display: flex;
  align-items: center;
  color: rgb(0, 0, 0);
  font-weight: bold;
  font-size: 26px;
  margin: 0;
  transition: margin-left 0.5s ease;
}

.logo svg {
  margin-right: 10px;
}

.logo:hover {
  cursor: pointer;
}

@media screen and (max-width: 1400px) {
  .logo {
    margin-left: 10px;
  }
}

.el-menu-hidden{
  position: fixed;
  top: 82px;                 /* 从70px改为82px，增加12px间距 */
  left: 12px;
  width: auto;               /* 关键：不要 100%/78vw */
  max-width: 300px;          /* 260~300 自行调 */
  padding: 8px;
  border: 0 !important;
  background: rgba(255,255,255,.68);
  backdrop-filter: saturate(160%) blur(10px);
  -webkit-backdrop-filter: saturate(160%) blur(10px);
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(0,0,0,.10);
  z-index: 1001;
  overflow: hidden;
}

.el-menu-hidden /deep/ .el-menu,
.el-menu-hidden /deep/ .el-menu-item,
.el-menu-hidden /deep/ .el-submenu__title {
  width: auto;               /* 关键：覆盖默认宽度 */
  background: transparent !important;
  border: 0 !important;
  padding: 0;
}

.menu-expend {
  display: none !important;
  position: absolute;
  top: 15px;                 /* 从20px调整为15px，更好地垂直居中 */
  left: 20px;
  font-size: 22px;           /* 从24px调整为22px */
  width: 40px;
  height: 40px;
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s ease, opacity 0.3s ease, background-color 0.3s ease;
}

.menu-expend:hover {
  background-color: rgba(0, 0, 0, 0.3);
  transform: scale(1.05);
}

.menu-expend .svg-icon {
  font-size: 20px !important;  /* 确保图标大小统一 */
}

/* Three-column layout containers */
.nav-left, .nav-center, .nav-right {
  display: flex;
  align-items: center;
  flex: 1 1 0;
}

.nav-left {
  justify-content: flex-start;
  gap: 12px;
}

.nav-center {
  justify-content: center;
}

.nav-right {
  justify-content: flex-end;
  gap: 16px;
}

.login-btn {
  font-weight: 500;
  padding: 10px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.login-btn:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 窗口可视区域小于1000隐藏搜索框 */
@media screen and (max-width: 1000px) {
  .search_input {
    display: none;
  }
}

@media screen and (max-width: 1200px) {
  /* Only apply semi-transparent background to the mobile dropdown menu items */
  .el-menu-hidden /deep/ .el-menu-item {
    background-color: rgba(255, 255, 255, 0.5) !important;
  }

  .el-menu-demo {
    display: none;
  }

  .menu-expend {
    display: flex !important;
    float: right;
  }

  .menu-expend:hover {
    transform: scale(1.1);
    cursor: pointer;
  }

  .logo {
    display: none;
  }
}
</style>
