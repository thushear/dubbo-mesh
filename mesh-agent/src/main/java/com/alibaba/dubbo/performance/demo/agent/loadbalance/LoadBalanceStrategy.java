package com.alibaba.dubbo.performance.demo.agent.loadbalance;


import java.util.Map;

public interface LoadBalanceStrategy {


    public Invoker select();


    static LoadBalanceStrategy  selectStrategy(Map<Invoker, Integer> invokersWeight){
        return new RoundRobinByWeightLoadBalanceStrategy(invokersWeight);
    }

}
