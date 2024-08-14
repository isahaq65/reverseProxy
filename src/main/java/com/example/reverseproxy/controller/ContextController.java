package com.example.reverseproxy.controller;


import com.example.reverseproxy.entity.Context;
import com.example.reverseproxy.service.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/context")
    public  Object getContext(@RequestHeader("service-name") String serviceName , @RequestBody Object body){
        Context context= service.getContextByServiceName(serviceName);
        String url=context.getUrl();

        HttpHeaders headers= new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Object> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Object> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                Object.class
        );

        return responseEntity.getBody();
    }
}
