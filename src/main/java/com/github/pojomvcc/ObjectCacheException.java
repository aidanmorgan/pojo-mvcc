package com.github.pojomvcc;

/**
 * {@code Exception} that is thrown when an error occurs.
 *
 * @author Aidan Morgan
 */
public class ObjectCacheException extends RuntimeException {

  /**
   * @inheritDoc
   */
  public ObjectCacheException() {
    super();
  }

  /**
   * @inheritDoc
   */
  public ObjectCacheException(String message) {
    super(message);
  }

  /**
   * @inheritDoc
   */
  public ObjectCacheException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectCacheException(Throwable cause) {
    super(cause);
  }
}
