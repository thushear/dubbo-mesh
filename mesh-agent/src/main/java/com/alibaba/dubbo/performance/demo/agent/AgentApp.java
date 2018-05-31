package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.registry.IpHelper;
import com.alibaba.dubbo.performance.demo.agent.vertx.AgentServerVerticle;
import com.alibaba.dubbo.performance.demo.agent.vertx.ConsumerVerticle;
import com.alibaba.dubbo.performance.demo.agent.vertx.ProviderVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

//@SpringBootApplication
public class AgentApp {


    private final static Logger logger = LoggerFactory.getLogger("vertx");

//    @Autowired
//    AgentServerVerticle agentServerVerticle;

    // TODO 启动加入策略
    static final Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(50));



    // agent会作为sidecar，部署在每一个Provider和Consumer机器上
    // 在Provider端启动agent时，添加JVM参数-Dtype=provider -Dserver.port=30000 -Ddubbo.protocol.port=20889
    // 在Consumer端启动agent时，添加JVM参数-Dtype=consumer -Dserver.port=20000
    // 添加日志保存目录: -Dlogs.dir=/path/to/your/logs/dir。请安装自己的环境来设置日志目录。

    public static void main(String[] args)  {
        try {
            logger.warn("ip:{}", IpHelper.getHostIp());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String type = System.getProperty("type");
        logger.warn("type={}",type);
        if ("provider".equalsIgnoreCase(type)){
            vertx.deployVerticle(new ProviderVerticle());
        }else {
            vertx.deployVerticle(new ConsumerVerticle());
        }
        vertx.deployVerticle(new AgentServerVerticle());

    }
}
