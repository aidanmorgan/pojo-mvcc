package code.google.pojomvcc;

import java.util.Comparator;

/**
 * Represents a revision of for a {@link V} with a specific {@link K}.
 * <p/>
 * This {@code CacheElementRevision} is stored in a {@link CacheElementRevision} entity for a specific {@link K}.
 * <p/>
 * Two instances of {@code CacheElementRevision} are considered equal if both have the same revision
 * history and {@link K} key.
 *
 * @author Aidan Morgan
 */
public class CacheElementRevision<K, V> {
  /**
   * Implementation of {@code java.util.Comparator} that compares {@link CacheElementRevision}.
   */
  public static Comparator<CacheElementRevision> COMPARATOR = new CacheElementRevisionComparator();

  private K key;
  private V ele;
  private CacheRevisionType revisionType;
  private long revision;

  /**
   * Constructor.
   *
   * @param key          the {@link K} of the {@link V} to add.
   * @param ele          the {@link V} to add to the revisions.
   * @param l            the revision number of the revisions.
   * @param revisionType the {@link CacheRevisionType} of the revision.
   */
  public CacheElementRevision(K key, V ele, long l, CacheRevisionType revisionType) {
    this.key = key;
    this.ele = ele;
    this.revision = l;
    this.revisionType = revisionType;
  }

  /**
   * Returns the {@link V} for this revision.
   *
   * @return the {@link V} for this revision.
   */
  public V getElement() {
    return ele;
  }

  /**
   * Returns the revision of this {@link CacheElementRevision}.
   *
   * @return the revision of this {@link CacheElementRevision}.
   */
  public long getRevision() {
    return revision;
  }

  /**
   * Returns the {@link CacheRevisionType} of this revision.
   *
   * @return the {@link CacheRevisionType} of this revision.
   */
  public CacheRevisionType getRevisionType() {
    return revisionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CacheElementRevision that = (CacheElementRevision) o;

    return revision == that.revision && key.equals(that.key) && revisionType == that.revisionType;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + revisionType.hashCode();
    result = 31 * result + (int) (revision ^ (revision >>> 32));
    return result;
  }

  /**
   * Returns the {@link code.google.pojomvcc.CacheRevisionType} of this {@link code.google.pojomvcc.CacheElementRevision}.
   *
   * @return the {@link code.google.pojomvcc.CacheRevisionType} of this {@link code.google.pojomvcc.CacheElementRevision}.
   */
  public CacheRevisionType getState() {
    return revisionType;
  }

  /**
   * Implementation of {@code java.util.Comparator} that compares {@link CacheElementRevision}.
   *
   * @author Aidan Morgan
   */
  private static class CacheElementRevisionComparator implements Comparator<CacheElementRevision> {
    public int compare(CacheElementRevision o1, CacheElementRevision o2) {
      Long one = o1.revision;
      Long two = o2.revision;

      return one.compareTo(two);
    }
  }
}
