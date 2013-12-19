package com.github.pojomvcc.impl;

import com.github.pojomvcc.ObjectCacheException;
import com.github.pojomvcc.RefreshOptions;
import com.github.pojomvcc.RevisionObjectCache;
import com.github.pojomvcc.RootObjectCache;
import com.github.pojomvcc.util.RevisionObjectList;

import java.util.List;

/**
 * Default implementation of the {@link com.github.pojomvcc.RevisionObjectCache} implementation.
 *
 * @author Aidan Morgan
 */
public class RevisionObjectCacheImpl<K,V> implements RevisionObjectCache<K,V> {
  private RevisionObjectList<K,V> revisionList;
  private RootObjectCache<K,V> rootCache;
  private long revision;

  /**
   * Constructor.
   *
   * @param cache    the {@link com.github.pojomvcc.RootObjectCache} that owns this {@link com.github.pojomvcc.RevisionObjectCache}.
   * @param revision the revision that this {@link com.github.pojomvcc.RevisionObjectCache} is for.
   */
  public RevisionObjectCacheImpl(RootObjectCache<K,V> cache, long revision) {
    this.rootCache = cache;
    this.revision = revision;

    revisionList = new RevisionObjectList<K,V>(cache, revision);
  }

  /**
   * @inheritDoc
   */
  public long getRevision() {
    return revision;
  }

  /**
   * @inheritDoc
   */
  public RootObjectCache<K,V> getParentCache() {
    return rootCache;
  }

  /**
   * @inheritDoc
   */
  public V getElement(K key) {
    return revisionList.get(key);
  }

  /**
   * @inheritDoc
   */
  public boolean containsKey(K key) {
    return revisionList.containsKey(key);
  }

  /**
   * @inheritDoc
   */
  public int size() {
    return revisionList.size();
  }

  /**
   * @inheritDoc
   */
  public void addElement(K key, V object) {
    revisionList.add(key, object);
  }

  /**
   * @inheritDoc
   */
  public void removeElement(K key) {
    revisionList.remove(key);
  }

  /**
   * @inheritDoc
   */
  public List<K> getAddedElements() {
    return revisionList.getAddedElements();
  }

  /**
   * @inheritDoc
   */
  public List<K> getModifiedElements() {
    return revisionList.getModifiedElements();
  }

  /**
   * @inheritDoc
   */
  public List<K> getRemovedElements() {
    return revisionList.getRemovedElements();
  }

  public List<K> getKeys() {
    return revisionList.getKeys();
  }

  /**
   * @inheritDoc
   */
  public void close() {
    revisionList.clear();
    rootCache.close(this);
  }

  /**
   * @inheritDoc
   */
  public void update(RefreshOptions options) {
    
    if (!options.isValid()) {
      throw new ObjectCacheException("Invalid RefreshOptions provided. Must have one of replaceOnUpdate(), mergeOnUpdate() or ignoreOnUpdate() set.");
    }

    RevisionObjectList newList = new RevisionObjectList<K,V>(rootCache, rootCache.getRevision());

    // TODO: [MF] I still think that the way conflict resolution is handled is messy. Can this not be simplified somehow?

    for (K ce : rootCache.getKeys()) {
      if (revisionList.containsKey(ce)) {
        if (revisionList.isAdded(ce)) {
          if (options.failOnUpdateAdd()) {
            throw new ObjectCacheException("Error while refreshing: RootObjectCache has modified key " + ce + " but it is marked as added.");
          }
        }                                                                                                   

        if (revisionList.isModified(ce)) {
          if (options.failOnUpdateModify()) {
            throw new ObjectCacheException("Error while refreshing: RootObjectCache has modified key " + ce + " but it has also been modified.");
          }

          if (options.mergeOnUpdate()) {
            V fromRoot = rootCache.getElement(ce);
            V fromLocal = revisionList.get(ce);

            V merged = rootCache.getElementFactory().merge(fromRoot, fromLocal);
            newList.replace(ce, merged);
          } else if (options.replaceOnUpdate()) {
            newList.replace(ce, revisionList.get(ce));
          }
        }

        if (revisionList.isRemoved(ce)) {
          if (options.failOnUpdateRemoved()) {
            throw new ObjectCacheException("Error while refreshing: RootObjectCache has modified key " + ce + " but it has been removed.");
          }
        }
      }
    }
  }

  /**
   * @inheritDoc
   */
  public void revert() {
    revisionList.clear();
  }
  
}
