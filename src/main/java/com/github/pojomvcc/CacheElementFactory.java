package com.github.pojomvcc;

/**
 * A {@link CacheElementFactory} is responsible for creating clones of existing
 * {@link V} instances and also providing the merge behaviour when the
 * {@link RootObjectCache} needs to merge back to a {@link V}.
 * <p/>
 * Implementations of this interface should be relatively performant as they will
 * potentially hold the {@link com.github.pojomvcc.RootObjectCache)'s
 * {@code java.util.concurrent.locks.ReadWriteLock} and therefore prevent other operations
 * against the cache from being performed if used in a multi-threaded environment.
 *
 * @author Aidan Morgan
 */
public interface CacheElementFactory<V> {
  /**
   * Creates a clone of the provided {@link V}. This should be an exact duplicate of
   * the provided {@link V} and ideally should be a deep-copy.
   *
   * @param ele The {@link V} instance to clone.
   * @return The cloned {@link V} instance.
   */
  public V createClone(V ele);

  /**
   * Merges two {@link V} instances into a single {@code V}.
   * <p/>
   * It is up to implementers to decide how this merge operation should be conducted depending on their specific
   * requirements.
   *
   * @param inRepository The {@link V} that is the current up-to-date element
   *                     from the {@link RootObjectCache}.
   * @param changes      the {@link V} that is taken from the
   *                     {@link RevisionObjectCache} and is to have its changes merged in.
   * @return The merged {@code CacheElement} instance.
   */
  public V merge(V inRepository, V changes);
}
