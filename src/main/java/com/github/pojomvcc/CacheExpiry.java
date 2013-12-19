package com.github.pojomvcc;

/**
 * A {@code CacheExpiry} dictates how long revision history (tracked as a {@link CacheElementRevision})
 * should be kept in the {@code com.github.pojomvcc.RootObjectCache}. Once the {@link CacheExpiryPolicy}
 * has determined that a {@link CacheElementRevision} should no longer be kept an optional
 * {@code com.github.pojomvcc.CacheExpirationHandler} can be used to keep the {@link CacheElementRevision}
 * somewhere else.
 *
 * @author Aidan Morgan
 */
public class CacheExpiry<K, V> {
  /**
   * The default {@link CacheExpiry} to use
   */
  public static <K, V> CacheExpiry<K, V> DEFAULT() {
    return new CacheExpiry<K, V>(CacheExpiryPolicy.<K,V>NO_LONGER_USED());
  }

  /**
   * The {@link CacheExpiryPolicy} to use.
   */
  private CacheExpiryPolicy<K, V> policy;

  /**
   * The {@link CacheExpirationHandler} to use, defaults to {@link CacheExpiry.NoOpCacheExpirationHandler}.
   */
  private CacheExpirationHandler<K, V> handler = new NoOpCacheExpirationHandler<K, V>();

  /**
   * Creates a new {@link CacheExpiry} with a no-op {@link CacheExpirationHandler}
   * handler.
   *
   * @param policy The cache expiration policy.
   */
  public CacheExpiry(CacheExpiryPolicy<K, V> policy) {
    this.policy = policy;
  }

  /**
   * Creates a new {@link CacheExpiry} with the provided {@link CacheExpiryPolicy}
   * and the provided {@link CacheExpirationHandler}.
   *
   * @param policy  The cache expiration policy.
   * @param handler The expiration handler.
   */
  public CacheExpiry(CacheExpiryPolicy<K, V> policy, CacheExpirationHandler<K, V> handler) {
    this.policy = policy;
    this.handler = handler;
  }

  /**
   * Returns the cache's expiration policy.
   *
   * @return The expiration policy.
   */
  public CacheExpiryPolicy<K, V> getPolicy() {
    return policy;
  }

  /**
   * Returns the cache's expiration handler.
   *
   * @return The cache's expiration handler.
   */
  public CacheExpirationHandler<K, V> getHandler() {
    return handler;
  }

  /**
   * A no-op implementation of {@link CacheExpirationHandler}.
   */
  private static class NoOpCacheExpirationHandler<K, V> implements CacheExpirationHandler<K, V> {

    public void expired(RevisionKeyList<K> rev) {
      // No-op
    }

    public CacheElementRevision<K, V> retrieve(K key, long revision) {
      return null; // No-op
    }

  }

}
