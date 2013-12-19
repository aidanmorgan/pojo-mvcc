package com.github.pojomvcc;

/**
 * Base-interface for all operations that can be performed on a {@link ObjectCache}.
 *
 * @author Aidan Morgan
 */
public interface ObjectCache<K, V> {

  /**
   * Returns the current revision of this {@code com.github.pojomvcc.ObjectCache} instance.
   *
   * @return The revision.
   */
  public long getRevision();

  /**
   * Returns the {@code CacheElement} with the provided {@link K} for the current revision of
   * this {@link ObjectCache} instance.
   * <p/>
   * If the {@link V} cannot be found then {@code null} should be returned.
   *
   * @param key The cache key.
   * @return The cache element.
   * @see ObjectCache#getRevision()
   */
  public V getElement(K key);

  /**
   * Returns the current size of this {@code com.github.pojomvcc.ObjectCache}.
   *
   * @return The current size of the this cache.
   */
  public int size();
}
