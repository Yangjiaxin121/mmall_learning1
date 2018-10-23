package com.mmall.controller.backend;


import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/user")
public class UserManageController {

    @Autowired
    IUserService iUserService;

    @RequestMapping(value = "login.do",method = RequestMethod.GET)
    @ResponseBody
    ServerResponse<User> login(String username, String password, HttpServletRequest httpServletRequest, HttpSession session, HttpServletResponse httpServletResponse){
        ServerResponse<User> response = iUserService.login(username,password);
        if (response.isSuccess()) {
            User user = response.getData();
            if (user.getRole() == Const.Role.ROLE_ADMIN){
                //说明是管理员用户
                //session.setAttribute(Const.CURRENT_USER,user);
                CookieUtil.writeLoginCookie(httpServletResponse,session.getId());
                RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.object2String(response.getData()),Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
                return response;
            } else{
                return ServerResponse.createByErrorMessage("不是管理员无法登录");
            }
        }
        return response;
    }
}
