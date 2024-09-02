package com.example.reverseproxy.service;

import com.example.reverseproxy.entity.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Objects;


@Service
public class ProxyService {
    String domain = "192.168.10.171";

    @Autowired
    private LogService logService;

    private ContextService contextService;
    private final static Logger logger = LogManager.getLogger(ProxyService.class);
    @Retryable(exclude = {
            HttpStatusCodeException.class}, include = Exception.class, backoff = @Backoff(delay = 5000, multiplier = 4.0), maxAttempts = 4)
    public ResponseEntity<String> processProxyRequest(HttpServletRequest request, HttpServletResponse response){

//        ThreadContext.put("traceId", traceId);
        //no service name = 404, otherwise forward
        Log newLog = new Log();
        newLog.setUrl(StringUtils.replaceOnce(request.getRequestURI(), request.getContextPath(), ""));
        newLog.setStartTime(LocalDateTime.now());

        URI uri = this.uriBuilder(request);
        HttpHeaders headers = this.getHeaders(request);
        HttpEntity<String> httpEntity = new HttpEntity<>(this.getBody(request), headers);
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        try {

            newLog.setContextObject(contextService.getContextByServiceName(headers.get("service").toString()));
            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, Objects.requireNonNull(HttpMethod.valueOf(request.getMethod())), httpEntity, String.class);


            newLog.setEndTime(LocalDateTime.now());
            newLog.setUrlBodySize(serverResponse.getBody().getBytes(StandardCharsets.UTF_8).length);

            logService.createLog(newLog);
            HttpHeaders responseHeaders = new HttpHeaders();
//            responseHeaders.put(HttpHeaders.CONTENT_TYPE, Objects.requireNonNull(serverResponse.getHeaders().get(HttpHeaders.CONTENT_TYPE)));
//            logger.info(serverResponse);
            return serverResponse;
        } catch (HttpStatusCodeException e) {
            logger.error(e.getMessage());
            return ResponseEntity.status(e.getRawStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        }
    }
    @Recover
    public ResponseEntity<String> recoverFromRestClientErrors(Exception e, String body, HttpMethod method, HttpServletRequest request, HttpServletResponse response, String traceId) {
        logger.error("retry method for the following url " + request.getRequestURI() + " has failed" + e.getMessage());
        logger.error(e.getStackTrace());
        throw new RuntimeException("There was an error trying to process you request. Please try again later");
    }
    HttpHeaders getHeaders(HttpServletRequest request){
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.set(headerName, request.getHeader(headerName));
        }
        return headers;
    }
    URI uriBuilder(HttpServletRequest request){
        //log if required in this line
        URI uri = null;
        try {
            uri = new URI(request.getScheme(), null, domain, 8504, null, null, null);
            // replacing context path form urI to match actual gateway URI
            uri = UriComponentsBuilder.fromUri(uri)
                    .path(StringUtils.replaceOnce(request.getRequestURI(), request.getContextPath(), ""))
                    .query(request.getQueryString())
                    .build(true)
                    .toUri();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }finally {
            return uri;
        }
    }
    String getBody(HttpServletRequest request){

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
//            throw new RuntimeException(e)
        }finally {
            return sb.toString();
        }
    }
}
