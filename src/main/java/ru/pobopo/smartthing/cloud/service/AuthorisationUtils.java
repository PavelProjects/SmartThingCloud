package ru.pobopo.smartthing.cloud.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ru.pobopo.smartthing.cloud.entity.GatewayEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.model.AuthorizedUser;
import ru.pobopo.smartthing.cloud.model.Role;

import javax.naming.AuthenticationException;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthorisationUtils {
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
        return isSameUser(authentication, gatewayEntity.getOwner()) ||
                checkAuthority(authentication, List.of(Role.ADMIN.getName(), GATEWAY_ADMIN_ROLE));
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

    private static boolean isSameUser(AuthorizedUser authentication, UserEntity user) {
        return authentication.getUser().equals(user);
    }

    @NotNull
    public static AuthorizedUser getAuthorizedUser() throws AuthenticationException {
        AuthorizedUser currentUser = (AuthorizedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentUser == null || currentUser.getUser() == null) {
            throw new AuthenticationException("Current user didn't authenticate!");
        }
        return currentUser;
    }
}
