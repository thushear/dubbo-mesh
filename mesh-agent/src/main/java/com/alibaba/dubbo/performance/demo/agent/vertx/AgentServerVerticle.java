package com.alibaba.dubbo.performance.demo.agent.vertx;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AgentServerVerticle extends AbstractVerticle{


    static String  type;


    static {
        try {
            type = System.getenv("type");
            System.err.println("type:" + type);
        } catch (Exception e) {
            type = "provider";
            e.printStackTrace();
        }

    }


    static Integer port;

    static int defaultPort = 8080;


    static {

        try {
            port = Integer.valueOf(System.getenv("server.port"));
            System.err.println("server.port=" + port);
        } catch (Exception e) {
            port = defaultPort;
            e.printStackTrace();
        }

    }



    @Override
    public void start() throws Exception {
        super.start();
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

                System.err.println("method=" + method);
                if ("consumer".equalsIgnoreCase(type)){
                    vertx.eventBus().<String>send("bus.consumer",JsonObject.mapFrom(params),res-> {

                        System.err.println("res:" + res);
                        req.response().setStatusCode(200).write(res.result().body()).end();
                    } );
                }else {
                    vertx.eventBus().<String>send("bus.provider",JsonObject.mapFrom(params),res-> {

                        System.err.println("res:" + res);
                        req.response().setStatusCode(200).write(res.result().body()).end();
                    } );
                }
             }
        } );
        httpServer.listen(port);
    }
}
