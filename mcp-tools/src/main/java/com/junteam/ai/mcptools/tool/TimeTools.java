package com.junteam.ai.mcptools.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
public class TimeTools {
@Tool(name = "getCurrentTime", description = "获取指定时区的当前时间")
    public String getCurrentTime(String timeZone) {
        log.info("获取{}的当前时间", timeZone);
        // 这里可以调用第三方API获取时间，或者使用Java的时间库进行计算
        return LocalDateTime.now(ZoneId.of(timeZone)).toString();
    }
}
