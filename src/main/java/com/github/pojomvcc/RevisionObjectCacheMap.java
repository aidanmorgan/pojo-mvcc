package com.github.pojomvcc;

import java.util.*;

/**
 * An implementation of the {@code java.util.Map} interface that retrieves all of it's
 * values from a {@link RevisionObjectCache}.
 * <p/>
 * The returned {@code java.util.Map} is effectively a read-only {@code java.util.Map},
 * that means that all methods which attempt to modify the underlying {@code java.util.Map}
 * will throw a {@code java.lang.UnsupportedOperationException}.
 *
 * @author Aidan Morgan
 */
public class RevisionObjectCacheMap<K,V> implements Map<K, V> {
  /**
   * A {@code java.util.Map} of {@link K} to {@link V}.
   */
  private Map<K, V> valueMap;

  /**
   * Constructor.
   *
   * @param rootCache the {@code com.github.pojomvcc.impl.RootObjectCacheImpl} to initialise this
   *                  {@link RevisionObjectCacheMap} from.
   */
  public RevisionObjectCacheMap(RootObjectCache<K,V> rootCache) {
    valueMap = new HashMap<K, V>(rootCache.size());
    initialiseMap(rootCache);
  }

  /**
   * Initalises the underlying {@code java.util.Map} from the provided {@link RootObjectCache}.
   *
   * @param rootCache
   */
  private void initialiseMap(RootObjectCache<K,V> rootCache) {
    RevisionObjectCache<K,V> cache = rootCache.checkout();

    for (K key : cache.getKeys()) {
      valueMap.put(key, cache.getElement(key));
    }
    
    cache.close();
  }

  /**
   * @inheritDoc
   */
  public int size() {
    return valueMap.size();
  }

  /**
   * @inheritDoc
   */
  public boolean isEmpty() {
    return valueMap.isEmpty();
  }

  /**
   * @inheritDoc
   */
  public boolean containsKey(Object key) {
    return valueMap.containsKey(key);
  }

  /**
   * @inheritDoc
   */
  public boolean containsValue(Object value) {
    return valueMap.containsValue(value);
  }

  /**
   * @inheritDoc
   */
  public V get(Object key) {
    return valueMap.get(key);
  }

  /**
   * <b>Will throw a {@code java.lang.UnsupportedOperationException} if called.</b>
   *
   * @inheritDoc
   */
  public V put(K key, V value) {
    throw new UnsupportedOperationException("Cannot add values to a RevisionObjectCacheMap.");
  }

  /**
   * <b>Will throw a {@code java.lang.UnsupportedOperationException} if called.</b>
   *
   * @inheritDoc
   */
  public V remove(Object key) {
    throw new UnsupportedOperationException("Cannot remove values from a RevisionObjectCacheMap.");
  }

  /**
   * <b>Will throw a {@code java.lang.UnsupportedOperationException} if called.</b>
   *
   * @inheritDoc
   */
  public void putAll(Map<? extends K, ? extends V> m) {
    throw new UnsupportedOperationException("Cannot add values to a RevisionObjectCacheMap.");
  }

  /**
   * <b>Will throw a {@code java.lang.UnsupportedOperationException} if called.</b>
   *
   * @inheritDoc
   */
  public void clear() {
    throw new UnsupportedOperationException("Cannot remove values from a RevisionObjectCacheMap.");
  }

  /**
   * @inheritDoc
   */
  public Set<K> keySet() {
    return valueMap.keySet();
  }

  /**
   * @inheritDoc
   */
  public Collection<V> values() {
    return valueMap.values();
  }

  /**
   * @inheritDoc
   */
  public Set<Entry<K, V>> entrySet() {
    return valueMap.entrySet();
  }
}
