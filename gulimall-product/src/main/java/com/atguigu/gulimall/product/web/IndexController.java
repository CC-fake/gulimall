package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 孟享广
 * @date 2021-01-06 12:02 下午
 * @description
 */

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @GetMapping({"/", "/index.html"})
    public String  indexPage(Map<String, List<CategoryEntity>> map) {

        List<CategoryEntity> entities = categoryService.getLevel1Catrgorys();
        map.put("categorys", entities);
        return "index";
    }

    //index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        Map<String, List<Catelog2Vo>> map = categoryService.getCatalogJson();
        return map;
    }

    //压力测试
    //redisson锁测试
    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
        //1 获取一把锁 （只要锁名一样，就是同一把锁）
        RLock lock = redisson.getLock("my-lock");

        //2 加锁
//        lock.lock();//阻塞式等待 默认加的锁 都是30s
        //1） 锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s。不用担心担心业务时间长，锁自动过期被删掉
        //2） 加锁的业务只要运行完成，不会给当前锁续期，即使不手动解锁 锁也会在30s以后自动删除

        lock.lock(10, TimeUnit.SECONDS); //10秒自动解锁，自动解锁时间一定要大于业务的执行时间
        //1) 如果我们传递了锁的超时时间，就发送给redis执行脚本进行占锁，默认超时时间就是我们指定的时间
        //2) 如果我们没有指定锁的超时时间，就使用30*1000 【看门狗默认时间】
        //      只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗默认时间】每隔20秒自动续期，续成30s
        //      【看门狗时间】3，10s

        //最佳实战
        //1） 推荐lock.lock(30, TimeUnit.SECONDS); 省掉了续期操作。手动解锁
        try {
            System.out.println("加锁成功，执行业务..." + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("解锁...." + Thread.currentThread().getId());
            //3 解锁  假设解锁代码没有运行，redisson会不会出现死锁
            lock.unlock();
        }
        return "hello";
    }
}