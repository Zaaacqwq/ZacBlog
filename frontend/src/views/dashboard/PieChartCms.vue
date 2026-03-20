<!-- admin pie chart -->
<template>
  <div :class="className" :style="{height:height,width:width}" >
    <div class="panel-heading">
      <span class="panel-eyebrow">Category Mix</span>
      <h3 class="title">Publishing balance</h3>
    </div>
    <div id="pieChartId" style="height: 280px;width: 100%;"></div>
  </div>
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import {
    pieChart,
  } from "@/api/cms/charts";
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
      type:['Industries', 'Technology', 'Forex', 'Gold', 'Forecasts'],
      data:[
              { value: 320, name: 'Industries' },
              { value: 240, name: 'Technology' },
              { value: 149, name: 'Forex' },
              { value: 100, name: 'Gold' },
              { value: 59, name: 'Forecasts' }
            ],
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.getPieChart()
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
    getPieChart(){
      pieChart().then(response => {
        this.type = response.type;
        this.data = response.data;
        this.initChart()
      });
    },
    initChart() {
      this.chart = echarts.init(document.getElementById('pieChartId'), 'macarons')

      this.chart.setOption({
        // title: {
        //     text: '分类',
        //     // subtext: 'Fake Data',
        //     textStyle:{
        //       color: 'rgba(0, 0, 0, 0.45)',
        //       fontWeight: 'bold',
        //       fontSize: '16'
        //     }
        //   },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b} : {c} ({d}%)'
        },
        legend: {
          left: 'center',
          bottom: '10',
          data: this.type
        },
        series: [
          {
            name: 'Category',
            type: 'pie',
            roseType: 'radius',
            radius: [15, 75],
            center: ['50%', '38%'],
            data: this.data,
            animationEasing: 'cubicInOut',
            animationDuration: 2600
          }
        ]
      })
    }
  }
}
</script>

<style scoped>
  .panel-heading {
    margin-bottom: 6px;
  }

  .panel-eyebrow {
    display: inline-block;
    margin-bottom: 8px;
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
</style>
