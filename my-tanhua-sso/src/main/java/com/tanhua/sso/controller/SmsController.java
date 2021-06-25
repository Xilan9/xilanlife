package com.tanhua.sso.controller;
import com.tanhua.sso.service.SmsService;
import com.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class SmsController {

    @Autowired
    private SmsService smsService;
    /**
     * 发送短信验证码接口
     * @param param
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<ErrorResult>sendCheckCode(@RequestBody Map<String,String>param){
        ErrorResult errorResult=null;
        String phone = param.get("phone");
        try {

            errorResult = this.smsService.sendCheckCode(phone);
            if (null==errorResult){
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            log.error("发送短信验证码失败--手机号码="+phone,e);

        }
        errorResult= ErrorResult.builder().errCode("000002").errMessage("短信验证码发送失败").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }
}
