package cache.strategy;

import org.junit.Test;

import static org.junit.Assert.*;

public class LRU_StrategyHandlerTest {

  @Test
  public void handle() {
    LRU_StrategyHandler handler = new LRU_StrategyHandler(3);
    String key1 = TestUtils.rnd();
    String key2 = TestUtils.rnd();
    String key3 = TestUtils.rnd();
    String key4 = TestUtils.rnd();

    assertNull(handler.handle(key1));
    assertNull(handler.handle(key2));
    assertNull(handler.handle(key3));
    assertNull(handler.handle(key1));
    assertNull(handler.handle(key2));

    assertEquals(key3, handler.handle(key4));
  }

}