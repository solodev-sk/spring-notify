package com.example.push.apns.support;

import tools.jackson.databind.json.JsonMapper;

public final class JsonHelper {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonHelper() {

    }

    public static String asJson(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }
}
