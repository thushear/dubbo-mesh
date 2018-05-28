package com.alibaba.dubbo.performance.demo.agent.vertx;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentServerVerticle extends AbstractVerticle{


    private final Logger logger = LoggerFactory.getLogger(AgentServerVerticle.class);



    @Override
    public void start() throws Exception {
        super.start();
        String type = System.getProperty("type");
        String port = System.getProperty("server.port");
        logger.warn("type:{},port:{}",type,port);
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(req -> {
            req.response().setChunked(true);

             if (req.path().equalsIgnoreCase("") || "/".equalsIgnoreCase(req.path())){
                String interfaceName = req.getParam("interface");
                String method = req.getParam("method");
                String parameterTypesString = req.getParam("parameterTypesString");
                String parameter = req.getParam("parameter");
                 Map<String,String> params = new HashMap<String,String>();
                 for (Map.Entry<String, String> entry : req.params()) {
                    params.put(entry.getKey(),entry.getValue());
                 }

                if ("consumer".equalsIgnoreCase(type)){
                    vertx.eventBus().<String>send("bus.consumer",JsonObject.mapFrom(params),res-> {
                        logger.warn("res:{},params:{}",res,params);
                        System.err.println("res:" + res);
                        req.response().setStatusCode(200).write(res.result().body()).end();
                    } );
                }else {
                    vertx.eventBus().<String>send("bus.provider",JsonObject.mapFrom(params),res-> {
                        logger.warn("res:{},params:{}",res,params);
                        System.err.println("res:" + res);
                        req.response().setStatusCode(200).write(res.result().body()).end();
                    } );
                }
             }
        } );
        httpServer.listen(Integer.valueOf(port));
    }
}
