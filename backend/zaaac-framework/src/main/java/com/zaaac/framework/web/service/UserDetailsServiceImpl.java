package com.zaaac.framework.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.zaaac.common.core.domain.entity.SysUser;
import com.zaaac.common.core.domain.model.LoginUser;
import com.zaaac.common.enums.UserStatus;
import com.zaaac.common.exception.ServiceException;
import com.zaaac.common.utils.StringUtils;
import com.zaaac.system.service.ISysUserService;

/**
 * 用户验证处理
 *
 * @author zaaac
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService
{
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        SysUser user = userService.selectUserByUserName(username);
        if (StringUtils.isNull(user))
        {
            log.info("Login user: {} does not exist.", username);
            throw new ServiceException("Login user: \" + username + \" does not exist.");
        }
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            log.info("Login user: {} has been deleted.", username);
            throw new ServiceException("Sorry, your account: \" + username + \" has been deleted.");
        }
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            log.info("Login user: {} has been disabled.", username);
            throw new ServiceException("Sorry, your account: \" + username + \" has been disabled.");
        }

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user)
    {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }
}
