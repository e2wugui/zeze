package Zeze.Util;

import org.jspecify.annotations.Nullable;
import harness.Fast;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;

/**
 * Json 解析必须有嵌套深度上限：深嵌套输入（如 10 万层 "[[[..."）修复前直接
 * StackOverflowError 打穿请求线程；现在超限抛带清晰信息的 IllegalStateException（见 FND-U1-5）。
 * 上限默认 1024：远大于写侧默认深度 16，且距 SOE 阈值有一个数量级以上余量。
 */
@Fast
public final class TestJsonNestingLimit {
	static class Node {
		@Nullable Node n;
	}

	abstract static class AbstractElem {
	}

	static class ContainerBean {
		@Nullable List<AbstractElem> list;
		@Nullable Map<String, AbstractElem> map;
		int @Nullable [] arr;
	}

	private static String nestArray(int depth) {
		return "[".repeat(depth) + "1" + "]".repeat(depth);
	}

	private static String nestMap(int depth) {
		var sb = new StringBuilder();
		for (int i = 0; i < depth; i++)
			sb.append("{\"a\":");
		sb.append("1");
		for (int i = 0; i < depth; i++)
			sb.append("}");
		return sb.toString();
	}

	private static String nestBean(int depth) {
		var sb = new StringBuilder();
		for (int i = 0; i < depth; i++)
			sb.append("{\"n\":");
		sb.append("{}");
		for (int i = 0; i < depth; i++)
			sb.append("}");
		return sb.toString();
	}

	@Test
	public void testWithinLimitOk() throws ReflectiveOperationException {
		// 默认上限 1024 内（1000 层）正常解析
		ArrayList<?> a = (ArrayList<?>)JsonReader.local().buf(nestArray(1000)).parse();
		assertNotNull(a);

		// 同一 reader 复用（buf 清零计数）再解析浅层
		a = (ArrayList<?>)JsonReader.local().buf("[1,2]").parse();
		assertNotNull(a);
		assertEquals(2, a.size());
	}

	@Test
	public void testUntypedArrayOverLimit() {
		var ex = assertThrows(IllegalStateException.class,
				() -> JsonReader.local().buf(nestArray(2000)).parse());
		assertTrue(ex.getMessage().contains("depth"), ex.getMessage());
	}

	@Test
	public void testUntypedMapOverLimit() {
		assertThrows(IllegalStateException.class, () -> {
			@SuppressWarnings("unused")
			Map<String, @Nullable Object> m = JsonReader.local().buf(nestMap(2000)).parseMap(null);
		});
	}

	@Test
	public void testTypedBeanOverLimit() {
		// 自引用 bean：TYPE_CUSTOM 路径经 parse0 递归
		assertThrows(IllegalStateException.class, () -> JsonReader.local().buf(nestBean(2000)).parse(Node.class));
	}

	@Test
	public void testStaticParseOverLimit() {
		// Json.parse 静态入口（DbWeb/网络入口形态）：深度守卫目前只覆盖 JsonReader 直接容器路径，
		// 静态入口经 Json.java 注册 parser 的嵌套递归不经检查（已记新发现候选，见 FND-U1-5.md），
		// 现状深嵌套抛 RuntimeException（此处表现为 tmp 缓冲越界的 AIOOBE）而非 StackOverflowError。
		assertThrows(RuntimeException.class, () -> Json.parse(nestArray(2000), Object.class));
	}

	@Test
	public void testCustomMaxDepth() throws ReflectiveOperationException {
		JsonReader jr = JsonReader.local();
		jr.setMaxDepth(8);
		// 5 层在上限内
		assertNotNull(jr.buf(nestBean(5)).parse(Node.class));
		// 10 层超限
		assertThrows(IllegalStateException.class, () -> jr.buf(nestBean(10)).parse(Node.class));
		// 触发超限异常后，buf() 已清零计数，reader 可正常复用；结束前还原默认配置
		assertNotNull(jr.buf(nestBean(2)).parse(Node.class));
		jr.setMaxDepth(JsonReader.DEFAULT_MAX_DEPTH);
	}

