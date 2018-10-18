package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisPool {

    private static JedisPool pool;  //jedis连接池

    private static  Integer maxTotal= Integer.parseInt(PropertiesUtil.getProperty("redis.max.total","20"));  //最大连接数

    private static Integer maxIdle= Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","10"));  //在jedispool的最大idle状态（空闲的）的jedis的实例的个数

    private static  Integer minIdle= Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","2"));   //在edispool的最大idle状态（空闲的）的jedis的实例的个数

    private static Boolean testOnBorrower = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));  //在borrower一个jedis实例时，是否进行验证操作。如果赋值为true，则得到的jedis实例肯定时可以用的

    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));  //在return一个jedis实例时，是否进行验证操作。如果赋值为true，则放回jedispool的jedis实例肯定是可以用的


    private static String redisIp = PropertiesUtil.getProperty("redis.ip");

    private static Integer redisPort = Integer.parseInt(PropertiesUtil.getProperty("redis.port"));

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrower);
        config.setTestOnReturn(testOnReturn);

        config.setBlockWhenExhausted(true);  //连接耗尽的时候是否阻塞，false会抛出异常，true阻塞直到超时，默认为true

        pool = new JedisPool(config,redisIp,redisPort,1000*2);


    }
    static {
        initPool();
    }

    public static Jedis getJedis(){
        return pool.getResource();
    }

    public static void returnResource(Jedis jedis){
        if (jedis != null){
            pool.returnResource(jedis);
        }
    }

    public static void returnBrokenResource(Jedis jedis){
        if (jedis != null){
            pool.returnBrokenResource(jedis);
        }
    }

    public static void main(String[] args) {
        Jedis jedis = pool.getResource();
        jedis.set("name","lily");
        returnResource(jedis);

        pool.destroy();
        System.out.println("program end");
    }
}
