<!-- admin navbar -->
<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" v-if="!topNav"/>
    <top-nav id="topmenu-container" class="topmenu-container" v-if="topNav"/>

    <div class="right-menu">
      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <!-- <img :src="avatar" class="user-avatar"> -->
          <el-avatar class="user-avatar" :src="avatar" @error="errorHandler">
            <i class="el-icon-s-custom" />
          </el-avatar>
          <div class="avatar-Name">{{name}}</div>
        </div>
        <el-dropdown-menu slot="dropdown">
          <router-link to="/user/profile">
            <el-dropdown-item>Profile</el-dropdown-item>
          </router-link>
          <el-dropdown-item @click.native="setting = true">
            <span>Layout Settings</span>
          </el-dropdown-item>
          <el-dropdown-item divided @click.native="logout">
            <span>Logout</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Breadcrumb from '@/components/Breadcrumb'
import TopNav from '@/components/TopNav'
import Hamburger from '@/components/Hamburger'

export default {
  components: {
    Breadcrumb,
    TopNav,
    Hamburger,
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'device',
      'name'
    ]),
    setting: {
      get() {
        return this.$store.state.settings.showSettings
      },
      set(val) {
        this.$store.dispatch('settings/changeSetting', {
          key: 'showSettings',
          value: val
        })
      }
    },
    topNav: {
      get() {
        return this.$store.state.settings.topNav
      }
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      this.$confirm('Are you sure you want to log out and exit the system?', 'Warning', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('LogOut').then(() => {
          location.href = '/index';
        })
      }).catch(() => {});
    },
    errorHandler() {
            return true
          },
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 64px;
  overflow: hidden;
  position: relative;
  background: rgba(255, 252, 245, 0.76);
  border: 1px solid rgba(23, 32, 51, 0.08);
  border-radius: 22px;
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 48px rgba(18, 27, 43, 0.08);

  .hamburger-container {
    line-height: 60px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 64px;
    display: flex;
    align-items: center;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #4b5361;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 0, 0, .025)
        }
      }
    }

    .avatar-container {
      margin-right: 5px;

      .avatar-wrapper {
        height: 100%;
        position: relative;
        display: flex;
        align-items: center;
        gap: 10px;

        .user-avatar {
          cursor: pointer;
          border: dashed rgba($color: #ffff7f, $alpha: 0.5);
          flex: 0 0 auto;
        }
        .avatar-Name {
          cursor: pointer;
          display: flex;
          align-items: center;
          font-size: 14px;
          font-weight: bold;
          color: #1d2433;
          letter-spacing: 0.04em;
          text-transform: uppercase;
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
