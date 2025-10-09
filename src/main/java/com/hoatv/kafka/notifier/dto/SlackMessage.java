package com.hoatv.kafka.notifier.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a Slack message for webhook API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackMessage {

    private String text;
    private String channel;
    private String username;
    private String icon_emoji;

    public static SlackMessage of(String text) {
        return SlackMessage.builder()
                .text(text)
                .build();
    }
}
