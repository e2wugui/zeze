package UnitTest.Zeze.Util;

import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import junit.framework.TestCase;
import org.junit.Assert;

@SuppressWarnings({"unused", "TextBlockMigration"})
public final class TestJson extends TestCase {
	static class A {
		int a;
	}

	static class B extends A {
		int b;
	}

	static class C {
		A a = new B();
		C c;
	}

	public void test1() throws ReflectiveOperationException {
		byte[] buf = "{a:[{x:1,y:2},{x:3,y:4},{x:5,y:6}]}".getBytes();
		Object obj = JsonReader.local().buf(buf).parse();
		Assert.assertNotNull(obj);
		Assert.assertEquals("{a=[{x=1, y=2}, {x=3, y=4}, {x=5, y=6}]}", obj.toString());
	}

	public void test2() throws ReflectiveOperationException {
		C c = JsonReader.local().buf("{a:{a:1,b:2}}").parse(C.class);
		Assert.assertNotNull(c);
		Assert.assertEquals(B.class, c.a.getClass());
		Assert.assertEquals(1, c.a.a);
		Assert.assertEquals(2, ((B)c.a).b);

		c.a = new A();
		c = JsonReader.local().buf("{a:{a:3,b:4}}").parse(c);
		Assert.assertNotNull(c);
		Assert.assertEquals(A.class, c.a.getClass());
		Assert.assertEquals(3, c.a.a);

		c.a = null;
		c = JsonReader.local().buf("{a:{a:5,b:6}}").parse(c);
		Assert.assertNotNull(c);
		Assert.assertEquals(A.class, c.a.getClass());
		Assert.assertEquals(5, c.a.a);

		Json.getClassMeta(A.class).setParser((Json, __, ___) -> Json.parse(B.class));
		c.a = null;
		c = JsonReader.local().buf("{a:{a:7,b:8}}").parse(c);
		Assert.assertNotNull(c);
		Assert.assertEquals(B.class, c.a.getClass());
		Assert.assertEquals(7, c.a.a);
		Assert.assertEquals(8, ((B)c.a).b);
	}

