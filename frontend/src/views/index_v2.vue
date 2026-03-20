<!-- index page for admin -->
<template>
  <div class="dashboard-editor-container">
    <section class="dashboard-hero">
      <div class="dashboard-hero__copy">
        <span class="dashboard-hero__eyebrow">Control Center</span>
        <h1 class="dashboard-hero__title">Site activity at a glance.</h1>
        <p class="dashboard-hero__text">
          Keep an eye on publishing, engagement, and note activity from one workspace.
        </p>
      </div>
      <div class="dashboard-hero__meta">
        <div class="dashboard-hero__meta-card">
          <span class="meta-label">Workspace</span>
          <strong class="meta-value">Zac Console</strong>
        </div>
        <div class="dashboard-hero__meta-card">
          <span class="meta-label">Focus</span>
          <strong class="meta-value">Content Ops</strong>
        </div>
      </div>
    </section>

    <panel-group @handleSetLineChartData="handleSetLineChartData" />

    <el-row class="dashboard-main-card dashboard-main-card--line">
      <div class="dashboard-main-card__header">
        <div>
          <span class="section-eyebrow">Traffic</span>
          <h3 class="section-title">Publishing and engagement trends</h3>
        </div>
      </div>
      <line-chart :chart-data="lineChartData" />
    </el-row>

    <el-row :gutter="28" class="dashboard-lower-grid">
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <raddar-chart />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <pie-chart />
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="8">
        <div class="chart-wrapper">
          <bar-chart />
        </div>
      </el-col>
    </el-row>


  </div>
</template>

<script>
import PanelGroup from './dashboard/PanelGroupCms'
import LineChart from './dashboard/LineChartCms'
import RaddarChart from './dashboard/TagChart'
import PieChart from './dashboard/PieChartCms'
import BarChart from './dashboard/BarChartCms'

const lineChartData = {
  views: {
    blogColor: 'rgba(243, 248, 255, 0)',
    commentColor: 'rgba(243, 248, 255, 0)',
    messageColor: 'rgba(243, 248, 255, 0)',
  },
  blog: {
    blogColor: 'rgba(56, 136, 250, 0.2)',
    commentColor: 'rgba(243, 248, 255, 0)',
    messageColor: 'rgba(243, 248, 255, 0)',
  },
  comment: {
    blogColor: 'rgba(243, 248, 255, 0)',
    commentColor: 'rgba(255, 0, 90, 0.2)',
    messageColor: 'rgba(243, 248, 255, 0)',
  },
  message: {
    blogColor: 'rgba(243, 248, 255, 0)',
    commentColor: 'rgba(243, 248, 255, 0)',
    messageColor: 'rgba(52, 191, 163, 0.2)',
  }
}

export default {
  name: 'Index',
  components: {
    PanelGroup,
    LineChart,
    RaddarChart,
    PieChart,
    BarChart
  },
  data() {
    return {
      lineChartData: lineChartData.views
    }
  },
  methods: {
    handleSetLineChartData(type) {
      this.lineChartData = lineChartData[type]
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-editor-container {
  padding: 28px 24px 40px;
  min-height: 100%;
  background:
    radial-gradient(circle at top left, rgba(174, 123, 50, 0.08), transparent 28%),
    linear-gradient(180deg, #f7f8fb 0%, #edf1f6 100%);
  position: relative;

  .dashboard-hero {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 24px;
    margin-bottom: 24px;
    padding: 28px 30px;
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 32px;
    background:
      linear-gradient(135deg, rgba(255, 252, 245, 0.96) 0%, rgba(250, 247, 240, 0.92) 55%, rgba(240, 246, 255, 0.88) 100%);
    box-shadow: 0 28px 70px rgba(18, 27, 43, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.76);
  }

  .dashboard-hero__copy {
    max-width: 640px;
  }

  .dashboard-hero__eyebrow,
  .section-eyebrow {
    display: inline-block;
    margin-bottom: 8px;
    color: rgba(29, 36, 51, 0.48);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  .dashboard-hero__title {
    margin: 0;
    color: #172033;
    font-size: 34px;
    line-height: 1.05;
    letter-spacing: -0.04em;
    font-weight: 700;
  }

  .dashboard-hero__text {
    margin: 12px 0 0;
    color: rgba(29, 36, 51, 0.6);
    font-size: 15px;
    line-height: 1.7;
  }

  .dashboard-hero__meta {
    display: grid;
    grid-template-columns: repeat(2, minmax(150px, 1fr));
    gap: 12px;
    min-width: 320px;
  }

  .dashboard-hero__meta-card {
    padding: 16px 18px;
    border: 1px solid rgba(23, 32, 51, 0.07);
    border-radius: 22px;
    background: rgba(255, 255, 255, 0.66);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
  }

  .meta-label {
    display: block;
    margin-bottom: 8px;
    color: rgba(29, 36, 51, 0.46);
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .meta-value {
    color: #172033;
    font-size: 16px;
    font-weight: 800;
  }

  .dashboard-main-card {
    margin-bottom: 28px;
    padding: 22px 24px 8px;
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 32px;
    background: rgba(255, 253, 249, 0.86);
    box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.75);
  }

  .dashboard-main-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .section-title {
    margin: 0;
    color: #172033;
    font-size: 24px;
    font-weight: 700;
    letter-spacing: -0.03em;
  }

  .chart-wrapper {
    height: 100%;
    padding: 22px 22px 10px;
    border: 1px solid rgba(23, 32, 51, 0.08);
    border-radius: 28px;
    background: rgba(255, 253, 249, 0.84);
    box-shadow: 0 24px 60px rgba(18, 27, 43, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.72);
  }

  .dashboard-lower-grid {
    display: flex;
    flex-wrap: wrap;
  }
}

@media (max-width:1024px) {
  .dashboard-editor-container {
    padding: 20px 16px 32px;
  }

  .dashboard-editor-container .dashboard-hero {
    flex-direction: column;
    align-items: stretch;
    padding: 22px 20px;
  }

  .dashboard-editor-container .dashboard-hero__meta {
    min-width: 0;
    grid-template-columns: 1fr 1fr;
  }

  .dashboard-editor-container .dashboard-hero__title {
    font-size: 28px;
  }

  .dashboard-editor-container .dashboard-main-card,
  .dashboard-editor-container .chart-wrapper {
    padding: 18px 16px 8px;
  }
}
</style>
