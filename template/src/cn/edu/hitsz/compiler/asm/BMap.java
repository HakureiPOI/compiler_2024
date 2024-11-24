package cn.edu.hitsz.compiler.asm;

import java.util.HashMap;
import java.util.Map;

public class BMap<K, V> {
    private final Map<K, V> KVmap = new HashMap<>();
    private final Map<V, K> VKmap = new HashMap<>();

    public void put(K key, V value) {
        // 复用 replace 方法来添加映射
        replace(key, value);
    }

    public void removeByKey(K key) {
        if (KVmap.containsKey(key)) {
            V value = KVmap.remove(key);
            VKmap.remove(value);
        }
    }

    public void removeByValue(V value) {
        if (VKmap.containsKey(value)) {
            K key = VKmap.remove(value);
            KVmap.remove(key);
        }
    }

    public boolean containsKey(K key) {
        return KVmap.containsKey(key);
    }

    public boolean containsValue(V value) {
        return VKmap.containsKey(value);
    }

    public void replace(K key, V value) {
        // 如果 key 或 value 已存在，则移除旧的映射
        if (containsKey(key)) {
            removeByKey(key);
        }
        if (containsValue(value)) {
            removeByValue(value);
        }
        // 添加新的映射
        KVmap.put(key, value);
        VKmap.put(value, key);
    }

    public V getByKey(K key) {
        return KVmap.get(key);
    }

    public K getByValue(V value) {
        return VKmap.get(value);
    }

    public void clear() {
        KVmap.clear();
        VKmap.clear();
    }

    public int size() {
        return KVmap.size(); // KVmap 和 VKmap 的大小始终一致
    }

    public boolean isEmpty() {
        return KVmap.isEmpty(); // KVmap 和 VKmap 的状态始终一致
    }
}
