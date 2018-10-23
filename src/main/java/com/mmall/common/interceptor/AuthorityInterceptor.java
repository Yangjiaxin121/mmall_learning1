package com.mmall.common.interceptor;

import com.google.common.collect.Maps;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;


@Slf4j
public class AuthorityInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handle) throws Exception {

        log.info("preHandle");

        HandlerMethod handlerMethod = (HandlerMethod)handle;
        //解析handleMethod
        String methodName = handlerMethod.getMethod().getName();
        String className = handlerMethod.getBean().getClass().getSimpleName();

        //解析参数，具体的参数key，value是什么，我们打印日志
        StringBuffer requestParamBuffer = new StringBuffer();
        Map paramMap = httpServletRequest.getParameterMap();
        Iterator it = paramMap.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            String mapKey = (String) entry.getKey();

            String mapValue = StringUtils.EMPTY;

            //request这个参数的map。里面的value返回一个String[]
            Object obj = entry.getValue();
            if (obj instanceof String[]){
                String[] strs = (String[])obj;
                mapValue = Arrays.toString(strs);
            }
            requestParamBuffer.append(mapKey).append("=").append(mapValue);
        }

        if (StringUtils.equals(className,"UserManageController") ||StringUtils.equals(methodName,"login")){
            log.info("权限拦截器拦截到请求className{},methodName{}",className,methodName);
            //拦截到登录请求，不打印参数，因为参数包含密码全部会打印到日志中，防止日志泄漏，
            return true;
        }

        User user = null;
        String loginToken = CookieUtil.readLoginCookie(httpServletRequest);
        if (StringUtils.isNotEmpty(loginToken)){
            String userJsonStr = RedisShardedPoolUtil.get(loginToken);
            user = JsonUtil.string2Obj(userJsonStr,User.class);
            //return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        if (user == null || user.getRole().intValue() != Const.Role.ROLE_ADMIN){
            //返回false方法
            httpServletResponse.reset();//这里要添加reset，否则会报异常 getWriter has been called for this response
            httpServletResponse.setCharacterEncoding("UTF-8");//这里要设置编码，否则会乱吗
            httpServletResponse.setContentType("application/json;charset=UTF-8");//这里要设置返回值的类型，因为全是json接口


            PrintWriter out = httpServletResponse.getWriter();

            Map resultMap = Maps.newHashMap();

            //上传由于富文本的控件要求，要特殊处理返回值，区分是否登录以及是否有权限
            if (user == null){
                if (StringUtils.equals(className,"ProductManagerController") ||StringUtils.equals(methodName,"richtextImguUpload")){
                    resultMap.put("success",false);
                    resultMap.put("msg","用户未登录，请登录管理员");
                    out.print(JsonUtil.object2String(resultMap));
                }else{
                    out.print(JsonUtil.object2String(ServerResponse.createByErrorMessage("拦截器拦截，请登录")));
                }
            } else{
                if (StringUtils.equals(className,"ProductManagerController") ||StringUtils.equals(methodName,"richtextImguUpload")){
                    resultMap.put("success",false);
                    resultMap.put("msg","无权限");
                    out.print(JsonUtil.object2String(resultMap));
                }else{
                    //out.print(JsonUtil.object2String(ServerResponse.createByErrorMessage("拦截器拦截，请登录")));
                    out.print(JsonUtil.object2String(ServerResponse.createByErrorMessage("拦截器拦截，无权限")));
                }

            }
            out.flush();
            out.close();   //这里要关闭
            return false;

        }



        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handle, ModelAndView modelAndView) throws Exception {

        log.info("postHandle");
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handle, Exception e) throws Exception {

        log.info("afterCompletion");
    }
}
