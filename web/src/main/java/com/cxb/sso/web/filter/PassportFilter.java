package com.cxb.sso.web.filter;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.cxb.sso.web.config.WebConfig;
import com.cxb.sso.web.util.HttpClientUtils;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * 过滤器，过滤所有请求，验证是否已经登录
 *
 * @author baixiaozheng
 * @date 2020 -03-14 15:25:06
 */
@Order(1)
@WebFilter(filterName = "passportFilter", urlPatterns = "/*")
public class PassportFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getContextPath();
        String gotoURL = request.getParameter("gotoURL");
        if (gotoURL == null) {
            gotoURL = request.getRequestURL().toString();
        }
        String URL = WebConfig.SSO_SERVICE + "preLogin?setCookieURL=" + request.getScheme() + "://"
                + request.getServerName() + ":" + request.getServerPort() + path + "/setCookie&gotoURL=" + gotoURL;

        Cookie cookie = getCookieByName(request, WebConfig.COOKIE_NAME);
        if (request.getRequestURI().equals(path + "/logout")) {
            doLogout( response, cookie, URL);
        } else if (request.getRequestURI().equals(path + "/setCookie")) {
            setCookie(request, response);
        } else if (cookie != null) {
            authCookie(request, response, chain, cookie, URL);
        } else {
            response.sendRedirect(URL);
        }
    }

    /**
     * 设置cookie
     * @param request
     * @param response
     * @throws IOException
     */
    private void setCookie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Cookie cookie = new Cookie(WebConfig.COOKIE_NAME, request.getParameter("token"));
        cookie.setPath("/");
        cookie.setMaxAge(Integer.parseInt(request.getParameter("expiry")));
        response.addCookie(cookie);

        String gotoURL = request.getParameter("gotoURL");
        if (gotoURL != null){
            response.sendRedirect(gotoURL);
        }
    }

    /**
     * 登出
     * @param response
     * @param cookie
     * @param URL
     * @throws IOException
     */
    private void doLogout(HttpServletResponse response, Cookie cookie,
                          String URL) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("cookieName", cookie.getValue());
        try {
            post(params, "doLogout");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            response.sendRedirect(URL);
        }
    }

    /**
     * 验证本地存储的cookie是否有效
     * @param request
     * @param response
     * @param chain
     * @param cookie
     * @param URL
     * @throws IOException
     * @throws ServletException
     */
    private void authCookie(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Cookie cookie,
                            String URL) throws IOException, ServletException {

        Map<String, String> params = new HashMap<>();
        params.put("cookieName", cookie.getValue());
        try {
            JSONObject result = post(params, "authToken");
            if (result.getBoolean("error")) {
                response.sendRedirect(URL);
            } else {
                request.setAttribute("username", result.getString("username"));
                chain.doFilter(request, response);
            }
        } catch (JSONException e) {
            response.sendRedirect(URL);
            throw new RuntimeException(e);
        }
    }

    private JSONObject post(Map<String, String> params, String method) throws JSONException {
        String result = HttpClientUtils.sendHttpPostMap(WebConfig.SSO_SERVICE + method, params);
        return JSONObject.parseObject(result);

    }

    /**
     * Gets cookie by name.
     *
     *
     * @param request the request
     * @param name    the name
     * @return the cookie by name
     */
    private Cookie getCookieByName(HttpServletRequest request, String name) {
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
