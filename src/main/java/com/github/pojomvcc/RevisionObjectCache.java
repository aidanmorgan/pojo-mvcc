package com.github.pojomvcc;

import java.util.List;

/**
 * A {@link RevisionObjectCache} is a modifiable view (or "working copy") of a {@link RootObjectCache}.
 * All additions/modifications/removals for {@code com.github.pojomvcc.CacheElement}s are performed against an
 * instance of this class.
 * <p/>
 * To persist the changes back into the {@link RootObjectCache} the {@link RootObjectCache#commit(RevisionObjectCache)}
 * operation should be invoked.
 *
 * @author Aidan Morgan
 */
public interface RevisionObjectCache<K,V> extends ReadOnlyRevisionObjectCache<K,V> {
  /**
   * Adds the provided {@code com.github.pojomvcc.CacheElement} with the provided {@code com.github.pojomvcc.CacheKey} to
   * this {@code com.github.pojomvcc.ObjectCache}.
   *
   * @param key
   * @param object
   */
  public void addElement(K key, V object);

  /**
   * Removes the {@code com.github.pojomvcc.CacheElement} currently registered with the provided {@code com.github.pojomvcc.CacheKey}
   * from this {@link ObjectCache}.
   *
   * @param key
   */
  public void removeElement(K key);

  /**
   * Returns a {@code List} of {@link K}s for all {@code com.github.pojomvcc.CacheElement}s that
   * were added to this {@link RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getAddedElements();

  /**
   * Returns a {@code List} of {@link K}s for all {@code com.github.pojomvcc.CacheElement}s that
   * were modified in this {@link RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getModifiedElements();

  /**
   * Returns a {@code List} of {@link K}s for all {@code com.github.pojomvcc.CacheElement}s that
   * were removed from this {@link RevisionObjectCache}.
   *
   * @return
   */
  public List<K> getRemovedElements();

  /**
   * Updates this {@link RevisionObjectCache} with the latest revision of the {@code com.github.pojomvcc.RootObjectCache}
   * automatically. The provided {@link RefreshOptions} dictates what should happen when
   * conflicts between this {@code com.github.pojomvcc.RevisionObjectCache} and the {@code com.github.pojomvcc.RootObjectCache}
   * occur.
   *
   * @param options
   */
  public void update(RefreshOptions options);

  /**
   * Removes all changes to this {@link RevisionObjectCache} and reverts back to the revision
   * in the {@link RootObjectCache}.
   */
  public void revert();

  /**
   * Closes and releases all resources used by this {@link RevisionObjectCache}.
   * <p/>
   * Implementations need to ensure that the {@link RootObjectCache#close(RevisionObjectCache)} method
   * is called as part of the implementation to ensure that resources are removed from the {@link RootObjectCache}.
   */
  public void close();

  /**
   * Returns the {@code java.util.List} of {@link K} that are in this {@link com.github.pojomvcc.RevisionObjectCache}.
   *
   * This method can be very slow to operate, so it should not be called often.
   *
   * @return
   */
  public List<K> getKeys();
}
