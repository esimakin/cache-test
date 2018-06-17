package cache.strategy;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Least-recent strategy implementation.
 */
public class LRU_StrategyHandler extends CacheStrategyHandler {

  private final Set<String> keys = new HashSet<>();
  private final LinkedList<String> keysOrdered = new LinkedList<>();

  public LRU_StrategyHandler(long cacheSize) {
    super(cacheSize);
  }

  @Override
  public String handle(String key) {
    if (keys.size() >= cacheSize && !keys.contains(key)) {
      String out = keysOrdered.pollFirst();
      keys.remove(out);
      return out;
    } else if (keys.contains(key)) {
      keysOrdered.remove(key);
      keysOrdered.addLast(key);
      return null;
    } else {
      keys.add(key);
      keysOrdered.addLast(key);
      return null;
    }
  }

}
