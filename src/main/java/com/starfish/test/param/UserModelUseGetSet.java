package com.starfish.test.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.util.Date;

/**
 * 基于idea生成get set
 */
public class UserModelUseGetSet implements Serializable {


    @SneakyThrows
    public static void main(String[] args) {
        UserModelUseGetSet u= new UserModelUseGetSet();
        u.setCreateTime(new Date());
        JsonMapper objectMapper = new JsonMapper();
//        objectMapper.writeValueAsString(u);
        System.out.println(objectMapper.writeValueAsString(u));
    }

    /**
     * 主键
     */
    private Long id;



    /**
     * 驼峰格式
     */
    private String username;

    /**
     * 首字母小写，第二个字母小写
     */
    private String aname;

    /**
     * 首字母小写，第二个字母大写
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

    /**
     * 首字母大写，连续3个大写
     */
    private String FNName;

    public String PractCode;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAname() {
        return aname;
    }

    public void setAname(String aname) {
        this.aname = aname;
    }

    public String getbName() {
        return bName;
    }

    public void setbName(String bName) {
        this.bName = bName;
    }

    public String getCname() {
        return Cname;
    }

    public void setCname(String cname) {
        Cname = cname;
    }

    public String getDName() {
        return DName;
    }

    public void setDName(String DName) {
        this.DName = DName;
    }

    public String geteNName() {
        return eNName;
    }

    public void seteNName(String eNName) {
        this.eNName = eNName;
    }

    public String getFNName() {
        return FNName;
    }

    public void setFNName(String FNName) {
        this.FNName = FNName;
    }

    public String getPractCode() {
        return PractCode;
    }

    public void setPractCode(String practCode) {
        PractCode = practCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

}
