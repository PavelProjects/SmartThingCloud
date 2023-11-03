package ru.pobopo.smartthing.cloud.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.pobopo.smartthing.cloud.SmartThingCloudApplication;

@CrossOrigin
@RestController
@RequestMapping("/")
public class SystemInfoController {

    @GetMapping("/heath")
    public void health() {}

    @GetMapping("/version")
    public String version() {
        return SmartThingCloudApplication.VERSION;
    }

}
