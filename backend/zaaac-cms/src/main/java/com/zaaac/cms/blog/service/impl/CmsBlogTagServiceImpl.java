package com.zaaac.cms.blog.service.impl;
/**
 * @program: Zaaac-Vue
 * @Author: WangNing
 * @Description: 〈〉
 * @Date: 2022/1/2 23:24
 */

import com.zaaac.cms.blog.domain.CmsBlogTag;
import com.zaaac.cms.blog.mapper.CmsBlogTagMapper;
import com.zaaac.cms.blog.service.ICmsBlogTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: WangNing
 * @Description:〈〉
 * @Date: 2022/1/2 23:24
 **/
@Service
public class CmsBlogTagServiceImpl implements ICmsBlogTagService {

    @Autowired
    CmsBlogTagMapper cmsBlogTagMapper;

    @Override
    public int batchBlogTag(List<CmsBlogTag> blogTagList) {
        return cmsBlogTagMapper.batchBlogTag(blogTagList);
    }

    @Override
    public int deleteBlogTagByBlogId(Long blogId) {
        return cmsBlogTagMapper.deleteBlogTagByBlogId(blogId);
    }

    @Override
    public int deleteBlogTag(Long[] ids) {
        return cmsBlogTagMapper.deleteBlogTag(ids);
    }

    @Override
    public List<CmsBlogTag> selectBlogTagList(Long blogId) {
        return cmsBlogTagMapper.selectBlogTagList(blogId);
    }
}
