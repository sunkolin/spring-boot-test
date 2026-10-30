package com.starfish.test.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserModelUseLomBok implements Serializable {

    /**
     * 主键
     */
    private boolean id;

    /**
     * 驼峰格式
     */
    private boolean username;

    /**
     * 首字母小写，第二个字母小写
     */
    private boolean aname;

    /**
     * 首字母小写，第二个字母大写，TODO，此种使用方法，lombok和idea自动生成的get set方法不一样
     */
    private boolean bName;

    /**
     * 首字母大写，第二个字母小写
     */
    private boolean Cname;

    /**
     * 首字母大写，第二个字母大写
     */
    private boolean DName;

    /**
     * 首字母小写，连续两个大写
     */
    private boolean eNName;

    private boolean isName;

    /**
     * 首字母大写，连续3个大写
     */
    private boolean FNName;

    private boolean iSCar;

    private boolean tSql;

}