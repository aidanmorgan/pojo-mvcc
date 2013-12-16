package code.google.pojomvcc.util;

import code.google.pojomvcc.ObjectCacheException;
import code.google.pojomvcc.RootObjectCache;

import java.util.*;

/**
 * A {@code code.google.pojomvcc.util.RevisionObjectList} allows the presentation of a set of changes to
 * appear as if they are a {@code java.lang.Iterable} for use.
 * <p/>
 * Provides mechanisms for tracking all added, modified and removed elements from the original
 * {@code code.google.pojomvcc.RootObjectCache}, without cloning all {@code V}s
 * in the {@code code.google.pojomvcc.RootObjectCache}.
 * <p/>
 * When a {@code V} is retrieved from this {@code code.google.pojomvcc.util.RevisionObjectList}
 * a clone of the original {@code V} in the {@code code.google.pojomvcc.RootObjectCache} is
 * created. All subsequent calls to the {@code get} methods will return the same clone.
 *
 * @author Aidan Morgan
 */
public class RevisionObjectList<K, V> implements Iterable<V> {
  /**
   * Version number for this list, incremented whenever a modification is made for the list. Used for generating a
   * {@code java.util.ConcurrentModificationException} if a {@code java.util.Iterator} has been allocated.
   */
  private long internalListVersion = 0;

  /**
   * A {@link java.util.List} of {@link K}s that have been copied from the
   * {@code code.google.pojomvcc.RootObjectCache}. This is a direct {@link java.util.List} copy as seperate {@code java.util.List}s
   * are maintained for each revision in the {@code code.google.pojomvcc.RootObjectCache} and therefore there is no need
   * to worry about external modification.
   */
  private List<K> coreKeys;

  /**
   * A {@code java.util.List} of {@code K}s for all {@code V}s that
   * have been added to this {@code code.google.pojomvcc.RevisionObjectCache}. The order of this is important.
   */
  private List<K> addedElementKeys;

  /**
   * A {@code java.util.List} of {@code K}s for all {@code V}s that
   * have been removed from this {@code code.google.pojomvcc.RevisionObjectCache}.
   */
  private List<K> removedElementKeys;

  /**
   * A {@code java.util.List} of {@code K}s for all {@code V}s that
   * have been retrieved from this {@code code.google.pojomvcc.RevisionObjectCache}. Although they have been retrieved does not
   * necessarily mean that the {@code V} has been modified, this is the best we can do.
   */
  private List<K> clonedElementKeys;

  /**
   * A {@link java.util.Map} of {@code K} to {@code V} that contains
   * the added and cloned {@code V}s for this list.
   */
  private Map<K, V> internalMap;

  /**
   * The {@code code.google.pojomvcc.RootObjectCache} that owns this {@code code.google.pojomvcc.util.RevisionObjectList}.
   */
  private RootObjectCache<K, V> rootCache;

  /**
   * The revision from the {@code code.google.pojomvcc.RootObjectCache} that this {@code code.google.pojomvcc.util.RevisionObjectList}
   * is operating on.
   */
  private long revision;

  /**
   * Constructor.
   *
   * @param cache    the {@code code.google.pojomvcc.RootObjectCache} that owns this {@code code.google.pojomvcc.util.RevisionObjectList}.
   * @param revision the revision from the {@code code.google.pojomvcc.RootObjectCache} that this {@code code.google.pojomvcc.util.RevisionObjectList}
   *                 is for.
   */
  public RevisionObjectList(RootObjectCache<K, V> cache, long revision) {
    this.rootCache = cache;
    this.revision = revision;

    // CacheKey's are immutable so we don't need to worry about cloning their individual values
    // just the list itself.
    this.coreKeys = new ArrayList<K>(cache.getKeysForRevision(revision));

    this.addedElementKeys = new ArrayList<K>();
    this.removedElementKeys = new ArrayList<K>();
    this.clonedElementKeys = new ArrayList<K>();
    this.internalMap = new HashMap<K, V>();
  }

  /**
   * Returns the size of the list. This size will include any added or removed {@code V}s.
   *
   * @return
   */
  public int size() {
    return coreKeys.size() + addedElementKeys.size() - removedElementKeys.size();
  }

