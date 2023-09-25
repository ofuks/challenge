package com.dws.challenge.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static <T> T toObject(String contentAsString, Class<T> tClass) throws JsonProcessingException {
        return objectMapper.readValue(contentAsString, tClass);
    }
}
