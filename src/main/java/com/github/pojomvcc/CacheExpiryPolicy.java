package com.github.pojomvcc;

/**
 * Defines how and when a {@code com.github.pojomvcc.CacheElement} instance's history should be removed from the
 * {@code com.github.pojomvcc.RootObjectCache}.
 * <p/>
 * It is very important that instances of this class perform very quickly. This operation is called frequently to clean
 * up the {@link RootObjectCache}, all while maintaining the write lock, so excessively long running
 * expiry policies will cause issues.
 *
 * @author Aidan Morgan
 */
public abstract class CacheExpiryPolicy<K, V> {

  /**
   * Never remove history from the {@code com.github.pojomvcc.RootObjectCache}.
   */
  public static <K, V> CacheExpiryPolicy<K, V> NEVER() {
    return new CacheExpiryPolicy<K, V>() {
      @Override
      public boolean shouldRun(RootObjectCache<K, V> rootObjectCache) {
        return false; // No-op
      }

      @Override
      public boolean shouldExpire(RootObjectCache<K, V> rootCache, RevisionKeyList<K> rkl) {
        return false; // No-op
      }

    };
  }

  /**
   * Remove history when there are no-longer any active {@code com.github.pojomvcc.RevisionObjectCache} instances
   * referring to the revision.
   */
  public static <K, V> CacheExpiryPolicy<K, V> NO_LONGER_USED() {
    return new CacheExpiryPolicy<K, V>() {
      @Override
      public boolean shouldRun(RootObjectCache rootObjectCache) {
        return true;
      }

      @Override
      public boolean shouldExpire(RootObjectCache<K, V> rootCache, RevisionKeyList<K> rkl) {
        return rkl.getRevision() < getOldestActiveRevision(rootCache);
      }

      private long getOldestActiveRevision(RootObjectCache<K, V> rootObjectCache) {
        long lowestRevisionNumber = Long.MAX_VALUE;

        for (RevisionObjectCache cache : rootObjectCache.getActiveRevisions()) {
          lowestRevisionNumber = Math.min(lowestRevisionNumber, cache.getRevision());
        }

        return lowestRevisionNumber;
      }

    };
  }

  /**
   * Creates a new {@link CacheExpiryPolicy} that uses the {@code NO_LONGER_USED}
   * policy, but only runs it on every n'th call.
   *
   * @param n Number indicating each n'th call that the expiry policy should be called on.
   * @return The cache expiry policy.
   */
  public static <K, V> CacheExpiryPolicy<K, V> NO_LONGER_USED(final int n) {
    return new CacheExpiryPolicy<K, V>() {
      //noinspection unchecked not much we can do here but cheat...
      private CacheExpiryPolicy<K, V> instance = (CacheExpiryPolicy<K, V>) NO_LONGER_USED();
      private int count = 0;

      public boolean shouldRun(RootObjectCache<K, V> rootObjectCache) {
        count++;
        return count % n == 0;
      }

      public boolean shouldExpire(RootObjectCache<K, V> rootCache, RevisionKeyList<K> rkl) {
        return instance.shouldExpire(rootCache, rkl);
      }
    };
  }

  /**
   * Remove history that is older than a certain amount of time.
   */
  public static <K, V> CacheExpiryPolicy<K, V> TIME_BASED(final long longestAgeInMs) {
    return new CacheExpiryPolicy<K, V>() {
      @Override
      public boolean shouldRun(RootObjectCache<K, V> rootObjectCache) {
        return true;
      }

      @Override
      public boolean shouldExpire(RootObjectCache<K, V> rootCache, RevisionKeyList<K> rkl) {
        // NOTE : [AM] : This is gross I know, but it's the easiest (and fastest) way possible.
        return (System.currentTimeMillis() - rkl.getRevisionTime().getTime() > longestAgeInMs);
      }
    };
  }

  /**
   * Checks whether this {@link CacheExpiryPolicy} should be run for the specified
   * {@link RootObjectCache}.
   *
   * @param rootObjectCache The {@code RootObjectCache} instance to check.
   * @return {@code true} if the this {@code CacheExpiryPolicy} should be run, otherwise {@code false}.
   */
  public abstract boolean shouldRun(RootObjectCache<K, V> rootObjectCache);

  /**
   * Checks whether the provided {@link RevisionKeyList} should be expired from the provided
   * {@link RootObjectCache}.                                                                              
   *
   * @param rootCache The {@code RootObjectCache} instance to check.
   * @param rkl       The revision key list.
   * @return {@code true} if the key list should expire, otherwise {@code false}.
   */
  public abstract boolean shouldExpire(RootObjectCache<K, V> rootCache, RevisionKeyList<K> rkl);

}
