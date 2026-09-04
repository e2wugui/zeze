package UnitTest.Zeze.Util;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import Zeze.Util.IntHashMap;
import Zeze.Util.Json;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.LongHashMap;

/**
 * 顶层 Json.parse(s, IntHashMap/LongHashMap/LongConcurrentHashMap.class) 没有字段
 * 上下文可取值类型：修复前 valueMeta=null，parseNested(null) 不消费值 token，静默产出
 * 全 null 值的错误结果（见 FND2-U1-4），与字段级 raw 声明的 fail-fast（c40b54a93）不一致。
 * 现在显式抛 InstantiationException。
 */
@Fast
public final class TestJsonTopLevelMap {
	@Test
	public void testTopLevelIntHashMapThrows() {
		var ex = assertThrows(InstantiationException.class,
				() -> Json.parse("{\"1\":5,\"2\":6}", IntHashMap.class));
		assertNotNull(ex.getMessage());
		assertTrue(ex.getMessage().contains("without concrete value type"), ex.getMessage());
	}

	@Test
	public void testTopLevelLongHashMapThrows() {
		assertThrows(InstantiationException.class, () -> Json.parse("{\"1\":5}", LongHashMap.class));
	}

	@Test
	public void testTopLevelLongConcurrentHashMapThrows() {
		assertThrows(InstantiationException.class, () -> Json.parse("{\"1\":5}", LongConcurrentHashMap.class));
	}

	static class Bean {
		IntHashMap<String> typed = new IntHashMap<>();
		LongHashMap<String> typedLong = new LongHashMap<>();
	}

	@Test
	public void testTypedFieldStillParses() throws ReflectiveOperationException {
		// 回归：带具体值类型的字段级路径不受影响（paramTypes[0] 为 Class，正常取值解析）
		Bean b = Json.parse("{\"typed\":{\"1\":\"a\",\"2\":\"b\"},\"typedLong\":{\"7\":\"z\"}}", Bean.class);
		assertEquals("a", b.typed.get(1));
		assertEquals("b", b.typed.get(2));
		assertEquals("z", b.typedLong.get(7L));
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonTopLevelMap();
		t.testTopLevelIntHashMapThrows();
		t.testTopLevelLongHashMapThrows();
		t.testTopLevelLongConcurrentHashMapThrows();
		t.testTypedFieldStillParses();
		System.out.println(t.getClass().getSimpleName() + ": 4 tests OK!");
	}
}
