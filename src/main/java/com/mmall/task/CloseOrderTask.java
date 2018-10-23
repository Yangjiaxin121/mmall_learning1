package com.mmall.task;


import com.mmall.common.Const;
import com.mmall.common.RedissonManager;
import com.mmall.service.IOrderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    IOrderService iOrderService;

    @Autowired
    RedissonManager redissonManager;


    @Scheduled(cron = "0 */1 * * * ?")  //每一分钟
    public void closeOrderTaskV4(){
        RLock lock = redissonManager.getRedisson().getLock(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        boolean getLock = false;
        try {
            if (getLock = lock.tryLock(2,5, TimeUnit.SECONDS)){

                log.info("Redisson获取到分布式锁{}.ThreadName{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
                //iOrderService.closeOrder(hour);

            } else {
                log.info("Redisson没有获取到分布式锁{}.ThreadName{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            log.info("Redisson获取分布式锁异常");
        } finally{
            if (!getLock){
                return;
            }
            lock.unlock();
            log.info("Redisson分布式锁释放锁");
        }
    }


    //@Scheduled(cron = "0 */1 * * * ?")  //每一分钟
    public void closeOrderTaskV1(){
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
        iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }


    //@Scheduled(cron = "0 */1 * * * ?")  //每一分钟
    public void closeOrderTaskV3(){
        log.info("关闭订单定时任务启动");
        long lockTimeout = Integer.parseInt(PropertiesUtil.getProperty("lock.timeout"));
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(lockTimeout+System.currentTimeMillis()));
        if (setnxResult != null && setnxResult.intValue() == 1) {
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }else{
            //未获取到锁，继续判断，判断时间戳，看是否可以重置并获取到锁
            String lockValueStr = RedisShardedPoolUtil.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            if (lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)){

                String getSetResult = RedisShardedPoolUtil.getSet(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(lockTimeout+System.currentTimeMillis()));
                //再次用当前时间戳getset
                //返回给定的key的旧值 --> 根据旧值进行判断 是否可以获得锁
                //当key没有旧值是，即key不存在时，返回nil --> 获取锁
                //这里我们set了一个新的value值，获取旧的值
                //没有被其他进程获取到
                if (getSetResult == null || (getSetResult != null && StringUtils.equals(getSetResult,lockValueStr))){
                    //真正获取到锁
                    closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }else{
                    log.info("没有获取到分布式锁{} ",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }
            }else{
                log.info("没有获取到分布式锁{} ",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            }
        }

        //iOrderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    private void closeOrder(String lockName){
        RedisShardedPoolUtil.expire(lockName,50);  //有效期为50秒，防止死锁
        log.info("获取锁{},ThreadName{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour"));
        //iOrderService.closeOrder(hour);
        RedisShardedPoolUtil.del(lockName);
        log.info("释放锁{},ThreadName{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread().getName());
        log.info("==================");
    }

}
