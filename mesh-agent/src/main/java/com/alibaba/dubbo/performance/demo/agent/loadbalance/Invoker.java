package com.alibaba.dubbo.performance.demo.agent.loadbalance;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;


public interface Invoker {
    Boolean isAvalable();

    Endpoint id();
}
