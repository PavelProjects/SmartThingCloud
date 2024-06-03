package ru.pobopo.smartthing.cloud.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import ru.pobopo.smartthing.cloud.model.AuthenticatedUser;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@WebFilter(filterName = "RequestLoggingFilter", urlPatterns = "/*")
public class RequestLoggingFilter extends AbstractRequestLoggingFilter {
    private static final String PREFIX = RequestLoggingFilter.class.getName();
    private static final String START_TIME_ATTR = PREFIX + ".http_request_start_time";
    private static final String RESPONSE_ATTR = PREFIX + ".http_response";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(RESPONSE_ATTR, response);
        super.doFilterInternal(request, response, filterChain);
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        log.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long endTime = System.nanoTime();
        String timeInSec = String.format("%.3f", (double) (endTime - startTime) / 1E9);
        log.info("{} in {}sec", message, timeInSec);
    }


    @Override
    protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
        StringBuilder stringBuilder = new StringBuilder();
        HttpServletResponse response = getServletResponse(request);
        if (response != null) {
            stringBuilder.append(", status=").append(response.getStatus());
        }
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (token != null) {
            AuthenticatedUser user = (AuthenticatedUser) token.getPrincipal();
            stringBuilder.append(", user=").append(user);
        }
        stringBuilder.append(suffix);
        return super.createMessage(request, prefix, stringBuilder.toString());
    }

    private HttpServletResponse getServletResponse(HttpServletRequest request) {
        Object response = request.getAttribute(RESPONSE_ATTR);
        if (response instanceof HttpServletResponse) {
            return (HttpServletResponse) response;
        }
        return null;
    }
}
