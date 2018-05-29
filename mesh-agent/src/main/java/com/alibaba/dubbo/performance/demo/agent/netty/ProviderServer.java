package com.alibaba.dubbo.performance.demo.agent.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * <pre>
 * Copyright: www.jd.com
 * Author: kongming@jd.com
 * Created: 2018年05月29日 下午 23:26
 * Version: 1.0
 * Project Name: dubbo-mesh
 * Last Edit Time: 2018年05月29日 下午 23:26
 * Update Log:
 * Comment:
 * </pre>
 */
public class ProviderServer {



    public static void startNetty(int port) throws InterruptedException {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,100)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {

                    }
                });

        serverBootstrap.bind(port).sync();
    }


    public static void main(String[] args) throws InterruptedException {
        startNetty(8000);
    }

}
