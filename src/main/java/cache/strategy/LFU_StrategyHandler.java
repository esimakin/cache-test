package cache.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Least-frequent strategy implementation.
 */
public class LFU_StrategyHandler extends CacheStrategyHandler {

  private final Map<String, Long> frequencies = new HashMap<>();
  private String leastFrequentKey = null;

  public LFU_StrategyHandler(long cacheSize) {
    super(cacheSize);
  }

  @Override
  public String handle(String key) {
    if (frequencies.keySet().size() >= cacheSize && !frequencies.containsKey(key)) {
      frequencies.remove(leastFrequentKey);
      frequencies.put(key, 1L);
      String out = leastFrequentKey;
      updateLeastFrequentKey();
      return out;
    } else {
      Long fr = frequencies.getOrDefault(key, 0L);
      frequencies.put(key, ++fr);
      updateLeastFrequentKey();
      return null;
    }
  }

  private void updateLeastFrequentKey() {
    TreeMap<Long, String> inverseMap = new TreeMap<>();
    for (Map.Entry<String, Long> entry : frequencies.entrySet()) {
      inverseMap.put(entry.getValue(), entry.getKey());
    }
    leastFrequentKey = inverseMap.firstEntry().getValue();
  }
}
