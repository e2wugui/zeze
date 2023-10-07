package UnitTest.Zeze.Util;

import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
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
		Map<A, Integer> m = new HashMap<>();
	}

	public void test1() throws ReflectiveOperationException {
		byte[] buf = "{a:[{x:1,y:2},{x:3,y:4},{x:5,y:6}]}".getBytes();
		Object obj = JsonReader.local().buf(buf).parse();
		assertNotNull(obj);
		assertEquals("{a=[{x=1, y=2}, {x=3, y=4}, {x=5, y=6}]}", obj.toString());
	}

	public void test2() throws ReflectiveOperationException {
		C c = JsonReader.local().buf("{a:{a:1,b:2}}").parse(C.class);
		assertNotNull(c);
		assertEquals(B.class, c.a.getClass());
		assertEquals(1, c.a.a);
		assertEquals(2, ((B)c.a).b);

		c.a = new A();
		c = JsonReader.local().buf("{a:{a:3,b:4}}").parse(c);
		assertNotNull(c);
		assertEquals(A.class, c.a.getClass());
		assertEquals(3, c.a.a);

		c.a = null;
		c = JsonReader.local().buf("{a:{a:5,b:6}}").parse(c);
		assertNotNull(c);
		assertEquals(A.class, c.a.getClass());
		assertEquals(5, c.a.a);

		Json.instance.getClassMeta(A.class).setParser((json, __, ___, ____, _____) -> json.parse(B.class));
		c.a = null;
		c = JsonReader.local().buf("{a:{a:7,b:8}}").parse(c);
		assertNotNull(c);
		assertEquals(B.class, c.a.getClass());
		assertEquals(7, c.a.a);
		assertEquals(8, ((B)c.a).b);
	}

	public void test3() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		String json = JsonWriter.local().clear().setNoQuoteKey(false).write(c).toString();
		assertEquals("{\"a\":{\"a\":1,\"b\":-1},\"m\":{}}", json);
	}

	public void test4() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		String json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_PRETTY_FORMAT).write(c).toString();
		assertEquals("{\n" +
				"\t\"a\": {\n" +
				"\t\t\"a\": 1,\n" +
				"\t\t\"b\": -1\n" +
				"\t},\n" +
				"\t\"m\": {}\n" +
				"}", json);
	}

	public void test5() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		String json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_NO_QUOTE_KEY).write(c).toString();
		assertEquals("{a:{a:1,b:-1},m:{}}", json);
	}

	public void test6() {
		C c = new C();
		c.a.a = 1;
		((B)c.a).b = -1;
		String json = JsonWriter.local().clear().setFlags(JsonWriter.FLAG_WRITE_NULL).write(c).toString();
		assertEquals("{\"a\":{\"a\":1,\"b\":-1},\"c\":null,\"m\":{}}", json);
	}

	public void test7() {
		System.out.println(System.getProperty("java.version"));
		System.out.println(Json.instance.getClassMeta(Inet4Address.class));
	}

	static class D {
		final HashMap<Integer, Integer> m = new HashMap<>();
	}

	public void test8() throws ReflectiveOperationException {
		D d = new D();
		d.m.put(123, 456);
		String s = JsonWriter.local().clear().setFlags(0).write(d).toString();
		assertEquals("{\"m\":{\"123\":456}}", s);
		d.m.clear();
		JsonReader.local().buf("{\"m\":{123:456}}").parse(d);
		assertEquals(1, d.m.size());
		assertEquals(String.valueOf(456), String.valueOf(d.m.get(123)));
		assertEquals(Integer.class, d.m.entrySet().iterator().next().getKey().getClass());
	}

	static class E {
		int a;
		E e;
	}

	public void test9() {
		E e = new E();
		e.a = 123;
		e.e = e;
		String json = JsonWriter.local().clear().setDepthLimit(4).write(e).toString();
		assertEquals("{\"a\":123,\"e\":{\"a\":123,\"e\":{\"a\":123,\"e\":{\"a\":123,\"e\":\"!OVERDEPTH!\"}}}}", json);
	}

	abstract static class F1 {
		int f;
	}

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

	public void testA() throws ReflectiveOperationException {
		G g = JsonReader.local().buf("{\"set1\":[123,456],\"set2\":[789],\"set3\":[],\"e1\":{\"1\":[]},\"f2\":[222]}")
				.parse(G.class);
		assertNotNull(g);
		assertEquals(HashSet.class, g.set1.getClass());
		assertNotNull(g.set2);
		assertEquals(HashSet.class, g.set2.getClass());
		assertNotNull(g.set3);
		assertEquals(HashSet.class, g.set3.getClass());
		assertEquals(222, ((Number)g.f2.get(0)).intValue());
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
		assertEquals("[1,2,3]", json);
		json = JsonWriter.local().clear().write(b).toString();
		assertEquals("[\"a\",\"b\",\"c\"]", json);
		json = JsonWriter.local().clear().write(c).toString();
		assertEquals("[1,2,3]", json);
		json = JsonWriter.local().clear().write(d).toString();
		assertEquals("[\"a\",\"b\",\"c\"]", json);
		json = JsonWriter.local().clear().write(e).toString();
		assertEquals("{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}", json);

		JsonWriter.local().setPrettyFormat(true).setWrapElement(true);

		json = JsonWriter.local().clear().write(a).toString();
		assertEquals("[\n" +
				"\t1,\n" +
				"\t2,\n" +
				"\t3\n" +
				"]", json);
		json = JsonWriter.local().clear().write(b).toString();
		assertEquals("[\n" +
				"\t\"a\",\n" +
				"\t\"b\",\n" +
				"\t\"c\"\n" +
				"]", json);
		json = JsonWriter.local().clear().write(c).toString();
		assertEquals("[\n" +
				"\t1,\n" +
				"\t2,\n" +
				"\t3\n" +
				"]", json);
		json = JsonWriter.local().clear().write(d).toString();
		assertEquals("[\n" +
				"\t\"a\",\n" +
				"\t\"b\",\n" +
				"\t\"c\"\n" +
				"]", json);
		json = JsonWriter.local().clear().write(e).toString();
		assertEquals("{\n" +
				"\t\"1\": \"a\",\n" +
				"\t\"2\": \"b\",\n" +
				"\t\"3\": \"c\"\n" +
				"}", json);
	}

	public void testC() {
		String s = String.format("%X", JsonWriter.umulHigh(0x8000_0000_0000_0001L, 0x8000_0000_0000_0000L));
		assertEquals("4000000000000000", s);
		if (Integer.parseInt(System.getProperty("java.version").replaceFirst("^1\\.", "").replaceFirst("\\D.*", "")) > 8) {
			s = String.format("%X", JsonWriter.umulHigh9(0x8000_0000_0000_0001L, 0x8000_0000_0000_0000L));
			assertEquals("4000000000000000", s);
		}
	}

	public void testD() throws ReflectiveOperationException {
		byte[] b = JsonReader.local().buf("'\\u001F\\u03A0\\u9abf\\uD955\\udeaa'").parseByteString();
		assertNotNull(b);
		String s = new String(b, StandardCharsets.UTF_8);
		assertEquals(10, b.length);
		assertEquals(5, s.length());
		assertEquals(0x1f, s.charAt(0));
		assertEquals(0x3a0, s.charAt(1));
		assertEquals(0x9abf, s.charAt(2));
		assertEquals(0xd955, s.charAt(3));
		assertEquals(0xdeaa, s.charAt(4));

		s = JsonReader.local().buf("'\\u001F\\u03A0\\u9abf\\uD955\\udeaa'").parseString();
		assertNotNull(s);
		assertEquals(5, s.length());
		assertEquals(0x1f, s.charAt(0));
		assertEquals(0x3a0, s.charAt(1));
		assertEquals(0x9abf, s.charAt(2));
		assertEquals(0xd955, s.charAt(3));
		assertEquals(0xdeaa, s.charAt(4));

		s = JsonReader.local().buf("\\u001F\\u03A0\\u9abf\\uD955\\udeaa ").parseStringNoQuot();
		assertNotNull(s);
		assertEquals(5, s.length());
		assertEquals(0x1f, s.charAt(0));
		assertEquals(0x3a0, s.charAt(1));
		assertEquals(0x9abf, s.charAt(2));
		assertEquals(0xd955, s.charAt(3));
		assertEquals(0xdeaa, s.charAt(4));

		JsonWriter.local().clear().write(s, true);
		char[] c = JsonWriter.local().toChars();
		assertEquals(15, JsonWriter.local().size()); // 6+2+3+4
		assertEquals(10, c.length); // 6+1+1+2
		assertEquals("\\u001F", new String(c, 0, 6));
		assertEquals(0x3a0, c[6]);
		assertEquals(0x9abf, c[7]);
		assertEquals(0xd955, c[8]);
		assertEquals(0xdeaa, c[9]);
	}

	public void testE() throws ReflectiveOperationException {
		Object o = JsonReader.local().buf("[Infinity,-Infinity,NaN,0x1234567890abcdef,-0x1]").parse();
		assertNotNull(o);
		assertEquals(ArrayList.class, o.getClass());
		ArrayList<?> a = (ArrayList<?>)o;
		assertEquals(5, a.size());
		assertEquals(Double.POSITIVE_INFINITY, a.get(0));
		assertEquals(Double.NEGATIVE_INFINITY, a.get(1));
		assertEquals(Double.NaN, a.get(2));
		assertEquals(0x1234567890abcdefL, a.get(3));
		assertEquals(-1, a.get(4));
	}

	public static void main(String[] args) throws ReflectiveOperationException {
		TestJson t = new TestJson();
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
		t.testD();
		t.testE();
		System.out.println(t.getClass().getSimpleName() + ": 14 tests OK!");
	}
}
