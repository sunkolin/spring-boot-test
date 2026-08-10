package com.starfish.test.config;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class XxlJobExecutorController {

    private final XxlJobNoNettyExecutor executor;

    private static final String ACCESS_TOKEN_HEADER = "XXL-JOB-ACCESS-TOKEN";

    public XxlJobExecutorController(XxlJobNoNettyExecutor executor) {
        this.executor = executor;
    }

    @PostMapping(value = "/beat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> beat(@RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) String accessTokenReq) {
        if (!validateToken(accessTokenReq)) {
            return jsonResponse(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong."));
        }
        ExecutorBiz executorBiz = executor.getExecutorBiz();
        return jsonResponse(executorBiz.beat());
    }

    @PostMapping(value = "/idleBeat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> idleBeat(@RequestBody String requestBody,
                                            @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) String accessTokenReq) {
        if (!validateToken(accessTokenReq)) {
            return jsonResponse(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong."));
        }
        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestBody, IdleBeatParam.class);
        ExecutorBiz executorBiz = executor.getExecutorBiz();
        return jsonResponse(executorBiz.idleBeat(idleBeatParam));
    }

    @PostMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> run(@RequestBody String requestBody,
                                      @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) String accessTokenReq) {
        if (!validateToken(accessTokenReq)) {
            return jsonResponse(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong."));
        }
        TriggerParam triggerParam = GsonTool.fromJson(requestBody, TriggerParam.class);
        ExecutorBiz executorBiz = executor.getExecutorBiz();
        return jsonResponse(executorBiz.run(triggerParam));
    }

    @PostMapping(value = "/kill", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> kill(@RequestBody String requestBody,
                                       @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) String accessTokenReq) {
        if (!validateToken(accessTokenReq)) {
            return jsonResponse(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong."));
        }
        KillParam killParam = GsonTool.fromJson(requestBody, KillParam.class);
        ExecutorBiz executorBiz = executor.getExecutorBiz();
        return jsonResponse(executorBiz.kill(killParam));
    }

    @PostMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> log(@RequestBody String requestBody,
                                      @RequestHeader(value = ACCESS_TOKEN_HEADER, required = false) String accessTokenReq) {
        if (!validateToken(accessTokenReq)) {
            return jsonResponse(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong."));
        }
        LogParam logParam = GsonTool.fromJson(requestBody, LogParam.class);
        ExecutorBiz executorBiz = executor.getExecutorBiz();
        ReturnT<LogResult> result = executorBiz.log(logParam);
        return jsonResponse(result);
    }

    private boolean validateToken(String accessTokenReq) {
        String accessToken = executor.getAccessToken();
        if (accessToken == null || accessToken.trim().length() == 0) {
            return true;
        }
        return accessToken.equals(accessTokenReq);
    }

    private ResponseEntity<String> jsonResponse(ReturnT<?> returnT) {
        String json = GsonTool.toJson(returnT);
        return ResponseEntity.ok(json);
    }
}