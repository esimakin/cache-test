package cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cache.strategy.CacheStrategyHandler;
import cache.strategy.FIFO_StrategyHandler;
import cache.strategy.LFU_StrategyHandler;
import cache.strategy.LRU_StrategyHandler;

/**
 * Default two-level cache implementation.
 *
 * @param <T> value type
 */
@SuppressWarnings("WeakerAccess")
public class CacheImpl<T extends Serializable> implements Cache<T> {

  private static final Logger Log = LoggerFactory.getLogger("cache");

  private final long levelOneSize;
  private long levelTwoSize;

  private final CacheStrategyHandler levelOneHandler;
  private final CacheStrategyHandler levelTwoHandler;

  protected final File levelTwoCacheDir;

  protected final Map<String, CacheEntry> map = new HashMap<>();

  public CacheImpl(long levelOneSize, long levelTwoSize) {
    this(levelOneSize, levelTwoSize, Strategy.LFU, new File("cache2"));
  }

  public CacheImpl(long levelOneSize, long levelTwoSize, Strategy strategy) {
    this(levelOneSize, levelTwoSize, strategy, new File("cache2"));
  }

  public CacheImpl(long levelOneSize, long levelTwoSize, Strategy strategy, File levelTwoCacheDir) {
    this.levelOneSize = levelOneSize;
    this.levelTwoSize = levelTwoSize;
    switch (strategy) {
      case LFU:
        levelOneHandler = new LFU_StrategyHandler(levelOneSize);
        levelTwoHandler = new LFU_StrategyHandler(levelTwoSize);
        break;
      case LRU:
        levelOneHandler = new LRU_StrategyHandler(levelOneSize);
        levelTwoHandler = new LRU_StrategyHandler(levelTwoSize);
        break;
      case FIFO:
        levelOneHandler = new FIFO_StrategyHandler(levelOneSize);
        levelTwoHandler = new FIFO_StrategyHandler(levelTwoSize);
        break;
      default:
        throw new IllegalArgumentException("Unknown cache cache.strategy");
    }
    this.levelTwoCacheDir = levelTwoCacheDir;
    if (!this.levelTwoCacheDir.exists()) {
      boolean created = this.levelTwoCacheDir.mkdir();
      if (!created) {
        Log.warn("Could not create directory for two-level cache");
      }
    }
  }

  @Override
  public void put(String key, T value) {
    Objects.requireNonNull(key, "The key cannot be NULL");
    Objects.requireNonNull(value, "The value cannot be NULL");

    if (map.containsKey(key)) {
      CacheEntry entry = map.get(key);
      if (entry.onDisk) {
        try {
          entry.object = readFromTwoLevel(key);
          if (!value.equals(entry.object)) {
            entry = moveToMem(key, entry);
            entry.object = value;
          }
        } catch (IOException e) {
          levelTwoSize = 0;
          entry.onDisk = false;
          put(key, value);
        }
      } else if (!value.equals(entry.object)) {
        entry.object = value;
      }
    } else if (levelOneSize > 0) {
      String outKey = levelOneHandler.handle(key);
      if (outKey != null) {
        CacheEntry outEntry = map.get(outKey);
        if (levelTwoSize <= 0) {
          map.remove(outKey);
        } else {
          String lostKey = levelTwoHandler.handle(outKey);
          if (lostKey != null) {
            map.remove(lostKey);
            removeFromTwoLevel(lostKey);
          }
          try {
            writeToTwoLevel(outKey, outEntry.object);
            outEntry.object = null;
            outEntry.onDisk = true;
          } catch (IOException e) {
            levelTwoSize = 0;
            outEntry.onDisk = false;
            put(key, value);
          }
        }
      }
      CacheEntry entry = new CacheEntry();
      entry.object = value;
      map.put(key, entry);
    } else if (levelTwoSize > 0) {
      CacheEntry entry = new CacheEntry();
      entry.onDisk = true;
      map.put(key, entry);
      String lostKey = levelTwoHandler.handle(key);
      if (lostKey != null) {
        map.remove(lostKey);
        removeFromTwoLevel(lostKey);
      }
      try {
        writeToTwoLevel(key, value);
      } catch (IOException e) {
        levelTwoSize = 0;
        put(key, value);
      }
    } else {
      throw new IllegalStateException("cache.Cache is unavailable");
    }
  }

  @Override
  public T get(String key) {
    Objects.requireNonNull(key, "The key cannot be NULL");

    CacheEntry entry = map.get(key);
    if (entry == null) {
      return null;
    }

    if (entry.onDisk) {
      try {
        return moveToMem(key, entry).object;
      } catch (IOException e) {
        levelTwoSize = 0;
        return null;
      }
    } else {
      levelOneHandler.handle(key);
      return entry.object;
    }
  }

  protected CacheEntry moveToMem(String key, CacheEntry entry) throws IOException {
    if (entry.object == null) {
      T obj = readFromTwoLevel(key);
      if (obj == null) {
        map.remove(key);
        return entry;
      }
      entry.object = obj;
    }

    removeFromTwoLevel(key);
    entry.onDisk = false;
    String outKey = levelOneHandler.handle(key);
    if (outKey != null && levelTwoSize > 0) {
      CacheEntry dropToDiskEntry = map.get(outKey);
      writeToTwoLevel(outKey, dropToDiskEntry.object);
      dropToDiskEntry.onDisk = true;
      dropToDiskEntry.object = null;
    }
    return entry;
  }

  @Override
  public void invalidate() {
    List<Map.Entry<String, CacheEntry>> toDel = map.entrySet().stream()
        .filter(entry -> entry.getValue().onDisk)
        .collect(Collectors.toList());
    for (Map.Entry<String, CacheEntry> entry : toDel) {
      removeFromTwoLevel(entry.getKey());
    }
    map.clear();
  }

  protected void writeToTwoLevel(String key, T object) throws IOException {
    try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(new File(levelTwoCacheDir, keyToFileName(key))))) {
      stream.writeObject(object);
      stream.flush();
    } catch (IOException e) {
      Log.error("IO Error", e);
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  protected T readFromTwoLevel(String key) throws IOException {
    try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(new File(levelTwoCacheDir, keyToFileName(key))))) {
      return (T) stream.readObject();
    } catch (FileNotFoundException e) {
      Log.error("IO Error", e);
      return null;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      Log.error("IO Error", e);
      throw e;
    }
  }

  protected void removeFromTwoLevel(String key) {
    File file = new File(levelTwoCacheDir, keyToFileName(key));
    boolean deleted = file.delete();
    if (!deleted) {
      Log.warn("Could not delete file {}", file);
    }
  }

  String keyToFileName(String key) {
    return key.replaceAll("[^a-zA-Z0-9-_\\\\.]", "");
  }

  @Override
  public void close() {
    invalidate();
    levelTwoCacheDir.delete();
  }

  class CacheEntry {

    T object;
    boolean onDisk;
  }

}
