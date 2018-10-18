package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class CookieUtil {

    private static final String COOKIE_DOMAIN = ".happymmall.com";

    private static final String COOKIE_NAME = "mmall_login_cookie";


    public static String readLoginCookie(HttpServletRequest request){
        Cookie[] cks = request.getCookies();
        if (cks != null){
            for (Cookie ck: cks) {
                log.info("read cookieName {}, cookieValue {}",ck.getName(),ck.getValue());
                if (StringUtils.equals(ck.getName(),COOKIE_NAME)){
                    log.info("read cookieName {}, cookieValue {}",ck.getName(),ck.getValue());
                    return ck.getValue();
                }
            }
        }
        return null;
    }

    public static void writeLoginCookie(HttpServletResponse response, String token){

        Cookie ck = new Cookie(COOKIE_NAME,token);
        ck.setDomain(COOKIE_DOMAIN);
        ck.setPath("/");   //代表设置的根目录
        ck.setMaxAge(60*60*24*365);   //单位为秒，如果不设置默认只在内存中，不会写入硬盘，只在当前页面有效   -1代表永久

        log.info("write cookieName{} cookieValue{}",ck.getName(),ck.getValue());

        response.addCookie(ck);

    }

    public static void delLoginCookie(HttpServletRequest request, HttpServletResponse response){
        Cookie[] cks = request.getCookies();
        for (Cookie ck: cks) {
            if (StringUtils.equals(ck.getName(),COOKIE_NAME)){
                ck.setDomain(COOKIE_DOMAIN);
                ck.setPath("/");
                ck.setMaxAge(0);  //设置为0，代表删除此cookie
                response.addCookie(ck);
                return;
            }
        }
    }
}
