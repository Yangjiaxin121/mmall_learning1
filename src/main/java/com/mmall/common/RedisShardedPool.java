package com.mmall.common;

import com.mmall.util.PropertiesUtil;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

import java.util.ArrayList;
import java.util.List;

public class RedisShardedPool {


    private static ShardedJedisPool pool;  //sharded jedis连接池

    private static  Integer maxTotal= Integer.parseInt(PropertiesUtil.getProperty("redis.max.total","20"));  //最大连接数

    private static Integer maxIdle= Integer.parseInt(PropertiesUtil.getProperty("redis.max.idle","10"));  //在jedispool的最大idle状态（空闲的）的jedis的实例的个数

    private static  Integer minIdle= Integer.parseInt(PropertiesUtil.getProperty("redis.min.idle","2"));   //在edispool的最大idle状态（空闲的）的jedis的实例的个数

    private static Boolean testOnBorrower = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.borrow","true"));  //在borrower一个jedis实例时，是否进行验证操作。如果赋值为true，则得到的jedis实例肯定时可以用的

    private static Boolean testOnReturn = Boolean.parseBoolean(PropertiesUtil.getProperty("redis.test.return","true"));  //在return一个jedis实例时，是否进行验证操作。如果赋值为true，则放回jedispool的jedis实例肯定是可以用的


    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");

    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));

    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");

    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    private static void initPool(){
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrower);
        config.setTestOnReturn(testOnReturn);

        config.setBlockWhenExhausted(true);  //连接耗尽的时候是否阻塞，false会抛出异常，true阻塞直到超时，默认为true

        //pool = new RedisShardedPool(config,redisIp,redisPort,1000*2);

        JedisShardInfo info1 = new JedisShardInfo(redis1Ip,redis1Port,1000*2);

        JedisShardInfo info2 = new JedisShardInfo(redis2Ip,redis2Port,1000*2);

        List<JedisShardInfo> jedisShardInfoList = new ArrayList<>(2);

        jedisShardInfoList.add(info1);
        jedisShardInfoList.add(info2);

        pool = new ShardedJedisPool(config, jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);


    }
    static {
        initPool();
    }

    public static ShardedJedis getShardedJedis(){
        return pool.getResource();
    }

    public static void returnResource(ShardedJedis jedis){
        if (jedis != null){
            pool.returnResource(jedis);
        }
    }

    public static void returnBrokenResource(ShardedJedis jedis){
        if (jedis != null){
            pool.returnBrokenResource(jedis);
        }
    }

    public static void main(String[] args) {
        ShardedJedis jedis = pool.getResource();
        for (int i=0;i<10;i++){
            jedis.set("key"+i,"value"+i);
        }
        //jedis.set("name","lily");
        returnResource(jedis);

        //pool.destroy();
        System.out.println("program end");
    }
}
