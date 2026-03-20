<!-- admin bar chart -->
<template>
  <div :class="className" :style="{height:height,width:width}" >
    <div class="panel-heading">
      <span class="panel-eyebrow">Notes</span>
      <h3 class="title">Draft rhythm</h3>
      <div class="panel-metric">{{ total }} total</div>
    </div>
    <div id="barChartId" style="height: 280px;width: 100%;"></div>
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import {
    essayChart,
  } from "@/api/cms/charts";
const animationDuration = 6000

export default {
  mixins: [resize],
  props: {
    className: {
      type: String,
      default: 'chart'
    },
    width: {
      type: String,
      default: '100%'
    },
    height: {
      type: String,
      default: '300px'
    }
  },
  data() {
    return {
      chart: null,
      date: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      data: [79, 52, 200, 334, 390, 330, 220],
      total: 0,
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.getEssayChartData()
    })
  },
  beforeDestroy() {
    if (!this.chart) {
      return
    }
    this.chart.dispose()
    this.chart = null
  },
  methods: {
    getEssayChartData(){
      essayChart().then(response => {
        this.date = response.datex;
        this.data = response.essayData;
        this.total = response.total;
        this.initChart()
      });
    },
    initChart() {
      this.chart = echarts.init(document.getElementById('barChartId'), 'macarons')

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { // 坐标轴指示器，坐标轴触发有效
            type: 'shadow' // 默认为直线，可选为：'line' | 'shadow'
          }
        },
        grid: {
          top: 10,
          left: '2%',
          right: '2%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [{
          type: 'category',
          data: this.date,
          axisTick: {
            alignWithLabel: true
          },
          axisLabel: {
             interval:0,
             rotate:40
          }
        }],
        yAxis: [{
          type: 'value',
          axisTick: {
            show: false
          }
        }],
        series: [{
          name: 'Notes',
          type: 'bar',
          stack: 'vistors',
          barWidth: '60%',
          data: this.data,
          animationDuration
        }]
      })
    }
  }
}
</script>

<style scoped>
  .panel-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 6px 12px;
    margin-bottom: 6px;
  }

  .panel-eyebrow {
    display: inline-block;
    color: rgba(29, 36, 51, 0.46);
    font-size: 11px;
    font-weight: 800;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  .title{
    margin: 0;
    color: #172033;
    font-size: 20px;
    font-weight:700;
    letter-spacing: -0.03em;
  }

  .panel-metric {
    color: rgba(29, 36, 51, 0.5);
    font-size: 13px;
    font-weight: 700;
  }
</style>
