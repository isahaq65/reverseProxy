package com.example.reverseproxy.service;

import com.example.reverseproxy.entity.Context;
import com.example.reverseproxy.repository.ContextRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class ContextService {
    @Autowired
    private ContextRepository repository;


    public Context saveContext(Context context){
       return repository.save(context);
    }


    public  Context getContextByServiceName(String serviceName){
        return repository.findByServiceName(serviceName);
    }
}
