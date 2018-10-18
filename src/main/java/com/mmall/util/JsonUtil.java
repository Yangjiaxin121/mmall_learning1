package com.mmall.util;

import com.google.common.collect.Lists;
import com.mmall.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

@Slf4j
public class JsonUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {

        //对象的所有字段全部列入
        objectMapper.setSerializationInclusion(Inclusion.ALWAYS);

        //取消默认转换时间戳形式
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS,false);

        //忽略空bean转json错误
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS,false);

        //所有的时间格式统一为  "yy-MM-dd HH-mm-ss"
        objectMapper.setDateFormat(new SimpleDateFormat(DateTimeUtil.STANDARD_FORMAT));

        //忽略在json字符串中产生，但在java对象中不存在对应属性的情况，防止错误
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,false);

    }

    public static <T> String object2String(T obj){
        if (obj == null){
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Parse object to String error",e);
            return null;
        }
    }

    public static <T> String object2StringPretty(T obj){
        if (obj == null){
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Parse object to String error",e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, Class<T> clazz){
        if (StringUtils.isEmpty(str) || clazz == null){
            return null;
        }
        try {
            return clazz.equals(String.class) ? (T)str : objectMapper.readValue(str,clazz);
        } catch (Exception e) {
            log.error("Parse str to obj error",e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, TypeReference<T> typeReference){
        if (StringUtils.isEmpty(str) || typeReference == null){
            return null;
        }
        try {
            return (T)(typeReference.getType().equals(String.class) ? (T)str : objectMapper.readValue(str,typeReference));
        } catch (Exception e) {
            log.error("Parse str to obj error",e);
            return null;
        }
    }

    public static <T> T string2Obj(String str, Class<?> collectionClass, Class<?>... elementClasses){
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClass,elementClasses);

        try {
            return objectMapper.readValue(str,javaType);
        } catch (Exception e) {
            log.error("Parse str to obj error",e);
            return null;
        }
    }

    public static void main(String[] args){

        User user = new User();
        user.setUsername("yang");
        user.setEmail("11232@qq.com");

        User user2 = new User();
        user2.setUsername("yang2");
        user2.setEmail("11232@qq.com");

        String userJson = object2String(user);

        String userJson1 = object2StringPretty(user);

        log.info("json1 {}", userJson);
        log.info("json2 {}",userJson1);

        User user1 = string2Obj(userJson,User.class);

        log.info("user1 {}",user1);

        List<User> userList = Lists.newArrayList();
        userList.add(user);
        userList.add(user2);

        String userJson2 = object2StringPretty(userList);
        log.info("userJson2 {}",userJson2);

        List userList2 = string2Obj(userJson2, new TypeReference<List<User>>() {
        });
        log.info("userList2 {}",userList2);

        List userList3 = string2Obj(userJson2,List.class,User.class);
        log.info("userList3 {}",userList3);

    }
}










