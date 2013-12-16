package code.google.pojomvcc;

import java.util.List;
import java.util.Map;

/**
 * The {@link RootObjectCache} is the base-cache for all operations. The {@code code.google.pojomvcc.RootObjectCache}
 * does not allow modifications to be made to it directly. To make any modifications a new {@code code.google.pojomvcc.RevisionObjectCache}
 * must be created using the {@link RootObjectCache#checkout()} operation.
 * <p/>
 * This class is responsible for tracking all revisions of {@link V}s and is the single
 * entry-point into the object-caching engine.
 *
 * @author Aidan Morgan
 */
public interface RootObjectCache<K, V> extends ObjectCache<K, V> {
  /**
   * Returns a {@code java.util.List} of {@link K} which are the registered keys for the
   * provided revision of the cache.
   *
   * @param revision
   * @return
   */
  public List<K> getKeysForRevision(long revision);

  /**
   * Returns a {@code java.util.List} of {@link K}s for the current revision.
   *
   * @return
   * @srr {@link RootObjectCache#getKeysForRevision(long)}
   * @see {@link ObjectCache#getRevision()}
   */
  public List<K> getKeys();

  /**
   * Returns the {@link V} which has been registered with this {@code code.google.pojomvcc.RootObjectCache}
   * with the provided {@link K} at the provided revision.
   * <p/>
   * Depending on the {@link CacheExpiryPolicy} this method may return {@code null} even though
   * there was once a {@code V} registered with the provided {@code K}.
   *
   * @param revision
   * @param key
   * @return
   * @see CacheExpiryPolicy
   */
  public V getElementWithRevision(long revision, K key);

  /**
   * Returns the {@code VFactory} that is used for all cloning and merging operations.
   *
   * @return
   */
  public CacheElementFactory<V> getElementFactory();

  /**
   * Creates a "checkout" of this {@code code.google.pojomvcc.RootObjectCache}. A checkout is a snapshot (or "working copy")
   * of this cache at the current revision.
   * <p/>
   * The returned {@code RevisionObjectCache} can be modified (add, delete modify {@link V}),
   * but for the changes to be made available to other callers the {@link RootObjectCache#commit(RevisionObjectCache)}
   * method must be called.
   * <p/>
   * All modifications made in the returned {@code code.google.pojomvcc.RevisionObjectCache} are independent of this
   * {@code code.google.pojomvcc.RootObjectCache} and will not be updated, unless a {@link RevisionObjectCache#update(RefreshOptions)}
   * is invoked.
   *
   * @return
   */
  public RevisionObjectCache<K, V> checkout();

  /**
   * Creates a "checkout" of this {@link RootObjectCache}, but is considered read-only. No changes can be
   * made to the returned {@link ReadOnlyRevisionObjectCache} and it is not tracked by the {@link RootObjectCache}.
   *
   * @return
   */
  public ReadOnlyRevisionObjectCache<K, V> export();

  /**
   * Committs all changes in the provided {@code code.google.pojomvcc.RevisionObjectCache} to this {@code code.google.pojomvcc.RootObjectCache},
   * that is, all added, removed and modified {@code V}s are updated and the revision
   * number is incremented.
   *
   * @param cache
   */
  public void commit(RevisionObjectCache<K, V> cache);

  /**
   * Defines the {@code code.google.pojomvcc.CacheExpiry} that defines how and when {@code V}s
   * history should be removed from the {@code code.google.pojomvcc.RootObjectCache}.
   *
   * @param pol
   */
  public void setCacheExpiry(CacheExpiry<K, V> pol);

  /**
   * Returns the currently active {@link CacheExpiry} that is being used by the
   * {@link RootObjectCache}.
   *
   * @return
   */
  public CacheExpiry<K, V> getCacheExpiry();

  /**
   * Callback from a {@link code.google.pojomvcc.impl.RevisionObjectCacheImpl} that it is no longer being updated and therefore
   * should have all resources allocated to it removed.
   *
   * @param revisionObjectCache
   */
  public void close(RevisionObjectCache<K, V> revisionObjectCache);

  /**
   * Returns a {@code List} of {@link RevisionObjectCache}s that are currently active (not closed).
   *
   * @return
   * @see RevisionObjectCache#close()
   */
  public List<RevisionObjectCache<K, V>> getActiveRevisions();

  /**
   * Returns a {@code java.util.Map} which is a simple mechanism for getting the current revision in a
   * read-only manner.
   * <p/>
   * The returned {@code java.util.Map} is read-only and cannot be modified in any way.
   *
   * @return
   */
  public Map<K, V> asMap();

  /**
   * Returns {@code true} if this {@link RootObjectCache} contains the provided
   * {@link K} at the provided revision.
   *
   * @param revision
   * @param keyForIndex
   * @return
   */
  public boolean containsKey(long revision, K keyForIndex);
}
