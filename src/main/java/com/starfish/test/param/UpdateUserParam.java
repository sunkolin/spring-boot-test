package com.starfish.test.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UpdateUserParam implements Serializable {

    private Long id;

    private String mobile;

    private String password;

    private String nickName;

    private String username;

    private String fullName;

    private String email;

    private Integer sex;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date birthday;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

}