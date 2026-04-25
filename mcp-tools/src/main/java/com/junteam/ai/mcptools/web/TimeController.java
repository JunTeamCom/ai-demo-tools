package com.junteam.ai.mcptools.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.junteam.ai.mcptools.tool.TimeTools;

@RestController
public class TimeController {
    private static final String CURRENT_TIME_TEMPLATE = "{city}地区当前时间是多少？";
    private final ChatClient chatClient;

    public TimeController(ChatClient.Builder chatClientBuilder,
            TimeTools timeTools) {
        this.chatClient = chatClientBuilder
                .defaultTools(timeTools)
                .build();
    }

    @SuppressWarnings("null")
    @GetMapping(path = "/time", params = "city")
    public String getTime(@RequestParam String city) {
        return chatClient.prompt()
                .user(userSpec -> {
                    userSpec
                            .text(CURRENT_TIME_TEMPLATE)
                            .param("city", city);
                })
                .call()
                .content();
    }
}
