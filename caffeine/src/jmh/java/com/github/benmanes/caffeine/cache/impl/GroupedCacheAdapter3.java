package com.github.benmanes.caffeine.cache.impl;

import com.github.benmanes.caffeine.cache.BasicCache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GroupedCacheAdapter3<K, V> implements BasicCache<K, V> {

  private GuavaCache<K, Map<K, V>> adaptedCache;

  public GroupedCacheAdapter3(int maximumSize) {
    adaptedCache = new GuavaCache<>(maximumSize);
  }

  @Override
  public V get(@NonNull K key) {
    Map<K, V> groupMap = adaptedCache.get(key);
    if (null == groupMap) {
      return null;
    }
    return groupMap.get(key);
  }

  @Override
  public synchronized void put(@NonNull K key, @NonNull V value) {
    Map<K, V> groupMap = adaptedCache.get(key);
    if (null == groupMap) {
      groupMap = new ConcurrentHashMap<>();
      adaptedCache.put(key, groupMap);
    }
    groupMap.put(key, value);
  }

  @Override
  public void clear() {
    adaptedCache.clear();
  }

  @Override
  public void cleanUp() {
    adaptedCache.cleanUp();
  }

}
