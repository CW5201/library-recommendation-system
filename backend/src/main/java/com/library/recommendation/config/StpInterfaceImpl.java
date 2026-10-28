package com.library.recommendation.config;

import cn.dev33.satoken.stp.StpInterface;
import com.library.recommendation.entity.SysUser;
import com.library.recommendation.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userMapper.selectById(Long.valueOf(String.valueOf(loginId)));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return Collections.emptyList();
        }
        return List.of(user.getRole());
    }
}
