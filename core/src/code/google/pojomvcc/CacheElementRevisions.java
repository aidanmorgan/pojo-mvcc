package code.google.pojomvcc;

import java.util.*;

/**
 * Class that contains all revisions for an {@code V}.
 *
 * @author Aidan Morgan
 */
public class CacheElementRevisions<K, V> {
  private static final Comparator<Long> REVISION_COMPARATOR = new AscendingRevisionComparator();

  /**
   * The {@link K} this set of {@link CacheElementRevisions} is for.
   */
  private K cacheKey;

  /**
   * The {@code java.util.SortedMap} of {@link CacheElementRevision}s that make up this revision
   * history.
   */
  private SortedMap<Long, CacheElementRevision<K, V>> revisions;

  /**
   * A reference to the {@link RootObjectCache} for this revision history.
   */
  private RootObjectCache<K, V> rootObjectCache;

  /**
   * Constructor.
   *
   * @param rootObjectCache the {@link RootObjectCache} that will own this revision history.
   * @param oid             the {@link K} of the {@link V} that this
   *                        class is keeping revision history for.
   */
  public CacheElementRevisions(RootObjectCache<K, V> rootObjectCache, K oid) {
    this.rootObjectCache = rootObjectCache;
    this.revisions = new TreeMap<Long, CacheElementRevision<K, V>>(REVISION_COMPARATOR);
    this.cacheKey = oid;
  }

  /**
   * Returns the leading (latest) revision of the enclosed {@link V}.
   *
   * @return
   */
  public V getLeading() {
    Long lastRevision = revisions.lastKey();
    return revisions.get(lastRevision).getElement();
  }

  /**
   * Returns the {@link V} with the enclosed {@link K} at the provided revision.
   * <p/>
   * This works by moving through the recorded revision history and finding the {@link CacheElementRevision}
   * in the internal store that has the highest revision that is less than the provided revision. This allows the storage
   * of deltas-only whilst being able to retrieve the object state at a specific revision.
   *
   * @param revision
   * @return
   */
  public V get(long revision) {
    if (revisions.isEmpty()) {
      throw new ObjectCacheException("There are no revisions in this CacheElementRevisions. This is a bug, there should be at least one.");
    }

    CacheElementRevision<K, V> rev = null;

    // if the key is in the map just return it straight away.
    if (revisions.containsKey(revision)) {
      rev = revisions.get(revision);
    } else {
      // head map will return a SortedMap of all values that have a revision STRICTLY less than the
      // value provided.
      SortedMap<Long, CacheElementRevision<K, V>> vals = revisions.headMap(revision);
      if (null != vals && !vals.isEmpty()) {
        rev = vals.get(vals.firstKey());
      }
    }

    // If the revision element was not found in memory, then we need to go to the CacheExpirationHandler and beg for
    // it (nicely of course).
    if (null == rev) {
      rev = rootObjectCache.getCacheExpiry().getHandler().retrieve(cacheKey, revision);
    }

    // if the revision is null at this point there is nothing we can do, it's not in the list.
    if (null == rev) {
      return null;
    }

    // if the revision has been deleted then we return null and pretend it never existed.
    if (CacheRevisionType.DELETED == rev.getState()) {
      return null;
    }

    return rev.getElement();
  }

  /**
   * Returns the {@link K} for the enclosed {@link V}.
   *
   * @return
   */
  public K getCacheKey() {
    return cacheKey;
  }

  /**
   * Adds the provided {@link V} to the revision history at the provided revision.
   * <p/>
   * Will record the modification as a {@link CacheRevisionType#ADDED}.
   *
   * @param l   the revision of the {@link RootObjectCache} that the provided {@link V} is added at.
   * @param key the {@link K} of the {@link V} to add.
   * @param ele the {@link V} to add.
   */
  public void addElement(long l, K key, V ele) {
    if (null == ele) {
      throw new ObjectCacheException("Element cannot be null.");
    }

    CacheElementRevision<K, V> cr = new CacheElementRevision<K, V>(key, ele, l, CacheRevisionType.ADDED);
    revisions.put(l, cr);
  }

  /**
   * Adds the provided {@link V} to the revision history at the provided revision.
   * <p/>
   * Will record the modification as a {@link CacheRevisionType#MODIFIED}.
   *
   * @param l
   * @param ele
   */
  public void addModification(long l, K key, V ele) {
    if (null == ele) {
      throw new ObjectCacheException("Element cannot be null.");
    }

    CacheElementRevision<K, V> cr = new CacheElementRevision<K, V>(key, ele, l, CacheRevisionType.MODIFIED);
    revisions.put(l, cr);
  }

  /**
   * Adds the provided {@link V} to the revision history at the provided revision.
   * <p/>
   * Will record the modification as a {@link CacheRevisionType#DELETED}.
   *
   * @param l
   * @param ele
   */
  public void removeElement(long l, K ele) {
    if (ele == null) {
      throw new ObjectCacheException("CacheKey for removal cannot be null.");
    }

    CacheElementRevision<K, V> cr = new CacheElementRevision<K, V>(ele, null, l, CacheRevisionType.DELETED);
    revisions.put(l, cr);
  }

  /**
   * Removes all of the provided {@link CacheElementRevision} from this history.
   *
   * @param remove
   */
  public void removeRevisions(List<CacheElementRevision<K, V>> remove) {
    for (CacheElementRevision<K, V> rev : remove) {
      revisions.remove(rev.getRevision());
    }
  }

  /**
   * Returns the {@code java.util.List} of {@link CacheElementRevision} that are the
   * history of the enclosed {@link V}.
   *
   * @return
   */
  public Iterator<CacheElementRevision<K, V>> getRevisions() {
    return new Iterator<CacheElementRevision<K, V>>() {

      Iterator<Map.Entry<Long, CacheElementRevision<K, V>>> itr = revisions.entrySet().iterator();

      public boolean hasNext() {
        return itr.hasNext();
      }

      public CacheElementRevision<K, V> next() {
        return itr.next().getValue();
      }

      public void remove() {
        throw new UnsupportedOperationException("Removes are not allowed.");
      }
    };
  }

  /**
   * Removes the revision history for the provided revision.
   *
   * @param revision
   */
  public void removeRevision(long revision) {
    if (!revisions.containsKey(revision)) {
      return;
    }

    // if there is more than one revision in here
    if (revisions.size() > 1) {
      // and the revision we are wanting to remove is not the latest revision
      if (revisions.get(revision).getRevision() > revision) {
        revisions.remove(revision);
      }
    }
    // we have only one revision (which is typically the minimum case)
    else if (revisions.size() == 1) {
      CacheElementRevision rev = revisions.get(revision);

      if (null != rev) {
        // if the revision was deleted then we are okay to evict the revision history
        // and just pretend it never existed.
        if (rev.getRevisionType() == CacheRevisionType.DELETED) {
          revisions.remove(revision);
        }
      }
    }
  }

  public boolean containsRevision(long revision) {
    return get(revision) != null;
  }

  /**
   * Returns the number of revisions that are being tracked by this {@link CacheElementRevision}.
   *
   * @return
   */
  public int size() {
    return revisions.size();
  }

  public boolean isDeleted() {
    Long lastRevision = revisions.lastKey();
    return revisions.get(lastRevision).getState() == CacheRevisionType.DELETED;

  }

  /**
   * Implementation of the {@code java.util.Comparator} interface for comparing revision numbers.
   */
  private static class AscendingRevisionComparator implements Comparator<Long> {
    public int compare(Long o1, Long o2) {
      return o1.compareTo(o2);
    }
  }
}
