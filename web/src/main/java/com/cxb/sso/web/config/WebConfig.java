package com.cxb.sso.web.config;

public class WebConfig {

    /**
     * token的cookie的有效期，单位为分钟
     */
    public static final int TOKEN_TIMEOUT = 30;

    /**
     * token的cookie的名字
     */
    public static final String COOKIE_NAME = "token";

    /**
     * des key
     */
    public static final String SECRET_KEY = "3dqYp97xiBni5geNpvcCW293";

    /**
     * 认证系统url
     */
    public static final String SSO_SERVICE = "http://passport.com:8888/passport/";
}
