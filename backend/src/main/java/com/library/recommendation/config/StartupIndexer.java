package com.library.recommendation.config;

import com.library.recommendation.service.VectorIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupIndexer {

    private final VectorIndexService vectorIndexService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void indexOnStartup() {
        try {
            vectorIndexService.ensureIndex();
            log.info("启动时向量索引检查完成");
        } catch (Exception ex) {
            log.warn("启动时向量索引初始化失败，AI 语义检索首次使用时会重试: {}", ex.getMessage());
        }
    }
}
