package com.tanhua.sso.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.sso.enums.SexEnum;
import com.tanhua.sso.mapper.UserInfoMapper;
import com.tanhua.sso.pojo.User;
import com.tanhua.sso.pojo.UserInfo;
import com.tanhua.sso.vo.PicUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class UserInfoService {

    @Autowired
    private UserService userService;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private FaceEngineService faceEngineService;
    @Autowired
    private PicUploadService picUploadService;
    public Boolean saveUserInfo(Map<String, String> param, String token) {
       User user= this.userService.queryUserByToken(token);
       if (null==user){
           return false;
       }
        UserInfo userInfo=new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setSex(StringUtils.equalsIgnoreCase(param.get("gender"), "man") ? SexEnum.MAN : SexEnum.WOMAN);
        userInfo.setNickName(param.get("nickname"));
        userInfo.setBirthday(param.get("birthday"));
        userInfo.setCity(param.get("city"));

        return this.userInfoMapper.insert(userInfo) == 1;
    }

    public Boolean saveUserLogo(MultipartFile file, String token) {
        User user = this.userService.queryUserByToken(token);
        if (null == user){
            return false;
        }
        try {
            boolean b = this.faceEngineService.checkIsPortrait(file.getBytes());
            if (!b){
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        PicUploadResult result = this.picUploadService.upload(file);
        if (StringUtils.isEmpty(result.getName())){
            return false;
        }
        UserInfo userInfo=new UserInfo();
        userInfo.setLogo(result.getName());
        QueryWrapper<UserInfo>queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",user.getId());
        return this.userInfoMapper.update(userInfo,queryWrapper) == 1;
    }
}
