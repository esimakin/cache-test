package cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CacheImplTest {

  private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  CacheImpl<String> cache;

  private static void accept(CacheImpl<Testee>.CacheEntry e) {
    if (e.onDisk) {
      assertNull(e.object);
    } else {
      assertNotNull(e.object);
    }
  }

  @Before
  public void create() {
    cache = new CacheImpl<>(2, 3);
  }

  @After
  public void cleanUp() {
    cache.close();
  }

  @Test(expected = NullPointerException.class)
  public void putNullKey() {
    cache.put(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void putNullValue() {
    cache.put(" ", null);
  }

  @Test(expected = NullPointerException.class)
  public void getByNullKey() {
    cache.get(null);
  }

  @Test(expected = NullPointerException.class)
  public void nullStrategy() {
    new CacheImpl<String>(2, 3, null);
  }

  @Test
  public void IOTest() throws IOException {
    String key = "key";
    String value = UUID.randomUUID().toString();
    cache.writeToTwoLevel(key, value);
    assertEquals(value, cache.readFromTwoLevel(key));
    cache.removeFromTwoLevel(key);
    assertNull(cache.readFromTwoLevel(key));
  }

  @Test
  public void onlyFirstLevel() {
    Cache<String> cache = new CacheImpl<>(2, 0, Cache.Strategy.LRU);
    cache.put("k1", "v1");
    cache.put("k2", "v2");
    cache.put("k3", "v3");

    assertEquals(2, ((CacheImpl<String>) cache).map.size());
    ((CacheImpl<String>) cache).map.values().forEach(e -> {
      assertFalse(e.onDisk);
      assertNotNull(e.object);
    });
  }

  @Test
  public void onlySecondLevel() throws Exception {
    cache.close();
    try (Cache<String> cache = new CacheImpl<>(0, 2)) {
      cache.put("k1", "v1");
      cache.put("k2", "v2");
      cache.put("k3", "v3");

      assertEquals(2, ((CacheImpl<String>) cache).map.size());
      ((CacheImpl<String>) cache).map.values().forEach(e -> {
        assertTrue(e.onDisk);
        assertNull(e.object);
      });
    }
  }

  @Test
  public void test() throws Exception {
    cache.close();
    String k1 = rndStr(5);
    String k2 = rndStr(5);
    String k3 = rndStr(5);
    String k4 = rndStr(5);
    String k5 = rndStr(5);
    Testee v1 = new Testee();
    Testee v2 = new Testee();
    Testee v3 = new Testee();
    Testee v4 = new Testee();
    Testee v5 = new Testee();

    try (Cache<Testee> cache = new CacheImpl<>(2, 3, Cache.Strategy.FIFO)) {
      cache.put(k1, v1);
      cache.put(k2, v2);
      assertEquals(2, ((CacheImpl<Testee>) cache).map.size());
      ((CacheImpl<Testee>) cache).map.values().forEach(CacheImplTest::accept);
      assertEquals(0, Objects.requireNonNull(((CacheImpl<Testee>) cache).levelTwoCacheDir.list()).length);

      assertEquals(v1, cache.get(k1));
      assertEquals(v2, cache.get(k2));

      assertEquals(2, ((CacheImpl<Testee>) cache).map.size());
      ((CacheImpl<Testee>) cache).map.values().forEach(CacheImplTest::accept);
      assertEquals(0, Objects.requireNonNull(((CacheImpl<Testee>) cache).levelTwoCacheDir.list()).length);

      cache.put(k3, v3);
      assertEquals(v1, cache.get(k1));
      assertEquals(v2, cache.get(k2));
      cache.put(k4, v4);
      cache.put(k5, v5);
      assertEquals(v4, cache.get(k4));
      assertEquals(v5, cache.get(k5));

      assertEquals(5, ((CacheImpl<Testee>) cache).map.size());
      ((CacheImpl<Testee>) cache).map.values().forEach(CacheImplTest::accept);
      assertEquals(3, Objects.requireNonNull(((CacheImpl<Testee>) cache).levelTwoCacheDir.list()).length);

      Testee t = new Testee();
      cache.put(k3, t);
      assertEquals(5, ((CacheImpl<Testee>) cache).map.size());
      ((CacheImpl<Testee>) cache).map.values().forEach(CacheImplTest::accept);
      assertEquals(3, Objects.requireNonNull(((CacheImpl<Testee>) cache).levelTwoCacheDir.list()).length);
      CacheImpl<Testee>.CacheEntry en3 = ((CacheImpl<Testee>) cache).map.get(k3);
      assertFalse(en3.onDisk);
      assertEquals(t, en3.object);

      cache.put(k5, t);
      assertEquals(5, ((CacheImpl<Testee>) cache).map.size());
      cache.put(k5, t);
      assertEquals(5, ((CacheImpl<Testee>) cache).map.size());
      cache.put(k1, t);
      assertEquals(5, ((CacheImpl<Testee>) cache).map.size());
    }
  }

  @Test(expected = IllegalStateException.class)
  public void unavailable() {
    cache.close();
    cache = new CacheImpl<String>(0, 0);
    cache.put(" ", " ");
  }

  @Test
  public void invalidate() {
    cache.put("key", "value");
    assertNotNull(cache.get("key"));
    cache.invalidate();
    assertNull(cache.get("key"));
  }

  static class Testee implements Serializable {

    String value;

    Testee() {
      value = rndStr(50);
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Testee testee = (Testee) o;
      return Objects.equals(value, testee.value);
    }

    @Override
    public int hashCode() {

      return Objects.hash(value);
    }
  }

  static String rndStr(int count) {
    StringBuilder b = new StringBuilder();
    while (count-- > 0) {
      int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
      b.append(ALPHA_NUMERIC_STRING.charAt(character));
    }
    return b.toString();
  }


}