package com.zaaac.cms.fileInfo.service.impl;
/**
 * @program: Zaaac-Vue
 * @Author: WangNing
 * @Description: 〈〉
 * @Date: 2022/1/2 1:00
 */

import com.zaaac.cms.fileInfo.domain.FileBlogInfo;
import com.zaaac.cms.fileInfo.mapper.FileBlogInfoMapper;
import com.zaaac.cms.fileInfo.service.IFileBlogInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: WangNing
 * @Description:〈〉
 * @Date: 2022/1/2 1:00
 **/
@Service
public class FileBlogInfoServiceImpl implements IFileBlogInfoService {

    @Autowired
    private FileBlogInfoMapper fileBlogInfoMapper;

    @Override
    public int batchFileBlog(List<FileBlogInfo> fileBlogList) {
        return fileBlogInfoMapper.batchFileBlog(fileBlogList);
    }

    @Override
    public int deleteFileBlogByBlogId(Long blogId) {
        return fileBlogInfoMapper.deleteFileBlogByBlogId(blogId);
    }

    @Override
    public int deleteFileBlog(Long[] ids) {
        return fileBlogInfoMapper.deleteFileBlog(ids);
    }

    @Override
    public List<FileBlogInfo> selectFileBlogList(Long blogId) {
        return fileBlogInfoMapper.selectFileBlogList(blogId);
    }
}
