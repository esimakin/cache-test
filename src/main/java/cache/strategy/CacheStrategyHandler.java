package cache.strategy;

/**
 * Strategy to choose replaced key in cache.
 */
public abstract class CacheStrategyHandler {

  protected final long cacheSize;

  protected CacheStrategyHandler(long cacheSize) {
    this.cacheSize = cacheSize;
  }

  public abstract String handle(String key);

}
