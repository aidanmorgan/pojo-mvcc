package code.google.pojomvcc.impl;

import code.google.pojomvcc.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@code RootObjectCache} is the root of the {@code ObjectCache} cache heirarchies.
 * <p/>
 * A {@code RootObjectCache} is not modifyable directly, a {@code RevisionObjectCache} must be created
 * (using the {@link RootObjectCacheImpl#checkout()} method) and the changes made to that.
 *
 * @author Aidan Morgan
 */
public class RootObjectCacheImpl<K, V> implements RootObjectCache<K, V> {
  /**
   * The current revision number of this store.
   */
  private final AtomicLong CURRENT_REVISION = new AtomicLong(0);

  /**
   * The {@code java.util.concurrent.locks.ReadWriteLock} that ensures safe access to this cache from
   * multiple threads.
   */
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  /**
   * A {@code List} of currently active {@code code.google.pojomvcc.RevisionObjectCache}s.
   */
  private List<RevisionObjectCache<K, V>> openRevisionCaches;
  /**
   * A {@code java.util.Map} that stores a {@code K} with the {@code code.google.pojomvcc.impl.ObjectRevisions}
   * that track the history of the objects.
   */
  private Map<K, CacheElementRevisions<K, V>> revisions;

  /**
   * A {@code java.util.Map} of revision to a {@code java.util.List} of {@link K} that are the keys
   * in the {@code code.google.pojomvcc.RootObjectCache} for that revision.
   */
  private Map<Long, RevisionKeyList<K>> keysPerRevisionMap;

  /**
   * The {@link CacheElementFactory<V>} that is used for creating clones and merging {@link V}s.
   */
  private CacheElementFactory<V> factory;

  /**
   * The {@link code.google.pojomvcc.CacheExpiry} that determines how long historical information about
   * {@link V}s should be retained. It also can provide a mechanism for
   * handling expired {@link code.google.pojomvcc.RevisionKeyList}s so that that can be retrieved later on.
   */
  private CacheExpiry<K, V> cacheExpiryPolicy = CacheExpiry.DEFAULT();

  /**
   * Constructor.
   *
   * @param factory the {@link CacheElementFactory<V>} that is used for creating clones and
   *                merging {@link V}s.
   */
  public RootObjectCacheImpl(CacheElementFactory<V> factory) {
    this.openRevisionCaches = new ArrayList<RevisionObjectCache<K, V>>();
    this.revisions = new HashMap<K, CacheElementRevisions<K, V>>();
    this.keysPerRevisionMap = new HashMap<Long, RevisionKeyList<K>>();
    this.keysPerRevisionMap.put(0L, new RevisionKeyList<K>(0L));

    this.factory = factory;
  }

  /**
   * @inheritDoc
   */
  public List<K> getKeysForRevision(long revision) {
    try {
      readWriteLock.readLock().lock();

      if (revision > CURRENT_REVISION.get()) {
        throw new ObjectCacheException("Attempting to access revision " + revision + " which is > current head " + CURRENT_REVISION.get());
      }

      if (!keysPerRevisionMap.containsKey(revision)) {
        throw new ObjectCacheException("Cannot get keys for revision " + revision + ". Current revision is " + CURRENT_REVISION.get() + ".");
      }

      return Collections.unmodifiableList(keysPerRevisionMap.get(revision).getKeys());
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public List<K> getKeys() {
    try {
      readWriteLock.readLock().lock();
      return getKeysForRevision(CURRENT_REVISION.get());
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public V getElementWithRevision(long revision, K key) {
    try {
      readWriteLock.readLock().lock();

      if (revision > CURRENT_REVISION.get()) {
        throw new ObjectCacheException("Attempting to access revision " + revision + " which is > current head " + CURRENT_REVISION.get());
      }

      CacheElementRevisions<K, V> revs = revisions.get(key);

      if (revs != null) {
        return revs.get(revision);
      }

      return null;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public CacheElementFactory<V> getElementFactory() {
    return factory;
  }

  /**
   * @inheritDoc
   */
  public long getRevision() {
    try {
      readWriteLock.readLock().lock();
      return CURRENT_REVISION.get();
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public V getElement(K key) {
    return getElementWithRevision(CURRENT_REVISION.get(), key);
  }

  /**
   * @inheritDoc
   */
  public RevisionObjectCache<K, V> checkout() {
    try {
      readWriteLock.readLock().lock();

      RevisionObjectCache<K, V> impl = new RevisionObjectCacheImpl<K, V>(this, CURRENT_REVISION.get());
      openRevisionCaches.add(impl);
      return impl;
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public ReadOnlyRevisionObjectCache<K, V> export() {
    try {
      readWriteLock.readLock().lock();

      return new RevisionObjectCacheImpl<K, V>(this, CURRENT_REVISION.get());
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public int size() {
    return keysPerRevisionMap.get(CURRENT_REVISION.get()).size();
  }

  /**
   * @inheritDoc
   */
  public void commit(RevisionObjectCache<K, V> cache) {
    try {
      readWriteLock.writeLock().lock();
      long revision = CURRENT_REVISION.incrementAndGet();

      // this is the set of keys associated with a revision.
      List<K> keysForRevision = keysPerRevisionMap.get(revision - 1).getKeys();
      List<K> cache_keys = new ArrayList<K>(keysForRevision.size() + cache.getAddedElements().size());
      cache_keys.addAll(keysForRevision);


      for (K key : cache.getAddedElements()) {
        assert !revisions.containsKey(key);

        CacheElementRevisions<K, V> revs = new CacheElementRevisions<K, V>(this, key);
        V added = cache.getElement(key);

        if (added == null) {
          throw new ObjectCacheException("Added CacheElement is null.");
        }

        V addedClone = factory.createClone(added);
        revs.addElement(revision, key, addedClone);

        // new item, so update the cache keys.
        cache_keys.add(key);

        // new item so need to create an CacheElementRevisions object for it.
        revisions.put(key, revs);
      }

      for (K key : cache.getModifiedElements()) {
        V element = cache.getElement(key);
        CacheElementRevisions<K, V> revs = revisions.get(key);

        assert revs != null;

        V merged = factory.createClone(factory.merge(revs.getLeading(), element));
        revs.addModification(revision, key, merged);
      }

      for (K key : cache.getRemovedElements()) {
        CacheElementRevisions<K, V> revs = revisions.get(key);

        assert revs != null;

        revs.removeElement(revision, key);

        cache_keys.remove(key);
        revisions.put(key, revs);
      }

      keysPerRevisionMap.put(revision, new RevisionKeyList<K>(revision, cache_keys));
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Handles the expiry of {@link code.google.pojomvcc.RevisionKeyList}s from this {@link code.google.pojomvcc.RootObjectCache}.
   * Will use the {@link code.google.pojomvcc.CacheExpiry} registered to check which revisions should be evicted
   * from memory.
   */
  private void expire() {
    try {
      readWriteLock.writeLock().lock();

      if (cacheExpiryPolicy != null) {
        if (!cacheExpiryPolicy.getPolicy().shouldRun(this)) {
          return;
        }

        List<RevisionKeyList<K>> keysToKill = new ArrayList<RevisionKeyList<K>>();
        for (RevisionKeyList<K> rkl : keysPerRevisionMap.values()) {
          // make sure we don't somehow drop the current revision
          if (rkl.getRevision() != CURRENT_REVISION.get()) {
            if (cacheExpiryPolicy.getPolicy().shouldExpire(this, rkl)) {
              keysToKill.add(rkl);
            }
          }
        }

        // we now have a set of RevisionKeyList(s) that should be evicted from the cache.
        // go through and evict them from memory.
        for (RevisionKeyList<K> rkl : keysToKill) {
          for (K ck : rkl.getKeys()) {
            CacheElementRevisions<K, V> revs = revisions.get(ck);
            // the CacheElementRevision won't necessarily remove the revision when we call this
            // there are cases in which the revision can't be removed.
            // @see CacheElementRevisions#remove(long) for more information
            revs.removeRevision(rkl.getRevision());
          }

          // remove all traces of the revision from the store
          keysPerRevisionMap.remove(rkl.getRevision());

          // optionally provide some mechanism for handling the expired revisions, probably by writing
          // them to disk, or a database, or something.
          cacheExpiryPolicy.getHandler().expired(rkl);
        }
      }
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public void close(RevisionObjectCache<K, V> cache) {
    try {
      readWriteLock.writeLock().lock();
      this.openRevisionCaches.remove(cache);

      // a dependent cache has been closed, so lets check for any values that can be removed from the
      // cache.
      expire();
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public List<RevisionObjectCache<K, V>> getActiveRevisions() {
    try {
      readWriteLock.readLock().lock();
      return Collections.unmodifiableList(openRevisionCaches);
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public void setCacheExpiry(CacheExpiry<K, V> pol) {
    this.cacheExpiryPolicy = pol;
  }

  /**
   * @inheritDoc
   */
  public CacheExpiry<K, V> getCacheExpiry() {
    return cacheExpiryPolicy;
  }

  /**
   * @inheritDoc
   */
  public boolean containsKey(long revision, K keyForIndex) {
    try {
      readWriteLock.readLock().lock();
      return revisions.get(keyForIndex).containsRevision(revision);
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * @inheritDoc
   */
  public boolean containsKey(K key) {
    try {
      readWriteLock.readLock().lock();
      return revisions.containsKey(key);
    }
    finally {
      readWriteLock.readLock().unlock();
    }
  }

  /**
   * Returns a {@code java.util.Map} which is a simple mechanism for getting the current revision.
   * <p/>
   * The returned {@code java.util.Map} is read-only and cannot be modified in any way.
   */
  public Map<K, V> asMap() {
    try {
      readWriteLock.writeLock().lock();
      return new RevisionObjectCacheMap<K, V>(this);
    }
    finally {
      readWriteLock.writeLock().unlock();
    }
  }
}
