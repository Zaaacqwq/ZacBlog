package com.zaaac.cms.fileInfo.mapper;

import com.zaaac.cms.fileInfo.domain.FileNoticeInfo;

import java.util.List;

/**
 * @program: Zaaac-Vue
 * @Author: Zaaac
 * @Description: 〈通知公告和文件关联 数据层〉
 * @Date: 2021/12/31 22:24
 */
public interface FileNoticeInfoMapper {
    /**
     * 批量新增
     */
    public int batchFileNotice(List<FileNoticeInfo> userRoleList);
    /**
     * 通过通知公告ID删除通知公告和文件关联
     */
    public int deleteFileNoticeByNoticeId(Long noticeId);
    /**
     * 批量删除通知公告和文件关联
     */
    public int deleteFileNotice(Long[] ids);
    /**
     * 查询文件列表
     */
    public List<FileNoticeInfo> selectFileNoticeList(Long noticeId);
}
