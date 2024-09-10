package com.example.reverseproxy.service;

import com.example.reverseproxy.entity.Context;
import com.example.reverseproxy.entity.Log;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Autowired
    private ContextService ctxService;

    private final static Logger logger = LogManager.getLogger(ProxyService.class);

    public ResponseEntity<String> processProxyRequest(HttpServletRequest request, HttpServletResponse response){

        Log log = new Log();
        log.setUrl(StringUtils.replaceOnce(request.getRequestURI(), request.getContextPath(), ""));
        log.setStartTime(LocalDateTime.now());

        try {
            HttpHeaders headers = this.getHeaders(request);
//            headers.set("service","ekyc");
            Context ctx=null;
            if(headers.containsKey("service")){
                String service= Objects.requireNonNull(headers.get("service")).get(0);
                ctx =ctxService.getContextByServiceName(service);
            }
            if (ctx==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Hello WORLD >> 404 <<");

            URI uri = this.uriBuilder(request, ctx);

            HttpEntity<String> httpEntity = new HttpEntity<>(this.getBody(request), headers);
            ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
            RestTemplate restTemplate = new RestTemplate(factory);

            log.setCtx(ctx);
            ResponseEntity<String> serverResponse = restTemplate.exchange(uri, Objects.requireNonNull(HttpMethod.resolve(request.getMethod())), httpEntity, String.class);
            log.setEndTime(LocalDateTime.now());
            log.setUrlBodySize(Objects.requireNonNull(serverResponse.getBody()).getBytes(StandardCharsets.UTF_8).length);
            log.setHttpStatus(serverResponse.getStatusCode().value());
            logService.createLog(log);
            return serverResponse;
        } catch (HttpStatusCodeException e) {
            logger.error(e.getMessage());
            log.setHttpStatus(e.getStatusCode().value());
            log.setResponseErrorMessage(e.getMessage());
            logService.createLog(log);
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
    URI uriBuilder(HttpServletRequest request, Context ctx){
        //log if required in this line
        URI uri = null;
        try {
            uri = new URI(ctx.getScheme(),
                    request.getRemoteUser(),
                    ctx.getHost(),
                    ctx.getPort(),
                    StringUtils.replaceOnce(request.getRequestURI(), request.getContextPath(), ""),
                    request.getQueryString(),
                    null);
            // replacing context path form urI to match actual gateway URI
//            uri = UriComponentsBuilder.
//                    .path()
//                    .query()
//                    .build(true)
//                    .toUri();
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
