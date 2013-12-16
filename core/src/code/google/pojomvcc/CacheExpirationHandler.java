package code.google.pojomvcc;

/**
 * A {@link CacheExpirationHandler} is invoked when a {@link RevisionKeyList} has
 * been decided to be evicted by the {@link CacheExpiryPolicy}.
 * <p/>
 * This can be used for persistent backup etc. This is an <b>OPTIONAL</b> interface and does not have to
 * be implemented.
 *
 * @author Aidan Morgan
 */
public interface CacheExpirationHandler<K, V> {

  /**
   * Called when the provided {@link RevisionKeyList} is about to be evicted from the
   * {@link RootObjectCache}.
   * <p/>
   * Perform whatever operations are required, typically used for persisting the revision to some other
   * storage mechanism.
   *
   * @param rev the {@link RevisionKeyList} to test for expiration.
   */
  public void expired(RevisionKeyList<K> rev);

  /**
   * Called by the {@link RootObjectCache} when a specific revision of the
   * {@link code.google.pojomvcc.RootObjectCache} with the provided {@link K} cannot be found
   * in-memory.
   * <p/>
   * Implementations should return {@code null} if they cannot find the revision.
   *
   * @param key The key for the element to be retrieved.
   * @param revision The revision number.
   * @return The element revision.
   */
  public CacheElementRevision<K, V> retrieve(K key, long revision);
  
}