	public void test3() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		var json = JsonWriter.local().clear().write(c).toString();
		Assert.assertEquals("{\"a\":{\"a\":1,\"b\":-1}}", json);
	}

	public void test4() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		var json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_PRETTY_FORMAT).write(c).toString();
		Assert.assertEquals("{\n" +
				"\t\"a\": {\n" +
				"\t\t\"a\": 1,\n" +
				"\t\t\"b\": -1\n" +
				"\t}\n" +
				"}", json);
	}

	public void test5() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		var json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY).write(c).toString();
		Assert.assertEquals("{a:{a:1,b:-1}}", json);
	}

	public void test6() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		var json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_WRITE_NULL).write(c).toString();
		Assert.assertEquals("{\"a\":{\"a\":1,\"b\":-1},\"c\":null}", json);
	}

	public void test7() {
		System.out.println(System.getProperty("java.version"));
		System.out.println(Json.getClassMeta(Inet4Address.class));
	}

	static class D {
		final HashMap<Integer, Integer> m = new HashMap<>();
	}

	public void test8() throws ReflectiveOperationException {
		D d = new D();
		d.m.put(123, 456);
		String s = JsonWriter.local().clear().setFlags(0).write(d).toString();
		Assert.assertEquals("{\"m\":{\"123\":456}}", s);
		d.m.clear();
		JsonReader.local().buf("{\"m\":{123:456}}").parse(d);
		Assert.assertEquals(1, d.m.size());
		Assert.assertEquals(String.valueOf(456), String.valueOf(d.m.get(123)));
		Assert.assertEquals(Integer.class, d.m.entrySet().iterator().next().getKey().getClass());
	}

	static class E {
		int a;
		E e;
	}

	public void test9() {
		E e = new E();
		e.a = 123;
		e.e = e;
		var json = JsonWriter.local().clear().setDepthLimit(4).write(e).toString();
		Assert.assertEquals("{\"a\":123,\"e\":{\"a\":123,\"e\":{\"a\":123,\"e\":{\"a\":123,\"e\":\"!OVERDEPTH!\"}}}}",
				json);
	}

	abstract static class F1 {
		int f;
	}

	@SuppressWarnings({"serial", "RedundantSuppression"})
	static class F2 extends ArrayList<Object> {
		private F2() {
		}
	}

	static class G {
		final Set<Integer> set1 = new HashSet<>();
		HashSet<Integer> set2;
		Set<Integer> set3;
		Map<String, String> e1;
		F1 f1;
		F2 f2;
	}

	@SuppressWarnings("null")
	public void testA() throws ReflectiveOperationException {
		G g = JsonReader.local().buf("{\"set1\":[123,456],\"set2\":[789],\"set3\":[],\"e1\":{\"1\":[]},\"f2\":[222]}")
				.parse(G.class);
		Assert.assertNotNull(g);
		Assert.assertEquals(HashSet.class, g.set1.getClass());
		Assert.assertNotNull(g.set2);
		Assert.assertEquals(HashSet.class, g.set2.getClass());
		Assert.assertNotNull(g.set3);
		Assert.assertEquals(HashSet.class, g.set3.getClass());
		Assert.assertEquals(222, ((Number)g.f2.get(0)).intValue());
	}

	public void testB() {
		int[] a = new int[]{1, 2, 3};
		String[] b = new String[]{"a", "b", "c"};
		List<Integer> c = new ArrayList<>();
		Collections.addAll(c, 1, 2, 3);
		List<String> d = new ArrayList<>();
		Collections.addAll(d, b);
		Map<Integer, String> e = new TreeMap<>();
		e.put(1, "a");
		e.put(2, "b");
		e.put(3, "c");

		String json;
		json = JsonWriter.local().clear().write(a).toString();
		Assert.assertEquals("[1,2,3]", json);
		json = JsonWriter.local().clear().write(b).toString();
		Assert.assertEquals("[\"a\",\"b\",\"c\"]", json);
		json = JsonWriter.local().clear().write(c).toString();
		Assert.assertEquals("[1,2,3]", json);
		json = JsonWriter.local().clear().write(d).toString();
		Assert.assertEquals("[\"a\",\"b\",\"c\"]", json);
		json = JsonWriter.local().clear().write(e).toString();
		Assert.assertEquals("{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}", json);

		JsonWriter.local().setPrettyFormat(true).setWrapElement(true);

		json = JsonWriter.local().clear().write(a).toString();
		Assert.assertEquals("[\n" +
				"\t1,\n" +
				"\t2,\n" +
				"\t3\n" +
				"]", json);
		json = JsonWriter.local().clear().write(b).toString();
		Assert.assertEquals("[\n" +
				"\t\"a\",\n" +
				"\t\"b\",\n" +
				"\t\"c\"\n" +
				"]", json);
		json = JsonWriter.local().clear().write(c).toString();
		Assert.assertEquals("[\n" +
				"\t1,\n" +
				"\t2,\n" +
				"\t3\n" +
				"]", json);
		json = JsonWriter.local().clear().write(d).toString();
		Assert.assertEquals("[\n" +
				"\t\"a\",\n" +
				"\t\"b\",\n" +
				"\t\"c\"\n" +
				"]", json);
		json = JsonWriter.local().clear().write(e).toString();
		Assert.assertEquals("{\n" +
				"\t\"1\": \"a\",\n" +
				"\t\"2\": \"b\",\n" +
				"\t\"3\": \"c\"\n" +
				"}", json);
	}

	public void testC() {
		var s = String.format("%X", JsonWriter.umulHigh(0x8000_0000_0000_0001L, 0x8000_0000_0000_0000L));
		Assert.assertEquals("3FFFFFFFFFFFFFFF", s);
		if (Integer.parseInt(System.getProperty("java.version").replaceFirst("^1\\.", "").replaceFirst("\\D.*", "")) > 8) {
			s = String.format("%X", JsonWriter.umulHigh9(0x8000_0000_0000_0001L, 0x8000_0000_0000_0000L));
			Assert.assertEquals("4000000000000000", s);
		}
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		var t = new TestJson();
		t.test1();
		t.test2();
		t.test3();
		t.test4();
		t.test5();
		t.test6();
		t.test7();
		t.test8();
		t.test9();
		t.testA();
		t.testB();
		t.testC();
		System.out.println(t.getClass().getSimpleName() + ": 12 tests OK!");
	}
}
