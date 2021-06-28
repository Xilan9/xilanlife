package com.tanhua.sso.controller;

import com.tanhua.sso.service.UserService;
import com.tanhua.sso.vo.ErrorResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param param
     * @return
     */
    @PostMapping("loginVerification")
    public ResponseEntity<Object>login(@RequestBody Map<String,String> param){
        try {
            String phone=param.get("phone");
            String code = param.get("verificationCode");
            String data =this.userService.login(phone,code);
            if (StringUtils.isNotEmpty(data)){
                //登录成功
                Map<String,Object> result=new HashMap<>(2);
                String[] ss = StringUtils.split(data, '|');
                result.put("token",ss[0]);
                result.put("isNew",Boolean.valueOf(ss[1]));
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000002").errMessage("登录失败").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);

    }
}
