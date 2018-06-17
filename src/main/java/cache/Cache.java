package cache;

import java.io.Serializable;

/**
 * A mapping from keys to values. Use {@link #put(String, Serializable)} to add entry and {@link #get(String)} to get it.
 *
 * @param <T> value type
 */
public interface Cache<T extends Serializable> extends AutoCloseable {

  /**
   * Adds value to cache.
   *
   * @param key   the key
   * @param value the value
   */
  void put(String key, T value);

  /**
   * Gets value by key.
   *
   * @param key the key
   * @return value
   */
  T get(String key);

  /**
   * Invalidates cache (clear).
   */
  void invalidate();

  /**
   * Strategies
   */
  enum Strategy {
    /**
     * First-In-First-Out
     */
    FIFO,
    /**
     * Least-recently-used
     */
    LRU,
    /**
     * Least-frequently-used
     */
    LFU
  }

}
