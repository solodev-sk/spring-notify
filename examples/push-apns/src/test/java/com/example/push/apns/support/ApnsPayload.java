package com.example.push.apns.support;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * The APNs notification payload, e.g. {@code {"aps":{"alert":{"title":"…","body":"…"}}, "campaign":"…"}}.
 * The {@code aps} dictionary holds the alert; any other top-level keys are custom properties.
 */
public record ApnsPayload(Aps aps, Map<String, Object> custom) {

    public ApnsPayload {
        custom = custom == null ? new HashMap<>() : custom;
    }

    @JsonAnySetter
    void custom(String key, Object value) {
        custom.put(key, value);
    }

    public record Aps(Alert alert) {

    }

    public record Alert(String title, String body) {

    }
}
