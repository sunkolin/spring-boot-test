package com.starfish.test.controller;

import com.starfish.common.storage.RustFSProperties;
import com.starfish.common.storage.RustFSService;
import com.starfish.core.annotation.RequireLogin;
import com.starfish.core.exception.CustomException;
import com.starfish.core.model.Result;
import com.starfish.test.enumeration.ResultEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Slf4j
@Tag(name = "FileController", description = "文件接口")
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Resource
    private RustFSService rustFSService;

    @Resource
    private RustFSProperties rustFSProperties;

    @RequireLogin(false)
    @Operation(summary = "图片上传", description = "上传图片到MinIO，返回公开访问地址")
    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ResultEnum.PARAM_ERROR);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException(ResultEnum.PARAM_ERROR);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String dateFolder = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomPart = String.format("%07d", new Random().nextInt(10000000));
        String objectName = "images/" + dateFolder + "/" + timestamp + randomPart + extension;

        rustFSService.uploadFile(file, objectName);

        String publicUrl = buildPublicUrl(objectName);

        log.info("FileController uploadImage success.objectName={}, publicUrl={}", objectName, publicUrl);

        Map<String, String> result = new HashMap<>();
        result.put("url", publicUrl);
        result.put("objectName", objectName);
        return Result.success(result);
    }

    private String buildPublicUrl(String objectName) {
        String endpoint = rustFSProperties.getEndpoint();
        String bucketName = rustFSProperties.getBucketName();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }

}