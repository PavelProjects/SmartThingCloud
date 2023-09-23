package ru.pobopo.smartthing.cloud.service;

import java.util.List;
import javax.naming.AuthenticationException;
import org.springframework.stereotype.Component;
import ru.pobopo.smartthing.cloud.dto.RequestTemplateDto;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;

@Component
public interface RequestTemplateService {
    List<RequestTemplateEntity> getRequestTemplates() throws AuthenticationException;
    RequestTemplateEntity createRequestTemplate(RequestTemplateDto requestTemplateDto) throws AuthenticationException;
    void updateRequestTemplate(RequestTemplateDto dto) throws AccessDeniedException, AuthenticationException;
    void deleteRequestTemplate(String id) throws AuthenticationException, AccessDeniedException;
}
