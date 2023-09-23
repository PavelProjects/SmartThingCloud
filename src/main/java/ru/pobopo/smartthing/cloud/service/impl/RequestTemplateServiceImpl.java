package ru.pobopo.smartthing.cloud.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.naming.AuthenticationException;
import javax.validation.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.webjars.NotFoundException;
import ru.pobopo.smartthing.cloud.dto.RequestTemplateDto;
import ru.pobopo.smartthing.cloud.entity.RequestTemplateEntity;
import ru.pobopo.smartthing.cloud.entity.UserEntity;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.mapper.RequestTemplateMapper;
import ru.pobopo.smartthing.cloud.repository.RequestTemplateRepository;
import ru.pobopo.smartthing.cloud.repository.UserRepository;
import ru.pobopo.smartthing.cloud.service.RequestTemplateService;

@Component
public class RequestTemplateServiceImpl implements RequestTemplateService {
    private final RequestTemplateMapper requestTemplateMapper;
    private final RequestTemplateRepository requestTemplateRepository;
    private final UserRepository userRepository;

    @Autowired
    public RequestTemplateServiceImpl(
        RequestTemplateMapper requestTemplateMapper,
        RequestTemplateRepository requestTemplateRepository,
        UserRepository userRepository
    ) {
        this.requestTemplateMapper = requestTemplateMapper;
        this.requestTemplateRepository = requestTemplateRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<RequestTemplateEntity> getRequestTemplates() throws AuthenticationException {
        return requestTemplateRepository.findByOwnerOrOwnerIsNull(getCurrentUser());
    }

    @Override
    public RequestTemplateEntity createRequestTemplate(RequestTemplateDto requestTemplateDto)
        throws AuthenticationException {
        if (requestTemplateDto == null) {
            throw new ValidationException("Dto can't be empty!");
        }
        if (StringUtils.isBlank(requestTemplateDto.getPath())) {
            throw new ValidationException("Path can't be empty!");
        }
        if (StringUtils.isBlank(requestTemplateDto.getMethod())) {
            throw new ValidationException("Method can't be empty!");
        }
        if (StringUtils.isBlank(requestTemplateDto.getName())) {
            requestTemplateDto.setName(requestTemplateDto.getPath());
        }

        RequestTemplateEntity entity = requestTemplateMapper.toEntity(requestTemplateDto);
        entity.setOwner(getCurrentUser());
        requestTemplateRepository.save(entity);
        return entity;
    }

    @Override
    public void updateRequestTemplate(RequestTemplateDto dto) throws AccessDeniedException, AuthenticationException {
        RequestTemplateEntity entity = getRequestTemplateWithRoleCheck(dto.getId());
        if (StringUtils.isBlank(dto.getPath())) {
            throw new ValidationException("Path can't be empty!");
        }
        if (StringUtils.isBlank(dto.getMethod())) {
            throw new ValidationException("Method can't be empty!");
        }

        entity.setName(StringUtils.isBlank(dto.getName()) ? dto.getPath() : dto.getName());
        entity.setPath(dto.getPath());
        entity.setMethod(dto.getMethod());
        entity.setPayload(dto.getPayload());
        entity.setSupportedVersion(dto.getSupportedVersion());
        requestTemplateRepository.save(entity);
    }

    @Override
    public void deleteRequestTemplate(String id) throws AuthenticationException, AccessDeniedException {
        requestTemplateRepository.delete(getRequestTemplateWithRoleCheck(id));
    }

    private RequestTemplateEntity getRequestTemplateWithRoleCheck(String id) throws AuthenticationException, AccessDeniedException {
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Template id is missing!");
        }

        Optional<RequestTemplateEntity> entity = requestTemplateRepository.findById(id);
        if (entity.isEmpty()) {
            throw new NotFoundException("There is no request template with id " + id);
        }

        if (!AuthoritiesService.canManageRequestTemplate(entity.get())) {
            throw new AccessDeniedException("Current user can't manage this template!");
        }
        return entity.get();
    }

    private UserEntity getCurrentUser() throws AuthenticationException {
        UserEntity user = userRepository.findByLogin(AuthoritiesService.getCurrentUserLogin());
        Objects.requireNonNull(user, "Can't find user " + AuthoritiesService.getCurrentUserLogin());
        return user;
    }
}
