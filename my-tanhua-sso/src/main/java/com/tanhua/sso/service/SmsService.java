package com.tanhua.sso.service;//接口类型：互亿无线触发短信接口，支持发送验证码短信、订单通知短信等。
// 账户注册：请通过该地址开通账户http://user.ihuyi.com/register.html
// 注意事项：
//（1）调试期间，请使用用系统默认的短信内容：您的验证码是：【变量】。请不要把验证码泄露给其他人。
//（2）请使用 APIID 及 APIKEY来调用接口，可在会员中心获取；
//（3）该代码仅供接入互亿无线短信接口参考使用，客户可根据实际需要自行编写；

import com.tanhua.sso.config.LoginConfig;
import com.tanhua.sso.vo.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class SmsService {

    @Autowired
    private LoginConfig loginConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    private static String Url = "http://106.ihuyi.com/webservice/sms.php?method=Submit";

    public String SendSms(String mobile) {

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(Url);

        client.getParams().setContentCharset("GBK");
        method.setRequestHeader("ContentType", "application/x-www-form-urlencoded;charset=GBK");

        String mobile_code = RandomUtils.nextInt(100000, 999999) + "";

        String content = new String("您的验证码是：" + mobile_code + "。请不要把验证码泄露给其他人。");

        NameValuePair[] data = {//提交短信
                new NameValuePair("account", loginConfig.getAccount()), //查看用户名 登录用户中心->验证码通知短信>产品总览->API接口信息->APIID
                new NameValuePair("password", loginConfig.getPassword()), //查看密码 登录用户中心->验证码通知短信>产品总览->API接口信息->APIKEY
                //new NameValuePair("password", util.StringUtil.MD5Encode("密码")),
                new NameValuePair("mobile", mobile),
                new NameValuePair("content", content),
        };
        method.setRequestBody(data);

        try {
            client.executeMethod(method);

            String SubmitResult = method.getResponseBodyAsString();


            Document doc = DocumentHelper.parseText(SubmitResult);
            Element root = doc.getRootElement();

            String code = root.elementText("code");
            String msg = root.elementText("msg");
            String smsid = root.elementText("smsid");

//			System.out.println(code);
//			System.out.println(msg);
//			System.out.println(smsid);

            if ("2".equals(code)) {
                System.out.println("短信提交成功1234556");
                return mobile_code;
            }

        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }


    /**
     * 发送短信验证码
     * 实现：发送完成短信验证码时需要把验证码保存到redis中
     *
     * @param phone
     * @return
     */
    public ErrorResult sendCheckCode(String phone) {

        //设置key
        String redisKey = "CHECK_CODE_" + phone;
        //判断短信验证码是否失效
        if (this.redisTemplate.hasKey(redisKey)) {
            String msg = "上一次发送的验证码还未失效!";
            return ErrorResult.builder().errCode("000001").errMessage(msg).build();
        }

        //String code = this.SendSms(phone);
        String code = "123456";
        if (StringUtils.isEmpty(code)) {
            String msg = "发送短信验证码失败";
            return ErrorResult.builder().errCode("000000").errMessage(msg).build();
        }
        //短信保存到redis中有效期为5分钟
        this.redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(5));
        return null;
    }

}
