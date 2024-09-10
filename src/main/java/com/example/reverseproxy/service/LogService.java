package com.example.reverseproxy.service;

import com.example.reverseproxy.entity.Log;
import com.example.reverseproxy.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogService {

    @Autowired
    private LogRepository logRepository;
    @Autowired
    private ContextService contextService;
    public Log createLog(Log logData) {
        Log log = new Log();
        log.setUrl(logData.getUrl());
        log.setUrlBodySize(logData.getUrlBodySize());
        log.setStartTime(logData.getStartTime());
        log.setEndTime(log.getEndTime());
        log.setCtx(this.contextService.getContextByServiceName(logData.getCtx().getServiceName()));
        log.setHttpStatus(logData.getHttpStatus());
        log.setResponseErrorMessage(logData.getResponseErrorMessage());
        return logRepository.save(log);   
    }
}
