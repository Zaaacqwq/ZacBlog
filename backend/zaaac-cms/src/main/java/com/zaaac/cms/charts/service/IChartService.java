package com.zaaac.cms.charts.service;

import com.zaaac.cms.blog.domain.CmsBlog;
import com.zaaac.cms.comment.domain.CmsComment;
import com.zaaac.cms.message.domain.CmsMessage;

import java.util.List;

/**
 * @program: zaaac-vue-blog
 * @Author: Zaaac
 * @Description: 〈${DESCRIPTION}〉
 * @Date: 2022/4/27 10:17
 */
public interface IChartService {
    public List<CmsBlog> selectList(CmsBlog cmsBlog);
    public List<CmsBlog> selectListBetweenCreateTime(CmsBlog cmsBlog,String createTimeBegin,String createTimeEnd);
    public List<CmsComment> selectCmsCommentListBetweenCreateTime(CmsComment cmsComment,String createTimeBegin,String createTimeEnd);
    public List<CmsMessage> selectCmsMessageListBetweenCreateTime(CmsMessage cmsMessage,String createTimeBegin,String createTimeEnd);
}
