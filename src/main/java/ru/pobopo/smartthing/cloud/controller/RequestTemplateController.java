package ru.pobopo.smartthing.cloud.controller;

import java.util.List;
import javax.naming.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.dto.RequestTemplateDto;
import ru.pobopo.smartthing.cloud.exception.AccessDeniedException;
import ru.pobopo.smartthing.cloud.mapper.RequestTemplateMapper;
import ru.pobopo.smartthing.cloud.service.RequestTemplateService;

@RestController
@RequestMapping("/gateway/request/template")
public class RequestTemplateController {
    private final RequestTemplateService requestTemplateService;
    private final RequestTemplateMapper requestTemplateMapper;

    @Autowired
    public RequestTemplateController(RequestTemplateService requestTemplateService,
        RequestTemplateMapper requestTemplateMapper
    ) {
        this.requestTemplateService = requestTemplateService;
        this.requestTemplateMapper = requestTemplateMapper;
    }

    @GetMapping("/list")
    public List<RequestTemplateDto> getTemplatesList() throws AuthenticationException {
        return requestTemplateMapper.toDto(requestTemplateService.getRequestTemplates());
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestTemplateDto createRequestTemplate(@RequestBody RequestTemplateDto dto) throws AuthenticationException {
        return requestTemplateMapper.toDto(requestTemplateService.createRequestTemplate(dto));
    }

    @PutMapping("/update")
    public void updateRequestTemplate(@RequestBody RequestTemplateDto dto)
        throws AuthenticationException, AccessDeniedException {
        requestTemplateService.updateRequestTemplate(dto);
    }

    @DeleteMapping("/delete")
    public void deleteRequestTemplate(@RequestParam String id) throws AccessDeniedException, AuthenticationException {
        requestTemplateService.deleteRequestTemplate(id);
    }
}
