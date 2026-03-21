<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link is-collapse" to="/index">
        <img v-if="logo" :src="logo" :class="['sidebar-logo', 'is-collapsed', { 'is-light': sideTheme === 'theme-dark' }]" />
        <h1 v-else class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link is-expand" to="/index">
        <img v-if="logo" :src="logo" :class="['sidebar-logo', 'is-expanded', { 'is-light': sideTheme === 'theme-dark' }]" />
        <h1 class="sidebar-title" :style="{ color: sideTheme === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor }">{{ title }} </h1>
      </router-link>
    </transition>
  </div>
</template>

<script>
import logoImg from '@/assets/images/logo.png'
import variables from '@/assets/styles/variables.scss'
export default {
  name: 'SidebarLogo',
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {
    variables() {
      return variables
    },
    sideTheme() {
      return this.$store.state.settings.sideTheme
    }
  },
  data() {
    return {
      title: 'Zac Console',
      logo: logoImg
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  width: 100%;
  height: 92px;
  padding-top: 8px;
  background: transparent;
  overflow: hidden;

  .sidebar-logo-link {
    width: 100%;
    height: 100%;
    display: flex !important;
    flex-direction: row !important;
    flex-wrap: nowrap !important;
    align-items: center !important;
    text-decoration: none;
    box-sizing: border-box;
    transition: padding .28s, gap .28s, justify-content .28s;
  }

  .sidebar-logo-link.is-expand {
    justify-content: flex-start !important;
    gap: 12px;
    padding: 0 16px;
  }

  .sidebar-logo-link.is-collapse {
    justify-content: center !important;
    padding: 0;
  }

  .sidebar-logo {
    display: block;
    width: 88px;
    height: 88px;
    flex: 0 0 auto;
    object-fit: cover;
    transition: width .28s, height .28s, margin .28s, filter .28s;

    &.is-light {
      filter: brightness(0) invert(1);
    }
  }

  .sidebar-logo.is-collapsed {
    width: 88px;
    height: 88px;
  }

  .sidebar-logo.is-expanded {
    width: 64px;
    height: 64px;
    margin-right: -4px;
    object-fit: contain;
  }

  .sidebar-title {
    display: block !important;
    flex: 0 0 auto;
    margin: 0;
    color: #fff;
    font-weight: 700;
    line-height: 1;
    font-size: 15px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    font-family: "Avenir Next", "Segoe UI", sans-serif;
    white-space: nowrap;
    transition: opacity .2s, transform .28s;
  }

  &.collapse {
    .sidebar-title {
      display: none;
    }
  }
}
</style>
