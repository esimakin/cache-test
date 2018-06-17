package cache.strategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static cache.strategy.TestUtils.rnd;

public class FIFO_StrategyHandlerTest {

  @Test
  public void handleSimple() {
    FIFO_StrategyHandler handler = new FIFO_StrategyHandler(2);
    assertNull(handler.handle(rnd()));
    assertNull(handler.handle(rnd()));
    assertNotNull(handler.handle(rnd()));
  }

  @Test
  public void handleSame() {
    FIFO_StrategyHandler handler = new FIFO_StrategyHandler(2);
    String key1 = rnd();
    String key2 = rnd();
    String key3 = rnd();
    assertNull(handler.handle(key1));
    assertNull(handler.handle(key2));
    assertNull(handler.handle(key1));
    assertEquals(key1, handler.handle(key3));
  }


}