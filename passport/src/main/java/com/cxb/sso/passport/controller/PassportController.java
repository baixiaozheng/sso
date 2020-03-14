package com.cxb.sso.passport.controller;

import com.alibaba.fastjson.JSON;
import com.cxb.sso.passport.config.PassportConfig;
import com.cxb.sso.passport.model.User;
import com.cxb.sso.passport.redis.RedisUtil;
import com.cxb.sso.passport.service.UserService;
import com.cxb.sso.passport.util.DESUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class PassportController {

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "preLogin")
    public String preLogin(HttpServletRequest request, Model model) {
        Cookie cookie = getCookieByName(request, PassportConfig.COOKIE_NAME);
        String setCookieURL = request.getParameter("setCookieURL");
        String gotoURL = request.getParameter("gotoURL");
        model.addAttribute("setCookieURL", setCookieURL);
        model.addAttribute("gotoURL", gotoURL);
        if (cookie == null) {
            return "login";
        } else {
            String encodedToken = cookie.getValue();
            String decodedToken = DESUtils.decrypt(encodedToken, PassportConfig.SECRET_KEY);
            if (redisUtil.hasKey(decodedToken)) {
                User user = JSON.parseObject(redisUtil.get(decodedToken).toString(), User.class);
                // 判断token是否存在
                if (user != null) {
                    if (setCookieURL != null) {
                        return "redirect:" + setCookieURL + "?token=" + encodedToken + "&expiry=" + cookie.getMaxAge() + "&gotoURL=" + gotoURL;
                    }
                }
            } else {
                return "login";
            }
        }
        return "login";
    }

    @RequestMapping(value = "authToken")
    @ResponseBody
    public String authToken(HttpServletRequest request) {
        StringBuilder result = new StringBuilder("{");
        String encodedToken = request.getParameter("cookieName");
        if (encodedToken == null) {
            result.append("\"error\":true,\"errorInfo\":\"Token can not be empty!\"");
        } else {
            String decodedToken = DESUtils.decrypt(encodedToken, PassportConfig.SECRET_KEY);
            if(redisUtil.hasKey(decodedToken)) {
                User user = JSON.parseObject(redisUtil.get(decodedToken).toString(), User.class);
                // 判断token是否存在
                if (user != null) {
                    result.append("\"error\":false,\"username\":").append("\"" + user.getUsername() + "\"");
                }
            }else {
                result.append("\"error\":true,\"errorInfo\":\"Token is not found!\"");
            }
        }
        result.append("}");
        return result.toString();
    }

    @RequestMapping(value = "doLogout")
    @ResponseBody
    public String doLogout(HttpServletRequest request) {
        StringBuilder result = new StringBuilder("{");
        String encodedToken = request.getParameter("cookieName");
        if (encodedToken == null) {
            result.append("\"error\":true,\"errorInfo\":\"Token can not be empty!\"");
        } else {
            String decodedToken = DESUtils.decrypt(encodedToken, PassportConfig.SECRET_KEY);
            redisUtil.del(decodedToken);
            result.append("\"error\":false");
        }
        result.append("}");
        return result.toString();
    }

    @RequestMapping(value = "doLogin")
    public String doLogin(HttpServletRequest request, HttpServletResponse response, String username, String password, Model model) {

        if (!userService.checkUser(username, password)) {
            model.addAttribute("errorInfo","username or password is wrong!");
            return "login";
        } else {
            String token = generateStrRecaptcha(16);
            String encodedToken = DESUtils.encrypt(token, PassportConfig.SECRET_KEY);
            User user = userService.getByUsernameAndPassword(username, password);

            redisUtil.set(token, JSON.toJSON(user), 30 * 60);
            addCookie(response, PassportConfig.COOKIE_NAME, encodedToken, PassportConfig.TOKEN_TIMEOUT);

            String setCookieURL = request.getParameter("setCookieURL");
            String gotoURL = request.getParameter("gotoURL");

            return "redirect:" + setCookieURL + "?token=" + encodedToken + "&expiry=" + PassportConfig.TOKEN_TIMEOUT + "&gotoURL=" + gotoURL;
        }
    }

    /**
     * 生成随机字符串(含大小写数字)
     */
    public static String generateStrRecaptcha(int length) {
        Random r = new Random(System.currentTimeMillis());

        StringBuffer sf = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = r.nextInt(3);
            long result = 0;
            switch (number) {
                case 0:
                    result = Math.round(Math.random() * 25 + 65);
                    sf.append(String.valueOf((char) result));
                    break;
                case 1:
                    result = Math.round(Math.random() * 25 + 97);
                    sf.append(String.valueOf((char) result));
                    break;
                case 2:
                    sf.append(String.valueOf(new Random().nextInt(10)));
                    break;

            }
        }
        return sf.toString();
    }

    public void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        if (maxAge > 0) {
            cookie.setMaxAge(maxAge);
        }
        response.addCookie(cookie);
    }

    public Cookie getCookieByName(HttpServletRequest request, String name) {
        Map<String, Cookie> cookieMap = readCookieMap(request);
        if (cookieMap.containsKey(name)) {
            Cookie cookie = (Cookie) cookieMap.get(name);
            return cookie;
        } else {
            return null;
        }
    }

    private Map<String, Cookie> readCookieMap(HttpServletRequest request) {
        Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                cookieMap.put(cookie.getName(), cookie);
            }
        }
        return cookieMap;
    }
}
