package com.cxb.sso.passport.config;

public class PassportConfig {

    /**
     * token的cookie的有效期，单位为秒
     */
    public static final int TOKEN_TIMEOUT = 1800;

    /**
     * token的cookie的名字
     */
    public static final String COOKIE_NAME = "token";

    /**
     * des key
     */
    public static final String SECRET_KEY = "3dqYp97xiBni5geNpvcCW293";
}
