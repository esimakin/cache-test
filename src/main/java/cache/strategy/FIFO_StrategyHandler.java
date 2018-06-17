package cache.strategy;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Simple First-In-First-Out cache strategy implementation.
 */
public class FIFO_StrategyHandler extends CacheStrategyHandler {

  private final Deque<String> queue = new LinkedList<>();

  public FIFO_StrategyHandler(long cacheSize) {
    super(cacheSize);
  }

  @Override
  public String handle(String key) {
    if (queue.contains(key)) {
      return null;
    }
    queue.addFirst(key);
    if (queue.size() > cacheSize) {
      return queue.pollLast();
    }
    return null;
  }

}
