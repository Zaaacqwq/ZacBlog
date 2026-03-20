<!-- admin line chart -->
<template>
  <div :class="className" :style="{height:height,width:width}" />
</template>

<script>
import echarts from 'echarts'
require('echarts/theme/macarons') // echarts theme
import resize from './mixins/resize'
import {
    lineChart,
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
      default: '350px'
    },
    autoResize: {
      type: Boolean,
      default: true
    },
    chartData: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      chart: null,
      datex:['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      blogData: [100, 120, 161, 134, 105, 160, 165],
      commentData: [120, 82, 91, 154, 162, 140, 145],
      messageData: [100, 72, 191, 54, 62, 100, 105]
    }
  },
  watch: {
    chartData: {
      deep: true,
      handler(val) {
        this.setOptions(val)
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initChart()
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
    initChart() {
      this.getLineChartData();
    },
    getLineChartData(){
      lineChart().then(response => {
        this.datex = response.datex;
        this.blogData = response.blogData;
        this.commentData = response.commentData;
        this.messageData = response.messageData;
        this.chart = echarts.init(this.$el, 'macarons')
        this.setOptions(this.chartData)
      });
    },
    setOptions({ blogColor, commentColor, messageColor } = {}) {
      this.chart.setOption({
        xAxis: {
          data: this.datex,
          boundaryGap: false,
          axisLine: {
            lineStyle: {
              color: 'rgba(23, 32, 51, 0.12)'
            }
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: 'rgba(29, 36, 51, 0.46)'
          }
        },
        grid: {
          left: 8,
          right: 18,
          bottom: 12,
          top: 44,
          containLabel: true
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross'
          },
          padding: [10, 14],
          backgroundColor: 'rgba(23, 32, 51, 0.92)',
          borderWidth: 0,
          textStyle: {
            color: '#fffdfa'
          }
        },
        yAxis: {
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: 'rgba(29, 36, 51, 0.42)'
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(23, 32, 51, 0.08)'
            }
          }
        },
        legend: {
          top: 0,
          right: 0,
          itemWidth: 10,
          itemHeight: 10,
          icon: 'circle',
          textStyle: {
            color: 'rgba(29, 36, 51, 0.58)'
          },
          data: [ 'Blog','Comment','Message']
        },
        series: [
        {
          name: 'Blog',
          smooth: true,
          type: 'line',
          itemStyle: {
            normal: {
              color: '#3888fa',
              lineStyle: {
                color: '#3888fa',
                width: 3
              },
              areaStyle: {
                color: blogColor
              }
            }
          },
          symbol: 'circle',
          symbolSize: 7,
          data: this.blogData,
          animationDuration: 2800,
          animationEasing: 'quadraticOut'
        },
        {
          name: 'Comment', itemStyle: {
            normal: {
              color: '#FF005A',
              lineStyle: {
                color: '#FF005A',
                width: 3
              },
              areaStyle: {
                color: commentColor
              }
            }
          },
          symbol: 'circle',
          symbolSize: 7,
          smooth: true,
          type: 'line',
          data: this.commentData,
          animationDuration: 2800,
          animationEasing: 'cubicInOut'
        },
        {
          name: 'Message', itemStyle: {
            normal: {
              color: '#34bfa3',
              lineStyle: {
                color: '#34bfa3',
                width: 3
              },
              areaStyle: {
                color: messageColor
              }
            }
          },
          symbol: 'circle',
          symbolSize: 7,
          smooth: true,
          type: 'line',
          data: this.messageData,
          animationDuration: 2800,
          animationEasing: 'cubicInOut'
        }]
      })
    }
  }
}
</script>
