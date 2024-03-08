package ru.pobopo.smartthing.cloud.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;
import ru.pobopo.smartthing.cloud.model.Role;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Slf4j
@Component
public class RequiredRoleAspect {
    @Pointcut("within(ru.pobopo.smartthing.cloud.controller.*)")
    public void inPackage(){}

    @Around("inPackage()")
    public Object roleCheck(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();

        if (targetMethod.isAnnotationPresent(RequiredRole.class)) {
            checkRoles(targetMethod.getAnnotation(RequiredRole.class));
        } else {
            log.warn(
                    "Method {}@{} is missing RequiredRole annotation!",
                    targetMethod.getDeclaringClass().getName(),
                    targetMethod.getName()
            );
        }

        return joinPoint.proceed();
    }

    private void checkRoles(RequiredRole annotation) throws AccessDeniedException {
        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() == null) {
            String message = "There is no authenticated user!";
            log.info(message);
            throw new AccessDeniedException(message);
        }
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Set<String> authorities = authenticatedUser.getAuthorities().stream().map(SimpleGrantedAuthority::getAuthority).collect(Collectors.toSet());

        if (authorities.contains(Role.ADMIN.getName())) {
            return;
        }

        if (Arrays.stream(annotation.roles()).anyMatch(authorities::contains)) {
            return;
        }

        String message = "Required roles: " + Arrays.toString(annotation.roles());
        log.info(message);
        throw new AccessDeniedException(message);
    }
}
