package com.aishare.knowledgerag.streaming;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AnswerStreamingConfiguration {

    @Bean(name = "answerStreamExecutor", destroyMethod = "shutdown")
    ExecutorService answerStreamExecutor(AnswerStreamingProperties properties) {
        AtomicInteger sequence = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                properties.coreThreads(),
                properties.maxThreads(),
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.queueCapacity()),
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "answer-stream-" + sequence.incrementAndGet()
                    );
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
