package com.library.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.dto.ChangePasswordDTO;
import com.library.recommendation.dto.LoginDTO;
import com.library.recommendation.dto.RegisterDTO;
import com.library.recommendation.dto.UpdateProfileDTO;
import com.library.recommendation.entity.SysUser;
import com.library.recommendation.mapper.SysUserMapper;
import com.library.recommendation.vo.LoginVO;
import com.library.recommendation.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginVO register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.username()));
        if (count != null && count > 0) {
            throw new BusinessException("该用户名已被注册");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.username().trim());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setNickname(StringUtils.hasText(dto.nickname()) ? dto.nickname().trim() : dto.username());
        user.setEmail(StringUtils.hasText(dto.email()) ? dto.email().trim() : null);
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), UserVO.from(user));
    }

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.username().trim())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        StpUtil.login(user.getId());
        return new LoginVO(StpUtil.getTokenValue(), UserVO.from(user));
    }

    public void logout() {
        StpUtil.logout();
    }

    public SysUser currentEntity() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，请重新登录");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw new BusinessException(403, "账号已被禁用");
        }
        return user;
    }

    public UserVO currentUser() {
        return UserVO.from(currentEntity());
    }

    public UserVO updateProfile(UpdateProfileDTO dto) {
        SysUser user = currentEntity();
        user.setNickname(dto.nickname().trim());
        user.setEmail(StringUtils.hasText(dto.email()) ? dto.email().trim() : null);
        user.setPhone(StringUtils.hasText(dto.phone()) ? dto.phone().trim() : null);
        user.setAvatar(StringUtils.hasText(dto.avatar()) ? dto.avatar().trim() : null);
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    public void changePassword(ChangePasswordDTO dto) {
        SysUser user = currentEntity();
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (passwordEncoder.matches(dto.newPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userMapper.updateById(user);
        StpUtil.logout();
    }
}
