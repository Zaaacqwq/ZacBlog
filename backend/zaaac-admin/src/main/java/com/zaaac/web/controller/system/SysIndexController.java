package com.zaaac.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zaaac.common.config.RuoYiConfig;
import com.zaaac.common.utils.StringUtils;

/**
 * 首页
 *
 * @author zaaac
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("Welcome to the {} backend management framework. Current version: v{}. Please access through the frontend address.", ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }
}
