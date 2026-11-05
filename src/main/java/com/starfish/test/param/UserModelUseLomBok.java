package com.starfish.test.param;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserModelUseLomBok {//implements Serializable {

    /**
     * 主键
     */
    private Long id;

    @JsonProperty("tid")
//    @JsonAlias({"tid","tId"})
    private String tId;

    @SneakyThrows
    public static void main(String[] args) {
        String s = "{\"tid\":\"abc\"}";
        System.out.println(new ObjectMapper().readValue(s,UserModelUseLomBok.class));
    }

    /**
     * 驼峰格式
     */
    private String username;

    /**
     * 首字母小写，第二个字母小写
     */
    private String aname;

    /**
     * 首字母小写，第二个字母大写，TODO，此种使用方法，lombok和idea自动生成的get set方法不一样
     */
    private String bName;

    /**
     * 首字母大写，第二个字母小写
     */
    private String Cname;

    /**
     * 首字母大写，第二个字母大写
     */
    private String DName;

    /**
     * 首字母小写，连续两个大写
     */
    private String eNName;

    private String isName;

    /**
     * 首字母大写，连续3个大写
     */
    private String FNName;

    private Date createTime;

    private Date updateTime;

}