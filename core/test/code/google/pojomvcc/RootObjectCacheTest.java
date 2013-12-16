package code.google.pojomvcc;

import code.google.pojomvcc.dummy.DummyCacheElement;
import code.google.pojomvcc.dummy.DummyCacheElementFactory;
import code.google.pojomvcc.dummy.DummyCacheKey;
import code.google.pojomvcc.impl.RootObjectCacheImpl;
import junit.framework.Assert;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author Aidan Morgan
 */
public class RootObjectCacheTest {
  @Test
  public void testRootObjectCacheCheckout() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> rootCache
        = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> rev = rootCache.checkout();

    assertEquals(0L, rev.getRevision());

    DummyCacheKey key = new DummyCacheKey(1L);
    DummyCacheElement ele = new DummyCacheElement();

    rev.addElement(key, ele);

    assertTrue(rev.getAddedElements().contains(key));

    rootCache.commit(rev);

    assertEquals(rootCache.getRevision(), 1L);
    assertEquals(rootCache.size(), 1);
    assertNotNull(rootCache.getElement(key));
  }

  @Test
  public void testAddAndModifyOriginal() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> root = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> checkout = root.checkout();

    DummyCacheKey key = new DummyCacheKey();
    DummyCacheElement element = new DummyCacheElement();

    checkout.addElement(key, element);
    root.commit(checkout);

    element.setValue("Should not be shown in root cache.");

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> checkout2 = root.checkout();
    DummyCacheElement duplicate = checkout2.getElement(key);

    assertNull(duplicate.getValue());
  }

  @Test
  public void testRootObjectCacheCheckoutExisting() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> rootCache = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> rev = rootCache.checkout();

    DummyCacheKey key = new DummyCacheKey(1L);
    DummyCacheElement ele = new DummyCacheElement();

    rev.addElement(key, ele);
    rootCache.commit(rev);

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> two = rootCache.checkout();

    assertNotNull(two.getElement(key));

    DummyCacheKey newValKey = new DummyCacheKey(2L);
    DummyCacheElement newVal = new DummyCacheElement();
    two.addElement(newValKey, newVal);

    rootCache.commit(two);

    assertEquals(1, rev.size());
    assertEquals(2, two.size());
    assertEquals(2, rootCache.size());

    // make sure that the newly added value isn't in the list
    assertNull(rev.getElement(newValKey));
    assertNotNull(rootCache.getElement(newValKey));
  }

  @Test
  public void testMassiveInsert() {
    createCache(10000);
  }

  @Test
  public void testDelete() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> rootCache = createCache(100);
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> cache = rootCache.checkout();

    // test removing the first element
    DummyCacheKey key = new DummyCacheKey(0L);
    cache.removeElement(key);
    rootCache.commit(cache);

    assertEquals(99, rootCache.size());
    assertNull(rootCache.getElement(key));
    cache.close();

    // test removing the last element
    cache = rootCache.checkout();
    key = new DummyCacheKey(99L);
    cache.removeElement(key);
    rootCache.commit(cache);

    assertEquals(98, rootCache.size());
    assertNull(rootCache.getElement(key));
    cache.close();

    cache = rootCache.checkout();
    key = new DummyCacheKey(49L);
    cache.removeElement(key);
    rootCache.commit(cache);

    assertEquals(97, rootCache.size());
    assertNull(rootCache.getElement(key));

    cache.close();
  }

  @Test
  public void testBigDelete() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> cache = createCache(500);

    // make 50 sets of 10 deletes, just to get the values up.
    for (int i = 0; i < 50; i++) {
      RevisionObjectCache<DummyCacheKey, DummyCacheElement> rev = cache.checkout();

      for (int j = 0; j < 10; j++) {
        DummyCacheKey key = new DummyCacheKey((long) ((i * 10) + j));
        rev.removeElement(key);
      }

      assertEquals(500 - ((i + 1) * 10), rev.size());
      cache.commit(rev);
      rev.close();
    }

    assertEquals(0, cache.size());
  }

  @Test
  public void testMerge() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> c = createCache(100);
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> revCache = c.checkout();

    DummyCacheKey key = new DummyCacheKey(500L);
    DummyCacheElement ele = new DummyCacheElement();
    revCache.addElement(key, ele);

    c.commit(revCache);

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> newCache = c.checkout();
    DummyCacheElement dce = newCache.getElement(key);
    dce.setValue("abcd");

    c.commit(newCache);

    DummyCacheElement fromCache = c.getElement(key);
    assertEquals("abcd", fromCache.getValue());
    assertNull(ele.getValue());
  }

  @Test
  public void testRevert() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> cache = createCache(100);

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> rev = cache.checkout();
    // make sure that we have the right revision.
    assertEquals(100, rev.size());

    for (long i = 0; i < 10; i++) {
      DummyCacheKey key = new DummyCacheKey(i);

      assertTrue(rev.containsKey(key));

      DummyCacheElement ce = rev.getElement(key);
      ce.setValue("if this stays we're in trouble");
    }

    rev.revert();

    for (long i = 0; i < 100; i++) {
      DummyCacheKey key = new DummyCacheKey(i);
      DummyCacheElement element = rev.getElement(key);

      assertNotNull(element);
      assertNull(element.getValue());
    }

    rev.close();
  }

  @Test
  public void testUpdateConflict() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> cache = createCache(100);

    RevisionObjectCache<DummyCacheKey, DummyCacheElement> revisionOne = cache.checkout();
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> revisionTwo = cache.checkout();

    DummyCacheKey key = new DummyCacheKey(101L);
    DummyCacheElement element = new DummyCacheElement();

    revisionOne.addElement(key, element);
    revisionTwo.addElement(key, element);

    cache.commit(revisionTwo);

    try {
      revisionOne.update(RefreshOptions.STRICT);
      Assert.fail();
    } catch (Exception e) {

    }
  }

  @Test
  public void averageCaseLarge() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> root = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    root.setCacheExpiry(new CacheExpiry(CacheExpiryPolicy.NO_LONGER_USED(2)));

    for (int i = 0; i < 100; i++) {
      long start = System.currentTimeMillis();
      insertIntoCache(root, 1000);
      long end = System.currentTimeMillis();

      System.out.println((end - start));
    }
  }

  @Test
  public void averageCaseMultithreaded() {
    final RootObjectCache<DummyCacheKey, DummyCacheElement> cache = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    cache.setCacheExpiry(new CacheExpiry(CacheExpiryPolicy.NO_LONGER_USED(2)));

    ExecutorService multiService = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 100; i++) {
      multiService.execute(new Runnable() {
        public void run() {
          long start = System.currentTimeMillis();
          insertIntoCache(cache, 1000);
          long end = System.currentTimeMillis();

          System.out.println((end - start));
        }
      });
    }

    try {
      multiService.shutdown();
      // yes it's excessive.
      multiService.awaitTermination(50, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Assert.fail();
    }
  }


  public void worstCaseLarge() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> root = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    root.setCacheExpiry(new CacheExpiry(CacheExpiryPolicy.NO_LONGER_USED(2)));

    long start = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      insertIntoCache(root, 1);

      if (i % 1000 == 0) {
        long end = System.currentTimeMillis();

        System.out.println((end - start));

        start = end;
      }

    }
  }

  // TODO : [AM] : If you want to test how many elements can be fit into memory before throwing an
  // TODO : [AM] : OutOfMemoryError annotate this method with @Test
  public void tryToRunOutOfMemory() {
    RootObjectCache<DummyCacheKey, DummyCacheElement> root = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());

    for (int i = 0; i < Integer.MAX_VALUE; i++) {
      long start = System.currentTimeMillis();
      insertIntoCache(root, 5000);
      long end = System.currentTimeMillis();

      System.out.println("Size: " + root.size() + " " + (end - start) + " ms.");
    }
  }


  private RootObjectCache<DummyCacheKey, DummyCacheElement> createCache(int numElements) {
    RootObjectCache<DummyCacheKey, DummyCacheElement> rootCache = new RootObjectCacheImpl<DummyCacheKey, DummyCacheElement>(new DummyCacheElementFactory());
    rootCache.setCacheExpiry(new CacheExpiry(CacheExpiryPolicy.NO_LONGER_USED(1)));


    long startTime = System.currentTimeMillis();
    for (long i = 0; i < numElements; i++) {
      RevisionObjectCache<DummyCacheKey, DummyCacheElement> cache = rootCache.checkout();

      DummyCacheKey key = new DummyCacheKey(i);
      DummyCacheElement element = new DummyCacheElement();
      cache.addElement(key, element);
      rootCache.commit(cache);
      cache.close();

      if (i % 1000 == 0) {
        long endTime = System.currentTimeMillis();
        System.out.println("Created " + i + " in " + (endTime - startTime) + " ms.");
        startTime = System.currentTimeMillis();
      }
    }

    assertEquals(numElements, rootCache.size());

    return rootCache;
  }

  private void insertIntoCache(RootObjectCache<DummyCacheKey, DummyCacheElement> rootCache, int numElements) {
    RevisionObjectCache<DummyCacheKey, DummyCacheElement> cache = rootCache.checkout();

    for (long i = 0; i < numElements; i++) {
      DummyCacheKey key = new DummyCacheKey();
      DummyCacheElement element = new DummyCacheElement();
      cache.addElement(key, element);
    }

    rootCache.commit(cache);
    cache.close();
  }


}
