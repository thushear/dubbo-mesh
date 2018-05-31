package com.alibaba.dubbo.performance.demo.agent.vertx;

import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.String;
import java.util.List;
import java.util.Random;

/**
 * <pre>
 * Copyright: www.jd.com
 * Author: kongming@jd.com
 * Created: 2018年05月26日 下午 21:47
 * Version: 1.0
 * Project Name: dubbo-mesh
 * Last Edit Time: 2018年05月26日 下午 21:47
 * Update Log:
 * Comment:
 * </pre>
 */
public class ProviderVerticle extends AbstractVerticle {


    private Logger logger = LoggerFactory.getLogger(ProviderVerticle.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient = new OkHttpClient();



    public byte[] provider(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {

        Object result = rpcClient.invoke(interfaceName,method,parameterTypesString,parameter);
        return (byte[]) result;
    }

    public void vertProvide(String interfaceName,String method,String parameterTypesString,String parameter,Message event){
        try {
            event.reply(
                    new String(provider(interfaceName,method,parameterTypesString,parameter))
            );
        } catch (Exception e) {
            event.reply(e.getMessage());
        }
    }



    @Override
    public void start() throws Exception {
        super.start();
        vertx.eventBus().consumer("bus.provider").handler(event -> {
            long start = System.currentTimeMillis();
            logger.warn("event body:{}",event.body());
            JsonObject jsonObject = (JsonObject) event.body();
            vertx.executeBlocking(fut -> {
                try {
                    byte[] result = provider(jsonObject.getString("interface"), jsonObject.getString("method"), jsonObject.getString("parameterTypesString"), jsonObject.getString("parameter"));
                    fut.complete(new String(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    fut.fail(e);
                }
            },false,result -> {

                if (result.succeeded()){
                    logger.warn("event res:{}",result.result());
                    logger.error("ThreadName {} executeBlocking cost time:{}ms",Thread.currentThread().getName(),(System.currentTimeMillis() - start) );
                    event.reply((String)result.result());
                }else {
                    logger.warn("event res:{}",result.cause());
                    event.reply(result.cause());
                }

            });
//            vertProvide(jsonObject.getString("interface"), jsonObject.getString("method"), jsonObject.getString("parameterTypesString"), jsonObject.getString("parameter"), event);
        });
    }
}
