package com.alibaba.dubbo.performance.demo.agent.vertx;

import com.alibaba.dubbo.performance.demo.agent.HelloController;
import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.loadbalance.Invoker;
import com.alibaba.dubbo.performance.demo.agent.loadbalance.LoadBalanceStrategy;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.google.common.collect.Maps;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * <pre>
 * Copyright: www.jd.com
 * Author: kongming@jd.com
 * Created: 2018年05月26日  上午 10:37
 * Version: 1.0
 * Project Name: dubbo-mesh
 * Last Edit Time: 2018年05月26日  上午 10:37
 * Update Log:
 * Comment:
 * </pre>
 */
public class ConsumerVerticle extends AbstractVerticle {


    private Logger logger = LoggerFactory.getLogger(ConsumerVerticle.class);

    private IRegistry registry = new EtcdRegistry(System.getProperty("etcd.url"));

    private RpcClient rpcClient = new RpcClient(registry);
    private Random random = new Random();
    private List<Endpoint> endpoints = null;
    private Object lock = new Object();
    private OkHttpClient httpClient = new OkHttpClient();

    WebClient webClient = WebClient.create(vertx);

    private LoadBalanceStrategy loadBalanceStrategy;


    public Endpoint selectEndPoint(){
        try {
            if (null == endpoints) {
                synchronized (lock) {
                    if (null == endpoints) {
                        endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                        Map<Invoker,Integer> invokerIntegerMap = Maps.newHashMap();
                        for (Endpoint endpoint : endpoints) {
                            invokerIntegerMap.put(new Invoker() {
                                @Override
                                public Boolean isAvalable() {
                                    return true;
                                }

                                @Override
                                public Endpoint id() {
                                    return endpoint;
                                }
                            },endpoint.getWeight());
                        }
                        loadBalanceStrategy = LoadBalanceStrategy.selectStrategy(invokerIntegerMap);
                    }
                }
            }
        } catch (Exception e) {
//            endpoints = new ArrayList<>();
//            endpoints.add(new Endpoint("localhost",30000));
            logger.error("consumer error:",e);
        }


        // TODO  负载加入策略
        // 简单的负载均衡，随机取一个
//        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        // 加权round robin
        return   loadBalanceStrategy.select().id();
    }

    public void consumer(String interfaceName, String method, String parameterTypesString, String parameter, Message event) throws Exception {
        logger.warn("consumer: start {}",endpoints);
        try {
            if (null == endpoints) {
                synchronized (lock) {
                    if (null == endpoints) {
                        endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                        Map<Invoker,Integer> invokerIntegerMap = Maps.newHashMap();
                        for (Endpoint endpoint : endpoints) {
                            invokerIntegerMap.put(new Invoker() {
                                @Override
                                public Boolean isAvalable() {
                                    return true;
                                }

                                @Override
                                public Endpoint id() {
                                    return endpoint;
                                }
                            },endpoint.getWeight());
                        }
                        loadBalanceStrategy = LoadBalanceStrategy.selectStrategy(invokerIntegerMap);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("consumer error:",e);
        }

        logger.warn("consumer end endpoints:{}",endpoints);
        // TODO  负载加入策略
        // 简单的负载均衡，随机取一个
//        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        // 加权round robin
        Endpoint endpoint =  loadBalanceStrategy.select().id();
        long start = System.currentTimeMillis();
        webClient.get(endpoint.getPort(), endpoint.getHost(), "/").addQueryParam("interface", interfaceName)
                .addQueryParam("method", method).addQueryParam("parameterTypesString", parameterTypesString)
                .addQueryParam("parameter", parameter)
                .send(ar -> {

                    if (ar.succeeded()) {
                        logger.error("ThreadName {} webClient cost time:{}ms",Thread.currentThread().getName(),(System.currentTimeMillis() - start) );
                        event.reply(ar.result().bodyAsString());

                    } else {
                        event.reply(ar.cause().getMessage());
                        ar.cause().printStackTrace();
                    }
                });
    }


    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().consumer("bus.consumer").handler(event -> {
            logger.warn("event:{}",event.body());
            JsonObject jsonObject = (JsonObject) event.body();
            try {

                consumer(jsonObject.getString("interface"), jsonObject.getString("method"), jsonObject.getString("parameterTypesString"), jsonObject.getString("parameter"), event);

            } catch (Exception e) {
                event.reply(e.getMessage());
                e.printStackTrace();
            }
        });


//        vertx.eventBus().consumer("bus.consumer").handler(event -> {
//            logger.warn("event:{}",event.body());
//            JsonObject jsonObject = (JsonObject) event.body();
//
//            vertx.executeBlocking(fut->{
//                Endpoint endpoint = selectEndPoint();
//                webClient.get(endpoint.getPort(), endpoint.getHost(), "/").addQueryParam("interface", jsonObject.getString("interface"))
//                        .addQueryParam("method", jsonObject.getString("method")).addQueryParam("parameterTypesString", jsonObject.getString("parameterTypesString"))
//                        .addQueryParam("parameter",jsonObject.getString("parameter"))
//                        .send(ar -> {
//
//                            if (ar.succeeded()) {
//                                fut.complete(ar.result().bodyAsString());
//
//                            } else {
//
//                                fut.fail(ar.cause());
//                            }
//                        });
//
//
//
//            },false,result->{
//
//                if (result.succeeded()){
//                    logger.warn("event res:{}",result.result());
//                    logger.error("ThreadName {} executeBlocking cost time:{}ms",Thread.currentThread().getName(),(System.currentTimeMillis() - start) );
//                    event.reply((String)result.result());
//                }else {
//                    logger.warn("event res:{}",result.cause());
//                    event.reply(result.cause());
//                }
//            });
//
//        });
    }
}
