package com.example.reverseproxy.controller;

import com.example.reverseproxy.service.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
public class ReverseProxyController {
    @Autowired
    ProxyService service;
    @RequestMapping("/**")
    public ResponseEntity<String> sendRequestToSPM(HttpServletRequest request, HttpServletResponse response) {
        return service.processProxyRequest(request,response);
    }
}
