import cache.Cache;
import cache.CacheImpl;

public class Main {

  /**
   * Usage example.
   */
  public static void main(String[] args) {
    Cache<String> cache = new CacheImpl<>(200, 300);
    cache.put("key", "test");
    cache.invalidate();
  }

}
