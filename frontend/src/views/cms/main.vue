<!-- main page -->
<template>
    <el-main>
      <router-view :key="key" />
    </el-main>
</template>

<script>
  export default {
    name: 'cmsMain',
    computed: {
      key() {
        return this.$route.path
      }
    }
  }
</script>

<style scoped>
  .el-main {
    min-height: 650px;
    margin: 0;
    padding: 0;
    overflow-x: hidden;
  }
</style>
