package com.aishare.knowledgerag.config;

import com.aishare.knowledgerag.retrieval.RetrievalProperties;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(prefix = "rag.nacos", name = "enabled", havingValue = "true")
public class NacosRetrievalConfigListener {

    private static final Logger log = LoggerFactory.getLogger(NacosRetrievalConfigListener.class);

    private final NacosProperties nacos;
    private final RetrievalProperties retrieval;
    private final NacosRetrievalConfigParser parser;
    private ConfigService configService;
    private Listener listener;

    public NacosRetrievalConfigListener(
            NacosProperties nacos,
            RetrievalProperties retrieval,
            NacosRetrievalConfigParser parser
    ) {
        this.nacos = nacos;
        this.retrieval = retrieval;
        this.parser = parser;
    }

    @PostConstruct
    void start() throws Exception {
        Properties connection = new Properties();
        connection.put("serverAddr", nacos.serverAddress());
        putIfPresent(connection, "namespace", nacos.namespace());
        putIfPresent(connection, "username", nacos.username());
        putIfPresent(connection, "password", nacos.password());
        configService = NacosFactory.createConfigService(connection);
        listener = new Listener() {
            @Override
            public Executor getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                apply(configInfo);
            }
        };
        String initial = configService.getConfig(
                nacos.dataId(), nacos.group(), nacos.timeoutMs()
        );
        if (initial != null && !initial.isBlank()) {
            apply(initial);
        }
        configService.addListener(nacos.dataId(), nacos.group(), listener);
        log.info("Nacos 检索配置监听已启动 dataId={} group={}",
                nacos.dataId(), nacos.group());
    }

    private void apply(String content) {
        try {
            parser.parse(content, retrieval).applyTo(retrieval);
            log.info("Nacos 检索配置已生效 vectorTopK={} keywordTopK={} finalTopK={} minScore={} rrfK={}",
                    retrieval.vectorTopK(), retrieval.keywordTopK(), retrieval.finalTopK(),
                    retrieval.minScore(), retrieval.rrfK());
        } catch (RuntimeException exception) {
            log.error("忽略无效的 Nacos 检索配置，继续使用上一个有效版本", exception);
        }
    }

    private void putIfPresent(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value);
        }
    }

    @PreDestroy
    void stop() throws Exception {
        if (configService != null && listener != null) {
            configService.removeListener(nacos.dataId(), nacos.group(), listener);
        }
        if (configService != null) {
            configService.shutDown();
        }
    }
}
