package com.example.reverseproxy.controller;


import com.example.reverseproxy.entity.Context;
import com.example.reverseproxy.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class ContextController {

    @Autowired
    private ContextService service;
    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/addContext")
    public Context addContext(@RequestBody Context context){
        return service.saveContext(context);
    }
}
