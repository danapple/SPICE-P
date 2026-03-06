package com.danapple.spicep.coincap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
class ExecutorConfig {
    @Value( "${spicep.coinCapPriceService.threadCount:3}" )
    private int threadCount;

    @Bean("coinCapExecutorService")
    ExecutorService executorService() {
        System.out.println("Creating executor with " + threadCount + " threads");
        return Executors.newFixedThreadPool(threadCount);
    }

    @Bean("priceRefreshExecutorService")
    ScheduledExecutorService priceRefreshExecutorService() {
        System.out.println("Creating price refresh executor");
        return Executors.newSingleThreadScheduledExecutor();
    }

}
