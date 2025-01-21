package com.zaaac.cms.message.mapper;

import com.zaaac.cms.message.domain.CmsMessageLike;

import java.util.List;

/**
 * @program: Zaaac-Vue
 * @Author: WangNing
 * @Description: 〈数据层〉
 * @Date: 2022/1/19 8:42
 */
public interface CmsMessageLikeMapper {
    /**
     * 查询列表
     */
    public List<CmsMessageLike> selectCmsMessageLikeList(CmsMessageLike cmsMessageLike);
    /**
     * 新增
     */
    public int addCmsMessageLike(CmsMessageLike cmsMessageLike);
    /**
     * 删除关联
     */
    public int deleteCmsMessageLike(CmsMessageLike cmsMessageLike);
}
