package com.alibaba.dubbo.performance.demo.agent.vertx;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentServerVerticle extends AbstractVerticle {


    private final Logger logger = LoggerFactory.getLogger(AgentServerVerticle.class);


    @Override
    public void start() throws Exception {
        super.start();
        String type = System.getProperty("type");
        String port = System.getProperty("server.port");
        logger.warn("type:{},port:{}", type, port);
        final Router router = Router.router(vertx);
        HttpServer httpServer = vertx.createHttpServer();

        router.route().handler(BodyHandler.create());

        router.post("/").handler(routingContext -> {
            String interfaceName = routingContext.request().getParam("interface");

            Map<String, String> params = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : routingContext.request().params()) {
                params.put(entry.getKey(), entry.getValue());
            }

            if ("consumer".equalsIgnoreCase(type)) {
                vertx.eventBus().<String>send("bus.consumer", JsonObject.mapFrom(params), res -> {
                    routingContext.response().setChunked(true);
                    logger.warn("res:{},params:{}", res, params);

                    routingContext.response().setStatusCode(200).write(res.result().body()).end();
                });
            }

        });


        router.get("/").handler(routingContext -> {
            routingContext.response().setChunked(true);
            String interfaceName = routingContext.request().getParam("interface");
            System.err.println("interfaceName:" + interfaceName);
            System.err.println("routingContext.request().formAttributes():" + routingContext.request().formAttributes());
            Map<String, String> params = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : routingContext.request().params()) {
                params.put(entry.getKey(), entry.getValue());
            }
            System.err.println("params:" + params);
            if ("provider".equalsIgnoreCase(type)) {
                vertx.eventBus().<String>send("bus.provider", JsonObject.mapFrom(params), res -> {
                    logger.warn("res:{},params:{}", res, params);
                    System.err.println("res:" + res.result().body());
                    routingContext.response().setStatusCode(200).write(res.result().body()).end();
                });
            }

        });


//        httpServer.requestHandler(req -> {
//            req.response().setChunked(true);
//
//             if (req.path().equalsIgnoreCase("") || "/".equalsIgnoreCase(req.path())){
//                 System.err.println("req:" + req.method() + "|formAttributes: " + req.formAttributes());
//
//                String interfaceName = req.getParam("interface");
//                String method = req.getParam("method");
//                String parameterTypesString = req.getParam("parameterTypesString");
//                String parameter = req.getParam("parameter");
//                 System.err.println("interfaceName:" + interfaceName + " | method:" + method);
//                 Map<String,String> params = new HashMap<String,String>();
//                 for (Map.Entry<String, String> entry : req.params()) {
//                    params.put(entry.getKey(),entry.getValue());
//                 }
//                 System.err.println("params:" + params);
//                logger.warn("params:{},uri:{}",params,req.uri());
//                if ("consumer".equalsIgnoreCase(type)){
//                    vertx.eventBus().<String>send("bus.consumer",JsonObject.mapFrom(params),res-> {
//                        logger.warn("res:{},params:{}",res,params);
//                        System.err.println("res:" + res);
//                        req.response().setStatusCode(200).write(res.result().body()).end();
//                    } );
//                }else {
//                    vertx.eventBus().<String>send("bus.provider",JsonObject.mapFrom(params),res-> {
//                        logger.warn("res:{},params:{}",res,params);
//                        System.err.println("res:" + res);
//                        req.response().setStatusCode(200).write(res.result().body()).end();
//                    } );
//                }
//             }
//        } );
        httpServer.requestHandler(router::accept);
        httpServer.listen(Integer.valueOf(port));
    }
}