  /**
   * Returns {@code true} if this {@code code.google.pojomvcc.util.RevisionObjectList} is empty, {@code false} otherwise.
   *
   * @return
   */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Returns {@code true} if this {@link RevisionObjectList} contains the provided
   * {@link K}.
   *
   * @param key
   * @return
   */
  public boolean containsKey(K key) {
    return addedElementKeys.contains(key) || (!removedElementKeys.contains(key) && coreKeys.contains(key));
  }

  /**
   * Returns a {@code java.util.Iterator} over the {@link V}s that are in this
   * {@link RevisionObjectList}.
   *
   * @return
   */
  public Iterator<V> iterator() {
    return new RevisionObjectListIterator<V>(this);
  }

  /**
   * Adds the provided {@link V} to this {@link RevisionObjectList}.
   *
   * @param cacheElement
   * @return
   */
  public boolean add(K key, V cacheElement) {
    boolean added = addedElementKeys.add(key);

    if (added) {
      internalListVersion++;
      internalMap.put(key, cacheElement);
    }

    return added;
  }

  /**
   * Removes the {@link V} with the provided {@link K}.
   *
   * @param cacheKey
   * @return
   */
  public boolean remove(K cacheKey) {
    if (addedElementKeys.contains(cacheKey)) {
      addedElementKeys.remove(cacheKey);
      internalMap.remove(cacheKey);

      internalListVersion++;
      return true;
    }

    if (removedElementKeys.contains(cacheKey)) {
      return false;
    }

    if (removedElementKeys.add(cacheKey)) {
      if (clonedElementKeys.contains(cacheKey)) {
        clonedElementKeys.remove(cacheKey);
      }

      internalListVersion++;
      return true;
    }

    return false;
  }

  /**
   * Clears out the contents of this {@link RevisionObjectList}.
   */
  public void clear() {
    addedElementKeys.clear();
    removedElementKeys.clear();
    clonedElementKeys.clear();
    internalMap.clear();

    internalListVersion++;
  }

  /**
   * Returns the {@link V} at the provided index in this {@link RevisionObjectList}.
   *
   * @param index
   * @return
   */
  public V get(int index) {
    K keyForIndex;

    if (index < coreKeys.size()) {
      keyForIndex = coreKeys.get(index);

      while (removedElementKeys.contains(keyForIndex)) {
        index++;
        keyForIndex = coreKeys.get(index);
      }
    } else {
      keyForIndex = addedElementKeys.get(index - coreKeys.size());
    }

    if (keyForIndex == null) {
      throw new ObjectCacheException("Cannot find CacheElement at index " + index + ".");
    }

    return getOrClone(keyForIndex);
  }

  /**
   * Internal helper method to either return the {@link V} with the provided
   * {@link K} that is in the {@link RevisionObjectList#internalMap}
   * or clones the provided {@link V} (using the {@link code.google.pojomvcc.CacheElementFactory}
   * registered with the {@link code.google.pojomvcc.RootObjectCache}) to return (the clone is also inserted into
   * the {@link RevisionObjectList#internalMap} for future reference).
   * <p/>
   * If the provided {@link K} has been removed then this method will return {@code null}.
   *
   * @param keyForIndex
   * @return
   */
  private V getOrClone(K keyForIndex) {
    if (keyForIndex == null) {
      throw new ObjectCacheException("Cannot return CacheElement for a null CacheKey.");
    }

    if (removedElementKeys.contains(keyForIndex)) {
      return null;
    }

    // check the internal map to see if we have already cloned the element or if it has been added.
    if (internalMap.containsKey(keyForIndex)) {
      return internalMap.get(keyForIndex);
    } else {
      // we can't find the element in the root cache, so throw an exception. The element should be in the root
      // cache if we're trying to find it...
      if (!rootCache.containsKey(revision, keyForIndex)) {
        throw new ObjectCacheException("Cannot find CacheElement with key " + keyForIndex + " in root cache at revision " + revision + ".");
      }

      // the element hasn't been removed or added, and is not in the internal map, which means we must ask
      // the root cache for the element and clone it.
      V originalElement = rootCache.getElementWithRevision(revision, keyForIndex);

      // clone the element here to prevent caller's modifying the instance that is in the core
      // object cache.
      V clone = rootCache.getElementFactory().createClone(originalElement);


      // store the clone in an internal map to ensure that repeated calls to this method will
      // return the same instance.
      internalMap.put(keyForIndex, clone);
      clonedElementKeys.add(keyForIndex);

      return clone;
    }
  }

