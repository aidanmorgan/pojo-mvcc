package code.google.pojomvcc;

/**
 * This class defines how the {@code code.google.pojomvcc.RevisionObjectCache} should handle conflicts
 * when it's {@link RevisionObjectCache#update(RefreshOptions)} operation is invoked to
 * retrieve the latest revision from the {@link RootObjectCache}.
 * <p/>
 * There are many different strategies for resolving a conflict with the {@link RootObjectCache}
 * and some of those are provided as static instances for convenience.
 *
 * @author Aidan Morgan
 */
// TODO: [MF] This class defines course-of-action under specific conflict circumstances. Some of these actions
// TODO: [MF] are mutually exclusive but the interface does not enforce this (for example, only on of the following can
// TODO: [MF] be true at any one time, mergeOnUpdate, replaceOnUpdate, ignoreOnUpdate). Is this better suited to be
// TODO: [MF] an enum? Or perhaps, change the responsibility of actually doing the action to this class using a visitor
// TODO: [MF] or strategy pattern?
public abstract class RefreshOptions {
  /**
   * A very strict implementation of the {@code code.google.pojomvcc.RefreshOptions}. Will raise exceptions if
   * any changes conflict with the {@code code.google.pojomvcc.RootObjectCache}.
   * <p/>
   * This instance basically complies with the default behaviour of  <a href="http://subversion.tigris.org/">Subversion</a>.
   */
  public static final RefreshOptions STRICT = new RefreshOptions() {
    @Override
    public boolean failOnUpdateRemoved() {
      return true;
    }

    @Override
    public boolean failOnUpdateAdd() {
      return true;
    }

    @Override
    public boolean failOnUpdateModify() {
      return true;
    }

    @Override
    public boolean updateAdded() {
      return true;
    }

    @Override
    public boolean updateModified() {
      return true;
    }

    @Override
    public boolean updateRemoved() {
      return true;
    }

    @Override
    public boolean mergeOnUpdate() {
      return true;
    }

    @Override
    public boolean replaceOnUpdate() {
      return false;
    }

    @Override
    public boolean ignoreOnUpdate() {
      return false;
    }

  };

  /**
   * Returns {@code true} if an exception should be raised if the {@code code.google.pojomvcc.RootObjectCache} has modified
   * a {@code code.google.pojomvcc.CacheElement} that the {@code code.google.pojomvcc.RevisionObjectCache} has decided
   * to remove, {@code false} otherwise.
   *
   * @return
   */
  public abstract boolean failOnUpdateRemoved();

  /**
   * Returns {@code true} if an exception should be raised if the {@code code.google.pojomvcc.RootObjectCache} has modified
   * a {@code code.google.pojomvcc.CacheElement} that the {@code code.google.pojomvcc.RevisionObjectCache} has added, {@code false}
   * otherwise.
   * <p/>
   * This should only occur on a {@code code.google.pojomvcc.CacheKey} collision, that is another {@code code.google.pojomvcc.RevisionObjectCache}
   * has added an object with the same {@code code.google.pojomvcc.CacheKey}.
   *
   * @return
   */
  public abstract boolean failOnUpdateAdd();

  /**
   * Returns {@code true} if an exception should be raised if the {@code code.google.pojomvcc.RootObjectCache} has modified
   * a {@code code.google.pojomvcc.CacheElement} that the {@code code.google.pojomvcc.RevisionObjectCache} has also modified,
   * {@code false} otherwise.
   *
   * @return
   */
  public abstract boolean failOnUpdateModify();

  // TODO : [AM] : consider removing.

  public abstract boolean updateAdded();

  // TODO : [AM] : consider removing.

  public abstract boolean updateModified();

  // TODO : [AM] : consider removing.

  public abstract boolean updateRemoved();

  /**
   * Returns {@code true} if an object in the {@code code.google.pojomvcc.RootObjectCache} has been modified, but so has
   * the value in the {@code code.google.pojomvcc.RevisionObjectCache} and the required behaviour is to replace the value in
   * the {@code code.google.pojomvcc.RevisionObjectCache} with the value from the {@code code.google.pojomvcc.RootObjectCache},
   * {@code false} otherwise.
   *
   * @return
   */
  public abstract boolean replaceOnUpdate();

  /**
   * Returns {@code true} if an object in the {@code code.google.pojomvcc.RootObjectCache} has been modified, but so has
   * the value in the {@code code.google.pojomvcc.RevisionObjectCache} and the required behaviour is to merge the value in
   * the {@code code.google.pojomvcc.RevisionObjectCache} with the value from the {@code code.google.pojomvcc.RootObjectCache},
   * {@code false} otherwise.
   *
   * @return
   * @see code.google.pojomvcc.CacheElementFactory#merge(Object, Object)
   */
  public abstract boolean mergeOnUpdate();

  /**
   * Returns {@code true} if an object in the {@code code.google.pojomvcc.RootObjectCache} has been modified, but so has
   * the value in the {@code code.google.pojomvcc.RevisionObjectCache} and the required behaviour is to ignore the value in
   * the {@code code.google.pojomvcc.RootObjectCache} and keep the changes in the {@code code.google.pojomvcc.RevisionObjectCache},
   * {@code false} otherwise.
   *
   * @return
   */
  public abstract boolean ignoreOnUpdate();

  /**
   * Returns {@code true} if the provided {@code code.google.pojomvcc.RefreshOptions} are valid, {@code false} otherwise.
   *
   * @return
   */
  public boolean isValid() {
    boolean a = ignoreOnUpdate();
    boolean b = mergeOnUpdate();
    boolean c = replaceOnUpdate();

    return (a && !b && !c) || (!a && b && !c) || (!a && !b && c);
  }
}
