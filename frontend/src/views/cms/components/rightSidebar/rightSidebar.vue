<template>
    <div class="right-sidebar" >
        <el-card style="background-color: rgba(255,255,255,0.9)" class="right-item">
            <div slot="header" class="attributes">
                <b>About Zac</b>
            </div>
            <div class="profile-card">
                <img :src="avatar" alt="Zac's Avatar" class="avatar" />
                <h3>Zac</h3>
                <p>CE student @ UW</p>
                <div class="stats">
                    <p><strong>{{ totalBlogs }}</strong> Blogs</p>
                    <p><strong>{{ totalViews }}</strong> Views</p>
                </div>
                <div class="social-links">
                    <a href="https://github.com/Zaaacqwq" target="_blank">
                        <svg-icon icon-class="github-thin" class="social-icon" />
                    </a>
                    <a href="mailto:zacchenzy@gmail.com" target="_blank">
                        <svg-icon icon-class="email-thin" class="social-icon" />
                    </a>
                    <a href="https://www.instagram.com/zaaacqwq" target="_blank">
                        <svg-icon icon-class="instagram-thin" class="social-icon" />
                    </a>
                    <a href="https://www.linkedin.com/in/zaaac" target="_blank">
                        <svg-icon icon-class="linkedin-thin" class="social-icon" />
                    </a>
                </div>
            </div>
        </el-card>
        <el-card style="background-color: rgba(255,255,255,0.9)" class="right-item">
            <div slot="header" class="attributes">
                <b>Customized</b>
            </div>
            <div class="custom-section">
                <p>Welcome to Zac's Blog!</p>
            </div>
        </el-card>
        <el-card style="background-color: rgba(255,255,255,0.9)" class="right-item">
            <div slot="header" class="attributes">
                <b>Announcement!</b>
            </div>
            <div class="announcement-section">
                <p v-if="latestAnnouncement">{{ latestAnnouncement }}</p>
                <p v-else>Loading latest announcement...</p>
            </div>
        </el-card>
        <el-card style="background-color: rgba(255,255,255,0.9)" class="right-item">
            <div slot="header" class="attributes">
                <b>SiteInfo</b>
            </div>
            <div class="site-info">
                <p><strong>Blogs Count:</strong> {{ totalBlogs }}</p>
                <p><strong>Views Count:</strong> {{ totalViews }}</p>
                <p><strong>Comments Count:</strong> {{ totalComments }}</p>
                <p><strong>Messages Count:</strong> {{ totalMessages }}</p>
            </div>
        </el-card>
    </div>
</template>

<script>
import avatar from '@/assets/images/avatar.png';
import {
    cmsListRecommend
} from "@/api/cms/blog";
import { listNotice } from "@/api/system/notice";
import { total } from "@/api/cms/charts";
export default {
    name: "RightSidebar",
    data() {
        return {
            avatar,
            totalBlogs: 0,
            totalViews: 0,
            totalComments: 0,
            totalMessages: 0,
            recommendList: [],
            latestAnnouncement: '',
        };
    },
    created() {
        this.fetchStatistics();
    },
    mounted() {
        this.fetchLatestAnnouncement();
        this.getRecommendList();
    },
    methods: {
        async fetchLatestAnnouncement() {
            try {
                const response = await listNotice({
                    pageSize: 10,
                    pageNum: 1,
                });

                if (response.code === 200 && response.rows && response.rows.length > 0) {
                    const latestAnnouncement = response.rows.reduce((latest, current) =>
                        current.noticeId > latest.noticeId ? current : latest
                    );
                    this.latestAnnouncement = latestAnnouncement.noticeTitle || 'No title available';
                } else {
                    this.latestAnnouncement = 'No announcements available.';
                }
            } catch (error) {
                console.error('Error fetching the latest announcement:', error);
                this.latestAnnouncement = 'Unable to load the latest announcement.';
            }
        },
        async fetchStatistics() {
            try {
                const response = await total();
                this.totalViews = response.views || 0;
                this.totalBlogs = response.blog || 0;
                this.totalComments = response.comment || 0;
                this.totalMessages = response.message || 0;
            } catch (error) {
                console.error("Error fetching statistics:", error);
            }
        },
        async getRecommendList() {
            try {
                const response = await cmsListRecommend();
                this.recommendList = response.rows.slice(0, 4);
            } catch (error) {
                console.error("Error fetching recommend list:", error);
            }
        },
        getBlogInfo(blogId) {
            let routeUrl = this.$router.resolve({
                path: '/cms/main/blog',
                query: {
                    id: blogId
                }
            });
            window.open(routeUrl.href, '_blank');
        },
    },
};
</script>


<style scoped>
.el-card /deep/ .el-card__body {
    padding: 0;
}

.right-sidebar {
    display: flex;
    flex-direction: column;
}

.right-item {
    margin-bottom: 20px;
}

.profile-card {
    text-align: center;
}

.avatar {
    width: 150px;
    height: 150px;
    border-radius: 50%;
    margin: 10px 0px;
}

.stats {
    display: flex;
    justify-content: space-around;
    margin: 10px 0;
}

.social-links {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 10px;
    margin: 10px 0;
}

.social-links a {
    margin: 0px 10px;
    text-decoration: none;
    color: #007bff;
}

.social-icon {
    width: 40px;
    height: 40px;
}

.custom-section {
    text-align: center;
    padding: 10px;
    font-size: 24px;
    line-height: 1.6;
}

.announcement-section {
    text-align: center;
    padding: 10px;
    font-size: 24px;
    line-height: 1.6;
    border-radius: 5px;
}

.site-info {
    padding: 10px;
    font-size: 14px;
    line-height: 1.6;
    border-radius: 5px;
}
</style>