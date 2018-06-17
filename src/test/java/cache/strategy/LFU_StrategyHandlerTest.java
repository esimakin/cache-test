package cache.strategy;

import org.junit.Test;

import static org.junit.Assert.*;
import static cache.strategy.TestUtils.rnd;

public class LFU_StrategyHandlerTest {

  @Test
  public void handle() {
    LFU_StrategyHandler handler = new LFU_StrategyHandler(2);
    String key1 = rnd();
    String key2 = rnd();
    String key3 = rnd();
    assertNull(handler.handle(key1));
    assertNull(handler.handle(key1));
    assertNull(handler.handle(key2));
    assertNull(handler.handle(key2));
    assertNull(handler.handle(key2));

    assertEquals(key1, handler.handle(key3));
  }

}