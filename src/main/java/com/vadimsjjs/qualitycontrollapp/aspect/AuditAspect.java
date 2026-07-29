package com.vadimsjjs.qualitycontrollapp.aspect;

import com.vadimsjjs.qualitycontrollapp.service.AuditService;
import com.vadimsjjs.qualitycontrollapp.service.NonconformingProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("execution(* com.vadimsjjs.qualitycontrollapp.service.NonconformingProductService.create(..))")
    public Object logCreate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        Long id = getId(result);
        if (id != null) {
            auditService.log("CREATE", "NONCONFORMING_PRODUCT", id, null, result);
        }
        return result;
    }

    @Around("execution(* com.vadimsjjs.qualitycontrollapp.service.NonconformingProductService.update(..))")
    public Object logUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = (Long) joinPoint.getArgs()[0];
        Object oldValue = getOldValue(id);
        Object result = joinPoint.proceed();
        auditService.log("UPDATE", "NONCONFORMING_PRODUCT", id, oldValue, result);
        return result;
    }

    @Around("execution(* com.vadimsjjs.qualitycontrollapp.service.NonconformingProductService.delete(..))")
    public Object logDelete(ProceedingJoinPoint joinPoint) throws Throwable {
        Long id = (Long) joinPoint.getArgs()[0];
        Object oldValue = getOldValue(id);
        Object result = joinPoint.proceed();
        auditService.log("DELETE", "NONCONFORMING_PRODUCT", id, oldValue, null);
        return result;
    }

    @Around("execution(* com.vadimsjjs.qualitycontrollapp.service.AuthService.login(..))")
    public Object logLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Object arg = joinPoint.getArgs()[0];
            Method method = arg.getClass().getMethod("getPersonalNo");
            Long personalNo = (Long) method.invoke(arg);
            auditService.logLogin(personalNo, null, null);
        } catch (Exception e) {
            log.error("Failed to log login: {}", e.getMessage());
        }
        return result;
    }

    @Around("execution(* com.vadimsjjs.qualitycontrollapp.service.AuthService.logout(..))")
    public Object logLogout(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            Long personalNo = getCurrentUser();
            if (personalNo != null) {
                auditService.logLogout(personalNo);
            }
        } catch (Exception e) {
            log.error("Failed to log logout: {}", e.getMessage());
        }
        return result;
    }

    private Long getId(Object obj) {
        if (obj == null) return null;
        try {
            Method method = obj.getClass().getMethod("getId");
            return (Long) method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getOldValue(Long id) {
        try {
            Method method = NonconformingProductService.class.getMethod("findById", Long.class);
            return method.invoke(getTargetObject(), id);
        } catch (Exception e) {
            return null;
        }
    }

    private Object getTargetObject() {
        return null;
    }

    private Long getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            return Long.parseLong(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}