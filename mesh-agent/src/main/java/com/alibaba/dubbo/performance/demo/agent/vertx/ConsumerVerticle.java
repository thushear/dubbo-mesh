package com.alibaba.dubbo.performance.demo.agent.vertx;

import com.alibaba.dubbo.performance.demo.agent.HelloController;
import com.alibaba.dubbo.performance.demo.agent.dubbo.RpcClient;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
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


    public void consumer(String interfaceName, String method, String parameterTypesString, String parameter, Message event) throws Exception {
        logger.warn("consumer: start {}",endpoints);
        try {
            if (null == endpoints) {
                synchronized (lock) {
                    if (null == endpoints) {
                        endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
                    }
                }
            }
        } catch (Exception e) {
//            endpoints = new ArrayList<>();
//            endpoints.add(new Endpoint("localhost",30000));
            logger.error("consumer error:",e);
        }

        logger.warn("consumer end endpoints:{}",endpoints);
        // TODO  负载加入策略
        // 简单的负载均衡，随机取一个
        Endpoint endpoint = endpoints.get(random.nextInt(endpoints.size()));

        WebClient webClient = WebClient.create(vertx);

        String s;
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



//        String url =  "http://" + endpoint.getHost() + ":" + endpoint.getPort();
//
//        RequestBody requestBody = new FormBody.Builder()
//                .add("interface",interfaceName)
//                .add("method",method)
//                .add("parameterTypesString",parameterTypesString)
//                .add("parameter",parameter)
//                .build();
//
//        Request request = new Request.Builder()
//                .url(url)
//                .post(requestBody)
//                .build();
//
//        try (Response response = httpClient.newCall(request).execute()) {
//            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
//            byte[] bytes = response.body().bytes();
//            String s = new String(bytes);
//            return Integer.valueOf(s);
//        }
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
    }
}