  /**
   * Returns the index of the {@link V} with the provided {@link K}.
   *
   * @param key
   * @return
   */
  public int getKeyIndex(K key) {
    if (removedElementKeys.contains(key)) {
      return -1;
    }

    if (addedElementKeys.contains(key)) {
      return addedElementKeys.indexOf(key) + (coreKeys.size() - removedElementKeys.size());
    }

    int index = coreKeys.indexOf(key);

    if (index < 0) {
      return -1;
    }

    while (index < coreKeys.size()) {
      K ele = coreKeys.get(index);

      if (removedElementKeys.contains(ele)) {
        index++;
      } else if (ele.equals(key)) {
        return index;
      }
    }

    throw new ObjectCacheException("Cannot find key " + key + " in added, removed or original list.");
  }

  /**
   * Returns the {@link V} with the provided {@link V}.
   *
   * @param key
   * @return
   */
  public V get(K key) {
    int index = getKeyIndex(key);

    if (index >= 0) {
      return get(index);
    }

    return null;
  }

  /**
   * Returns a {@code java.util.List} of {@link K} that are the keys for any modified
   * {@link V} in this {@link code.google.pojomvcc.RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getModifiedElements() {
    return Collections.unmodifiableList(clonedElementKeys);
  }

  /**
   * Returns a {@code java.util.List} of {@link K} that are the keys for any added
   * {@link V} in this {@link code.google.pojomvcc.RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getAddedElements() {
    return Collections.unmodifiableList(addedElementKeys);
  }

  /**
   * Returns a {@code java.util.List} of {@link K} that are the keys for any removed
   * {@link V} in this {@link code.google.pojomvcc.RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getRemovedElements() {
    return Collections.unmodifiableList(removedElementKeys);
  }

  /**
   * Returns {@code true} if the provided {@link K} belongs to an added {@link V}.
   *
   * @param ce
   * @return
   */
  public boolean isAdded(K ce) {
    return addedElementKeys.contains(ce);
  }

  /**
   * Returns {@code true} if the provided {@link K} belongs to a modified {@link V}.
   *
   * @param ce
   * @return
   */
  public boolean isModified(K ce) {
    return clonedElementKeys.contains(ce);
  }

  /**
   * Returns {@code true} if the provided {@link K} belongs to a removed {@link V}.
   *
   * @param ce
   * @return
   */
  public boolean isRemoved(K ce) {
    return removedElementKeys.contains(ce);
  }

  /**
   * Replaces the {@link V} with the provided {@link K} with
   * the provided {@link V}.
   *
   * @param ce
   * @param merged
   */
  public void replace(K ce, V merged) {
    clonedElementKeys.add(ce);                                                      
    internalMap.put(ce, merged);
    internalListVersion++;
  }

  /**
   * Returns a {@code List} of {@link K} which contains all of the keys currently in this {@link code.google.pojomvcc.util.RevisionObjectList}/
   *
   * @return
   */
  public List<K> getKeys() {
    List<K> currentKeys = new ArrayList<K>(clonedElementKeys);
    currentKeys.addAll(addedElementKeys);
    currentKeys.removeAll(removedElementKeys);

    return currentKeys;
  }

  /**
   * Inner class that implements {@code java.util.Iterator} for traversing this {@link RevisionObjectList}.
   */
  private class RevisionObjectListIterator<V> implements Iterator<V> {
    /**
     * The version of the {@link RevisionObjectList} that this
     * {@code java.util.Iterator} was created for.
     */
    private long iteratorVersion = -1;
    private int index = 0;

    public RevisionObjectListIterator(RevisionObjectList<K, V> list) {
      iteratorVersion = list.internalListVersion;
    }

    public boolean hasNext() {
      checkForComod();

      return index < size();
    }

    public V next() {
      checkForComod();

      @SuppressWarnings({"unchecked"}) // got no real choice here but to suppress. we know it is of the right type...
          V ele = (V) get(index);
      index++;
      return ele;
    }

    public void remove() {
      throw new UnsupportedOperationException("code.google.pojomvcc.util.RevisionObjectList.RevisionObjectListIterator.remove");
    }

    /**
     * Determines if the {@code code.google.pojomvcc.util.RevisionObjectList} underlying this {@code java.util.Iterator} has
     * changed since it was created. If so, throws a {@code java.util.ConcurrentModificationException}.
     */
    private void checkForComod() {
      if (internalListVersion != iteratorVersion) {
        throw new ConcurrentModificationException("RevisionObjectList has changed since iterator was taken.");
      }
    }
  }
}
