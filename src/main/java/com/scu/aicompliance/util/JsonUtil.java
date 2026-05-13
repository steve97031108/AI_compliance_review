package com.scu.aicompliance.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * JSON工具类，基于Jackson ObjectMapper封装常用JSON读写操作。
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
        // 工具类禁止实例化
    }

    /**
     * 从文件读取JSON并转换为指定类型的对象。
     *
     * @param file      JSON文件
     * @param valueType 目标类型
     * @param <T>       泛型类型
     * @return 转换后的对象
     * @throws RuntimeException 读取或解析失败时抛出
     */
    public static <T> T readValue(File file, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(file, valueType);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read JSON from file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 从文件读取JSON并转换为指定类型对象，支持泛型集合。
     *
     * @param file          JSON文件
     * @param typeReference 泛型类型引用
     * @param <T>           泛型类型
     * @return 转换后的对象
     * @throws RuntimeException 读取或解析失败时抛出
     */
    public static <T> T readValue(File file, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(file, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read JSON from file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 将对象写入JSON文件。
     *
     * @param file  目标文件
     * @param value 要写入的对象
     * @throws RuntimeException 写入失败时抛出
     */
    public static void writeValue(File file, Object value) {
        try {
            OBJECT_MAPPER.writeValue(file, value);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write JSON to file: " + file.getAbsolutePath(), e);
        }
    }
}