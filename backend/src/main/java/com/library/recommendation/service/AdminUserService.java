package com.library.recommendation.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.SysUser;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.SysUserMapper;
import com.library.recommendation.vo.PageResult;
import com.library.recommendation.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final SysUserMapper userMapper;
    private final BorrowRecordMapper borrowMapper;

    public PageResult<UserVO> page(long page, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(w -> w.like(SysUser::getUsername, value)
                    .or().like(SysUser::getNickname, value)
                    .or().like(SysUser::getEmail, value));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(50, Math.max(1, size))), wrapper);
        return PageResult.of(result.getRecords().stream().map(UserVO::from).toList(),
                result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        if (user.getId().equals(StpUtil.getLoginIdAsLong()) && status == 0) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void updateRole(Long id, String role) {
        SysUser user = requireUser(id);
        if (user.getId().equals(StpUtil.getLoginIdAsLong()) && !"ADMIN".equals(role)) {
            throw new BusinessException("不能取消当前登录账号的管理员角色");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (user.getId().equals(StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("不能删除当前登录账号");
        }
        Long borrowCount = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, id));
        if (borrowCount != null && borrowCount > 0) {
            throw new BusinessException("该用户存在借阅记录，无法删除；可将其禁用");
        }
        userMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }
}