	@Test
	public void testShallowUnaffected() throws ReflectiveOperationException {
		Node n = JsonReader.local().buf("{\"n\":{\"n\":{}}}").parse(Node.class);
		assertNotNull(n);
		assertNotNull(n.n);
		assertNotNull(n.n.n);
	}

	@Test
	public void testDepthRestoredAfterExceptionsWithoutReset() throws ReflectiveOperationException {
		// 同一 JsonReader 在仅 pos(0) 复用、不调用 buf()/reset() 时，连续触发超过 maxDepth 次容器内异常，
		// depth 必须在 finally 中完整归零，不得累积泄漏导致后续合法浅层 JSON 误报超深。
		int maxDepth = 4;

		// 1) IllegalStateException (parseArray0 / parseMap0 / parse0)
		var jr = new JsonReader().setMaxDepth(maxDepth).buf(nestArray(maxDepth + 2) + " " + nestMap(maxDepth + 2) + " [1]");
		int mapOverPos = nestArray(maxDepth + 2).length() + 1;
		int validPos = mapOverPos + nestMap(maxDepth + 2).length() + 1;
		for (int i = 0; i < maxDepth + 3; i++) {
			jr.pos(0);
			assertThrows(IllegalStateException.class, jr::parse);
			jr.pos(mapOverPos);
			assertThrows(IllegalStateException.class, () -> jr.parseMap(null));
		}
		jr.pos(validPos);
		assertEquals(List.of(1), jr.parse());

		// 2) InstantiationException inside parseList0 / parseMap0(Map, ClassMeta, FieldMeta) / parse0
		String badList = "{\"list\":[{}]}";
		String badMap = "{\"map\":{\"k\":{}}}";
		String badArr = "{\"arr\":[bad]}";
		String okBean = "{\"n\":{}}";
		var jr2 = new JsonReader().setMaxDepth(maxDepth).buf(badList + " " + badMap + " " + badArr + " " + okBean);
		int mapPos = badList.length() + 1;
		int arrPos = mapPos + badMap.length() + 1;
		int okPos = arrPos + badArr.length() + 1;
		for (int i = 0; i < maxDepth + 2; i++) {
			jr2.pos(0);
			assertThrows(InstantiationException.class, () -> jr2.parse(ContainerBean.class));
			jr2.pos(mapPos);
			assertThrows(InstantiationException.class, () -> jr2.parse(ContainerBean.class));
			jr2.pos(arrPos);
			assertThrows(NumberFormatException.class, () -> jr2.parse(ContainerBean.class));
		}
		jr2.pos(okPos);
		Node ok = jr2.parse(Node.class);
		assertNotNull(ok);
		assertNotNull(ok.n);

		// 3) parseArray(Json, Collection, Class) & parseMap0(Map)
		String typedBad = "[{}]";
		var jr3 = new JsonReader().setMaxDepth(maxDepth).buf(typedBad + " {\"a\":1}");
		int okMapPos = typedBad.length() + 1;
		for (int i = 0; i < maxDepth + 2; i++) {
			jr3.pos(0);
			assertThrows(InstantiationException.class, () -> jr3.parseArray(null, AbstractElem.class));
		}
		jr3.pos(okMapPos);
		var m = jr3.parseMap(null);
		assertNotNull(m);
		assertEquals(1, m.get("a"));
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJsonNestingLimit();
		t.testWithinLimitOk();
		t.testUntypedArrayOverLimit();
		t.testUntypedMapOverLimit();
		t.testTypedBeanOverLimit();
		t.testStaticParseOverLimit();
		t.testCustomMaxDepth();
		t.testShallowUnaffected();
		System.out.println(t.getClass().getSimpleName() + ": 7 tests OK!");
	}
}
