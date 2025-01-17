package com.zaaac.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.zaaac.common.core.controller.BaseController;
import com.zaaac.common.core.domain.AjaxResult;
import com.zaaac.common.core.domain.model.RegisterBody;
import com.zaaac.common.utils.StringUtils;
import com.zaaac.framework.web.service.SysRegisterService;
import com.zaaac.system.service.ISysConfigService;

/**
 * 注册验证
 * 
 * @author zaaac
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    @Autowired
    private ISysConfigService configService;

    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("The registration feature is not enabled in the current system!");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
