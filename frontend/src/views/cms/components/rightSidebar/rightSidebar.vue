<!-- right sidebar -->
<template>
  <div class="right-sidebar">
    <el-card class="right-item profile-panel">
      <div class="eyebrow">Editor</div>
      <div class="profile-card">
        <img :src="avatar" alt="Zac's Avatar" class="avatar" />
        <h3>Zac</h3>
        <p class="role-line">Building software, writing notes, shipping experiments.</p>
        <div class="stats-grid">
          <div class="stat-box">
            <strong>{{ totalBlogs }}</strong>
            <span>Posts</span>
          </div>
          <div class="stat-box">
            <strong>{{ totalViews }}</strong>
            <span>Views</span>
          </div>
        </div>
        <div class="social-links">
          <a href="https://github.com/Zaaacqwq" target="_blank" rel="noopener noreferrer" aria-label="GitHub">
            <svg-icon icon-class="github-line" class="social-icon" />
          </a>
          <a href="https://portfolio.zaaac.vip/" target="_blank" rel="noopener noreferrer" aria-label="Portfolio">
            <svg-icon icon-class="user-line" class="social-icon" />
          </a>
          <a href="https://www.instagram.com/zaaacqwq" target="_blank" rel="noopener noreferrer" aria-label="Instagram">
            <svg-icon icon-class="instagram-line" class="social-icon" />
          </a>
          <a href="https://www.linkedin.com/in/zaaac" target="_blank" rel="noopener noreferrer" aria-label="LinkedIn">
            <svg-icon icon-class="linkedin-box-line" class="social-icon" />
          </a>
        </div>
      </div>
    </el-card>

    <el-card class="right-item note-panel">
      <div class="eyebrow">About This Site</div>
      <p class="supporting-copy">
        A personal corner for engineering notes, longer essays, experiments, and the occasional public draft.
      </p>
    </el-card>

    <el-card class="right-item note-panel">
      <div class="eyebrow">Latest Notice</div>
      <p class="supporting-copy" v-if="latestAnnouncement">{{ latestAnnouncement }}</p>
      <p class="supporting-copy" v-else>Loading latest announcement...</p>
    </el-card>

    <el-card class="right-item stats-panel">
      <div class="eyebrow">Site Numbers</div>
      <div class="stat-row">
        <span>Articles</span>
        <strong>{{ totalBlogs }}</strong>
      </div>
      <div class="stat-row">
        <span>Views</span>
        <strong>{{ totalViews }}</strong>
      </div>
      <div class="stat-row">
        <span>Comments</span>
        <strong>{{ totalComments }}</strong>
      </div>
      <div class="stat-row">
        <span>Messages</span>
        <strong>{{ totalMessages }}</strong>
      </div>
    </el-card>
  </div>
</template>

<script>
import avatar from '@/assets/images/avatar.png'
import { latestNotice } from '@/api/system/notice'
import { info } from '@/api/cms/charts'

export default {
  name: 'RightSidebar',
  data() {
    return {
      avatar,
      totalBlogs: 0,
      totalViews: 0,
      totalComments: 0,
      totalMessages: 0,
      latestAnnouncement: '',
    };
  },
  created() {
    this.fetchStatistics()
  },
  mounted() {
    this.fetchLatestAnnouncement()
  },
  methods: {
    async fetchLatestAnnouncement() {
      try {
        const response = await latestNotice({
          pageSize: 10,
          pageNum: 1,
        })

        if (response.code === 200 && response.rows && response.rows.length > 0) {
          const latestAnnouncement = response.rows.reduce((latest, current) =>
            current.noticeId > latest.noticeId ? current : latest
          )
          this.latestAnnouncement = latestAnnouncement.noticeTitle || 'No title available'
        } else {
          this.latestAnnouncement = 'No announcements available.'
        }
      } catch (error) {
        console.error('Error fetching the latest announcement:', error)
        this.latestAnnouncement = 'Unable to load the latest announcement.'
      }
    },
    async fetchStatistics() {
      try {
        const response = await info()
        this.totalViews = response.views || 0
        this.totalBlogs = response.blog || 0
        this.totalComments = response.comment || 0
        this.totalMessages = response.message || 0
      } catch (error) {
        console.error('Error fetching statistics:', error)
      }
    },
  },
}
</script>

<style scoped>
.right-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-item {
  overflow: hidden;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(29, 36, 51, 0.08);
  box-shadow: 0 24px 60px rgba(20, 28, 43, 0.08);
}

.right-item /deep/ .el-card__body {
  padding: 24px;
}

.eyebrow {
  margin-bottom: 16px;
  color: rgba(29, 36, 51, 0.5);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.profile-card {
  text-align: left;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  margin-bottom: 18px;
  object-fit: cover;
  box-shadow: 0 18px 32px rgba(23, 32, 51, 0.14);
}

.profile-card h3 {
  margin: 0 0 8px;
  font-size: 1.7rem;
  font-family: "Iowan Old Style", "Palatino Linotype", Georgia, serif;
  color: #1d2433;
}

.role-line,
.supporting-copy {
  margin: 0;
  line-height: 1.7;
  color: rgba(29, 36, 51, 0.7);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 22px 0 18px;
}

.stat-box {
  padding: 14px;
  border-radius: 18px;
  background: rgba(23, 32, 51, 0.04);
}

.stat-box strong,
.stat-row strong {
  display: block;
  font-size: 1.05rem;
  font-weight: 500;
  color: rgba(29, 36, 51, 0.82);
}

.stat-box span,
.stat-row span {
  color: rgba(29, 36, 51, 0.58);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.social-links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.social-links a {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: rgba(17, 17, 17, 0.045);
  color: rgba(29, 36, 51, 0.82);
  transition: 0.18s ease;
}

.social-links a:hover {
  transform: translateY(-2px);
  background: rgba(23, 32, 51, 0.08);
  color: rgba(23, 32, 51, 0.96);
}

.social-icon {
  width: 21px;
  height: 21px;
  display: block;
  object-fit: contain;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(29, 36, 51, 0.08);
}

.stat-row:last-child {
  border-bottom: 0;
}
</style>
