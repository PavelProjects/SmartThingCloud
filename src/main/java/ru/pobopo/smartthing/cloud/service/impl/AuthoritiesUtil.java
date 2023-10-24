package ru.pobopo.smartthing.cloud.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;

@Service
public class AuthoritiesUtil {
    public static final String USER_ROLE = "user";
    public static final String ADMIN_ROLE = "admin";
    public static final String GATEWAY_ADMIN_ROLE = "gateway_admin";

    /**
     * Возвращает логин текущего авторизованного юзера
     * @return логин пользователя
     * @throws AuthenticationException - если текущий юзер каким-то образом был не авторизован
     */

    @NotNull
    public static UserEntity getCurrentUser() throws AuthenticationException {
        return getAuthorizedUser().getUser();
    }

    /**
     * Проверяет, может ли текущий юзер как либо изменять/управлять маршрутизатором
     * @param gatewayEntity маршртизатор, доступ к которому надо проверить
     * @return true, если доступ есть, false в протимном случае
     * @throws AuthenticationException выкидывается, если в контенксте нет авторизованного пользователя
     */
    public static boolean canManageGateway(GatewayEntity gatewayEntity) throws AuthenticationException {
        if (gatewayEntity == null || gatewayEntity.getOwner() == null) {
            return false;
        }
        AuthorizedUser authentication = getAuthorizedUser();
        return checkAuthority(authentication, List.of(ADMIN_ROLE, GATEWAY_ADMIN_ROLE))
               || isSameUser(authentication, gatewayEntity.getOwner().getLogin());
    }

    /**
     * Проверяет, может ли текущий юзер изменять шаблон запроса
     * @param entity - шаблон запроса, доступ к которому надо проверить
     * @return - true, если доступ есть
     * @throws AuthenticationException выкидывается, если в контенксте нет авторизованного пользователя
     */
    public static boolean canManageRequestTemplate(RequestTemplateEntity entity) throws AuthenticationException {
        if (entity == null || entity.getOwner() == null) {
            return false;
        }
        AuthorizedUser authentication = getAuthorizedUser();
        return checkAuthority(authentication, List.of(ADMIN_ROLE)) || isSameUser(authentication, entity.getOwner().getLogin());
    }

    /**
     * Проверяет наличие прав у юзера
     * @param authorities набор прав, которые должны быть у юзера для получения доступа
     */
    private static boolean checkAuthority(AuthorizedUser authentication, Collection<String> authorities) {
        Collection<? extends GrantedAuthority> userAuthorities = authentication.getAuthorities();
        if (userAuthorities == null || userAuthorities.isEmpty() || userAuthorities.size() < authorities.size()) {
            return false;
        }
        return authorities.containsAll(userAuthorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
    }

    private static boolean isSameUser(AuthorizedUser authentication, String userLogin) {
        return StringUtils.equals(userLogin, authentication.getUser().getLogin());
    }

    private static AuthorizedUser getAuthorizedUser() throws AuthenticationException {
        AuthorizedUser currentUser = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AuthenticationException("Current user didn't authenticate!");
        }
        return currentUser;
    }
}
