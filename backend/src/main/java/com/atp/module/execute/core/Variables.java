package com.atp.module.execute.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 变量上下文：保存步骤执行过程中提取的变量，供后续步骤通过 ${varName} 引用。
 */
public class Variables {

    private final Map<String, Object> map = new LinkedHashMap<>();

    public void put(String name, Object value) {
        map.put(name, value);
    }

    public Object get(String name) {
        return map.get(name);
    }

    public boolean contains(String name) {
        return map.containsKey(name);
    }

    public Map<String, Object> asMap() {
        return map;
    }
}
