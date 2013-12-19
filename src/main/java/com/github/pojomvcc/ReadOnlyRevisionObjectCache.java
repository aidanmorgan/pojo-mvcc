package com.github.pojomvcc;

/**
 * An implementation of the {@link com.github.pojomvcc.ObjectCache} interface that provides a
 * read-only view of a specific revision of the {@link com.github.pojomvcc.RootObjectCache}.
 *
 * @author Aidan Morgan
 */
public interface ReadOnlyRevisionObjectCache<K, V> extends ObjectCache<K, V> {
  /**
   * Returns the {@code com.github.pojomvcc.RootObjectCache} that owns this {@code com.github.pojomvcc.RevisionObjectCache}.
   *
   * @return
   */
  public RootObjectCache<K, V> getParentCache();

  /**
   * Returns {@code true} if the provided {@link K is present in this {@link ReadOnlyRevisionObjectCache}, {@code false} otherwise.
   *
   * @param key the key to retrieve the value for.
   * @return the value in the cache with the provided key, {@code null} if it does not exist. For read-only
   *         versions of this interface a {@link V} might have been removed by the {@link com.github.pojomvcc.CacheExpiry}
   *         implementation.
   */
  public boolean containsKey(K key);

}
