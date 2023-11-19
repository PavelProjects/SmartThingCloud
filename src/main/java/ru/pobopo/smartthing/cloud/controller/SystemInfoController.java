package ru.pobopo.smartthing.cloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.SmartThingCloudApplication;
import ru.pobopo.smartthing.cloud.annotation.RequiredRole;

import static ru.pobopo.smartthing.cloud.model.Role.Constants.ADMIN;

@RestController
@RequestMapping("/")
public class SystemInfoController {

    @RequiredRole(roles = ADMIN)
    @GetMapping("/heath")
    public void health() {}

    @RequiredRole(roles = ADMIN)
    @GetMapping("/version")
    public String version() {
        return SmartThingCloudApplication.VERSION;
    }

}
