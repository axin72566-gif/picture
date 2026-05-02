package com.axin.picturebackend.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.axin.picturebackend.manager.websocket.PictureEditHandler;
import com.axin.picturebackend.manager.websocket.disruptor.PictureEditEvent;
import com.axin.picturebackend.manager.websocket.disruptor.PictureEditEventWorkHandler;
import com.axin.picturebackend.service.UserService;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

@Configuration
public class PictureEditEventDisruptorConfig {

    private static final int WORKER_COUNT = 4;

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PictureEditHandler pictureEditHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> messageModelRingBuffer() {
        int bufferSize = 1024 * 256;
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create().setNamePrefix("pictureEditEventDisruptor").build()
        );
        PictureEditEventWorkHandler[] workHandlers = new PictureEditEventWorkHandler[WORKER_COUNT];
        for (int i = 0; i < WORKER_COUNT; i++) {
            workHandlers[i] = new PictureEditEventWorkHandler(pictureEditHandler, userService, i, WORKER_COUNT);
        }
        // handleEventsWith 将同一事件广播给所有 handler，各 handler 按 pictureId 哈希过滤，
        // 保证同一图片的事件始终由同一线程处理，避免乱序
        disruptor.handleEventsWith(workHandlers);
        disruptor.start();
        return disruptor;
    }
}

