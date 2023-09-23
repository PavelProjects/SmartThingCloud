package ru.pobopo.smartthing.cloud.service.impl;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;

@Service
public class AuthoritiesService {
    /**
     * Возвращает логин текущего авторизованного юзера
     * @return логин пользователя
     * @throws AuthenticationException - если текущий юзер каким-то образом был не авторизован
     */

    @NotNull
    public static String getCurrentUserLogin() throws AuthenticationException {
        return getCurrentUser().getName();
    }

    public static boolean canManageGateway(GatewayEntity gatewayEntity) throws AuthenticationException {
        if (gatewayEntity == null) {
            return false;
        }
        return StringUtils.equals(getCurrentUserLogin(), gatewayEntity.getOwner().getLogin());
    }

    public static boolean canManageRequestTemplate(RequestTemplateEntity entity) throws AuthenticationException {
        if (entity == null || entity.getOwner() == null) {
            return false;
        }
        return StringUtils.equals(getCurrentUserLogin(), entity.getOwner().getLogin());
    }

    /**
     * Проверяет наличие прав у текущего юзера
     * @param authorities - набор прав, которые должны быть у юзера для получения доступа
     * @return
     * @throws AuthenticationException
     */
    public static boolean checkAuthority(Collection<String> authorities) throws AuthenticationException {
        Collection<? extends GrantedAuthority> userAuthorities = getCurrentUser().getAuthorities();
        if (userAuthorities == null || userAuthorities.isEmpty() || userAuthorities.size() < authorities.size()) {
            return false;
        }
        return authorities.containsAll(userAuthorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
    }

    private static boolean isSameUser(String userLogin) throws AuthenticationException {
        return StringUtils.equals(userLogin, getCurrentUser().getName());
    }

    private static Authentication getCurrentUser() throws AuthenticationException {
        Authentication currentUser = SecurityContextHolder.getContext().getAuthentication();
        if (currentUser == null || StringUtils.isBlank(currentUser.getName())) {
            throw new AuthenticationException("Current user didn't authenticate!");
        }
        return currentUser;
    }
}
