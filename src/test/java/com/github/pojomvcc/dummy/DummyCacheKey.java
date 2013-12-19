package com.github.pojomvcc.dummy;

import java.util.UUID;

/**
 * @author Aidan Morgan
 */
public class DummyCacheKey {
  private Long l;

  public DummyCacheKey(Long val) {
    l = val;
  }

  public DummyCacheKey() {
    l = UUID.randomUUID().getLeastSignificantBits();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DummyCacheKey that = (DummyCacheKey) o;

    return l.equals(that.l);
  }

  @Override
  public int hashCode() {
    return l.hashCode();
  }

  @Override
  public String toString() {
    return "DummyCacheKey{" +
        "long=" + l +
        '}';
  }
}
