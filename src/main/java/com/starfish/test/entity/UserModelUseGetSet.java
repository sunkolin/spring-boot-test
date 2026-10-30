package com.starfish.test.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 基于idea生成get set
 */
public class UserModelUseGetSet implements Serializable {

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
     * 首字母小写，第二个字母大写
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

    /**
     * 首字母大写，连续3个大写
     */
    private boolean FNName;

    private boolean isName;

    private boolean iSCar;

    private boolean card;


    public boolean isId() {
        return id;
    }

    public void setId(boolean id) {
        this.id = id;
    }

    public boolean isUsername() {
        return username;
    }

    public void setUsername(boolean username) {
        this.username = username;
    }

    public boolean isAname() {
        return aname;
    }

    public void setAname(boolean aname) {
        this.aname = aname;
    }

    public boolean isbName() {
        return bName;
    }

    public void setbName(boolean bName) {
        this.bName = bName;
    }

    public boolean isCname() {
        return Cname;
    }

    public void setCname(boolean cname) {
        Cname = cname;
    }

    public boolean isDName() {
        return DName;
    }

    public void setDName(boolean DName) {
        this.DName = DName;
    }

    public boolean iseNName() {
        return eNName;
    }

    public void seteNName(boolean eNName) {
        this.eNName = eNName;
    }

    public boolean isFNName() {
        return FNName;
    }

    public void setFNName(boolean FNName) {
        this.FNName = FNName;
    }

    public boolean isName() {
        return isName;
    }

    public void setName(boolean name) {
        isName = name;
    }


    @JsonProperty("bName3")
    private boolean bName2;


//    public boolean getBName2() {
//        return bName2;
//    }
//
//    public void setBName2(boolean bName2) {
//        this.bName2 = bName2;
//    }


    public boolean isISCar() {
        return iSCar;
    }

    public void setiSCar(boolean iSCar) {
        this.iSCar = iSCar;
    }

    public boolean isbName2() {
        return bName2;
    }

    public void setbName2(boolean bName2) {
        this.bName2 = bName2;
    }

    public boolean isCard() {
        return card;
    }

    public void setCard(boolean card) {
        this.card = card;
    }

}
