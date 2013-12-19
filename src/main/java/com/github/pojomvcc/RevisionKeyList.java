package com.github.pojomvcc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A {@link RevisionKeyList} represents the set of {@link K} instances that
 * are associated with a specific revision in the {@link RootObjectCache}.
 *
 * @author Aidan Morgan
 */
public class RevisionKeyList<K> {
  /**
   * A {@code java.util.List} of {@link K} that are the keys for this revision.
   */
  private List<K> keys;

  /**
   * The revision this set of {@link K} are for.
   */
  private long revision;

  /**
   * The {@code Date} that this {@link RevisionKeyList} was created.
   */
  private long revisionTime;

  /**
   * Constructor.
   *
   * @param rev       the revision of this {@link RevisionKeyList}.
   * @param timestamp the time that this {@link RevisionKeyList} was created.
   */
  RevisionKeyList(long rev, long timestamp) {
    this.revision = rev;
    this.revisionTime = timestamp;

    this.keys = new ArrayList<K>();
  }

  /**
   * Constructor.
   *
   * @param rev the revision of this {@link RevisionKeyList}
   */
  public RevisionKeyList(long rev) {
    this(rev, System.currentTimeMillis());
  }


  /**
   * Copy-Constructor. This will create a new {@link RevisionKeyList} with a copy of the
   * keys in the provided {@code com.github.pojomvcc.RevisionKeyList} but assigned to the provided revision
   * and the {@link RevisionKeyList#revisionTime} set to the current time.
   *
   * @param revision the revision of this {@link RevisionKeyList}.
   * @param other    a {@link RevisionKeyList} to clone the {@link K}s for.
   */
  public RevisionKeyList(long revision, RevisionKeyList other) {
    this(revision, System.currentTimeMillis());
    this.keys = new ArrayList<K>(other.keys);
  }

  /**
   * Copy-Constructor.
   *
   * @param revision the revision of this {@link RevisionKeyList}.
   * @param other    a {@code java.util.List} of {@link K}s that make up the revision.
   */
  public RevisionKeyList(long revision, List<K> other) {
    this(revision, System.currentTimeMillis());
    keys = new ArrayList<K>(other);
  }

  /**
   * Returns the revision number of this {@link RevisionKeyList}.
   *
   * @return The revision number.
   */
  public long getRevision() {
    return revision;
  }

  /**
   * Returns the time (in milliseconds from epoch) that this revision was created.
   *
   * @return The creation timestamp.
   */
  public Date getRevisionTime() {
    return new Date(revisionTime);
  }

  /**
   * @inheritDoc
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RevisionKeyList that = (RevisionKeyList) o;

    return revision == that.revision;
  }

  /**
   * @inheritDoc
   */
  @Override
  public int hashCode() {
    return (int) (revision ^ (revision >>> 32));
  }

  /**
   * Returns a {@code List} of {@link K}s that are in this {@link RevisionKeyList}.
   *
   * @return
   */
  public List<K> getKeys() {
    return keys;
  }

  /**
   * Returns the number {@link K}s in this {@link RevisionKeyList}.
   *
   * @return
   */
  public int size() {
    return keys.size();
  }
}
