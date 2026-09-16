package UnitTest.Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.LinkedHashMap;
import Zeze.Util.Json;
import Zeze.Util.JsonWriter;
import harness.Fast;
import org.junit.jupiter.api.Test;

/**
 * FND7-72 回归：JsonWriter.free()后busy()恒false（tail==null不可见），同线程
 * acquireLocalWriter取回该实例，clear()首行tail.next即NPE——线程本地writer一旦
 * free即永久不可用。
 * 修复：busy()增加tail==null判定，free后的实例被视为占用，Json静态入口自动改用
 * 新实例（正常复用路径零变化）。
 * 补全：free()同时从ThreadLocal摘除自身——JsonWriter.local()直取路径
 * （DbWeb/AsyncSocket/TestJson等）不得拿到已释放实例，否则clear()同样NPE
 * （并行测试下跨类同worker线程随机中招）。
 */
@Fast
public class TestFnd772JsonWriterFree {
	@Test
	public void testJsonStillWorksAfterLocalWriterFreed() {
		JsonWriter.local().free();
		var map = new LinkedHashMap<String, Object>();
		map.put("a", 1);
		map.put("b", "x");
		// 修复前：acquireLocalWriter取回freed实例，clear()的tail.next抛NPE
		assertDoesNotThrow(() -> assertEquals("{\"a\":1,\"b\":\"x\"}", Json.toCompactString(map)));
		// 后续同线程多次使用也正常（每次都自动改用新实例）
		assertDoesNotThrow(() -> assertEquals("[1,2]", Json.toCompactString(new int[] {1, 2})));
	}

	@Test
	public void testLocalPathReturnsFreshInstanceAfterFree() {
		var freed = JsonWriter.local();
		freed.free();
		// 补全前：freed实例仍滞留ThreadLocal，local()直取拿到毒化实例，clear()首行NPE
		var fresh = JsonWriter.local();
		assertNotSame(freed, fresh);
		var map = new LinkedHashMap<String, Object>();
		map.put("a", 1);
		map.put("b", "x");
		assertDoesNotThrow(() -> assertEquals("{\"a\":1,\"b\":\"x\"}", fresh.clear().write(map).toString()));
		// 手写（非ThreadLocal登记）实例free不得摘除他人的登记
		var manual = new JsonWriter();
		manual.free();
		assertSame(fresh, JsonWriter.local());
	}
}
