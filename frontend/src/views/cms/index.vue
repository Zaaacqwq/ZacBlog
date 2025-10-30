<!-- main content area -->
<template>

  <el-container>
    <!-- <ParticlesBackground /> -->
    <!-- <div class="background" :style="backgroundImage"></div> -->
    <cmsNavBar></cmsNavBar>
    <keep-alive>
      <cmsMain></cmsMain>
    </keep-alive>
    <el-footer height="40px">
      <cmsFooter></cmsFooter>
    </el-footer>
  </el-container>
</template>

<script>
  import cmsMain from './main.vue'
  import cmsNavBar from './NavBar.vue'
  import cmsFooter from './Footer.vue'
  import ParticlesBackground from './ParticlesBackground.vue'

  export default {
    methods: {
      handleSelect(key, keyPath) {
        console.log(key, keyPath);
      }
    },
    components: {
      cmsMain,
      cmsNavBar,
      cmsFooter,
      ParticlesBackground
    },
    computed: {
      // backgroundImage() {
      //   // 根据背景图数组的长度随机选择索引
      //   const randIndex = Math.floor(Math.random() * this.backgroundImages.length)
      //   return {
      //     // 获取对应的图片资源并将其设置到`background-image`属性上
      //     backgroundImage: `url(${this.backgroundImages[randIndex]})`
      //   }
      // }
    },
  }
</script>

<style scoped>
  .el-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }
  
  .el-footer {
    background-color: rgba(84, 92, 100, 0.5);
  }

  .background {
    background-repeat: no-repeat;
    background-size: cover;
    margin: 0px;
    padding: 0px;
    top: 0;
    width: 100%;
    height: 120vh;
    position: fixed;
    z-index:-1;
  }

  @media screen and (max-width: 768px) {
    .title {
      width: 100%;
      background-position-x: center;
      background-position-y: 0;
    }
  }
</style>
