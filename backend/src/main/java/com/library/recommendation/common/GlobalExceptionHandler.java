package com.library.recommendation.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getCode())
                .body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException validEx && validEx.getBindingResult().getFieldError() != null) {
            message = validEx.getBindingResult().getFieldError().getDefaultMessage();
        } else if (ex instanceof BindException bindEx && bindEx.getBindingResult().getFieldError() != null) {
            message = bindEx.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(Result.fail(400, ex.getMessage()));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.fail(401, "登录状态已失效，请重新登录"));
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<Result<Void>> handleNoPermission(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.fail(403, "没有访问该资源的权限"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "服务器内部错误，请稍后重试"));
    }
}
