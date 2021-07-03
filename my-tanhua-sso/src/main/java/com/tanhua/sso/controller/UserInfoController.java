package com.tanhua.sso.controller;

import com.tanhua.common.pojo.User;
import com.tanhua.sso.service.UserInfoService;
import com.tanhua.sso.service.UserService;
import com.tanhua.sso.vo.ErrorResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private UserService userService;
    /**
     * 完善个人信息-基本信息
     * @param param
     * @return
     */
    @PostMapping("loginReginfo")
    public ResponseEntity<Object> loginReginfo(@RequestBody Map<String, String> param, @RequestHeader("Authorization") String token) {
        try {
            Boolean bool = this.userInfoService.saveUserInfo(param, token);
            //判断是否为false
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000001").errMessage("保存用户信息失败").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);

    }

    /**
     * 用户的头像
     * @return
     */
    @PostMapping("loginReginfo/head")
    public ResponseEntity<Object> saveUserLogo(@RequestParam("headPhoto") MultipartFile file, @RequestHeader("Authorization") String token) {
        try {
            Boolean bool = this.userInfoService.saveUserLogo(file, token);
            if (bool) {
                return ResponseEntity.ok(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ErrorResult errorResult = ErrorResult.builder().errCode("000001").errMessage("保存用户头像失败").build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
    }

    /**
     * 校验token，根据token查询用户数据
     *
     * @param token
     * @return
     */
    @GetMapping("{token}")
    public User queryUserByToken(@PathVariable("token") String token) {
        return this.userService.queryUserByToken(token);
    }
}
