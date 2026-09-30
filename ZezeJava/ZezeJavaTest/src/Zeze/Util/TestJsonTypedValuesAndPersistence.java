package Zeze.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import Zeze.Serialize.Helper;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestJsonTypedValuesAndPersistence {
	public static class Node {
		public int value;
	}
	public static class Container {
		public List<Node> list;
		public Map<String, Node> map;
	}
	public static class BooleanKeys {
		public Map<Boolean, Integer> values;
	}

	@Test
	public void typedScalarsAndNullKeepTheirValues() throws Exception {
		assertEquals(Arrays.asList(1, null, -2),
				new JsonReader().buf("[1,null,-2]").parseArray(null, Integer.class));
		assertEquals(Arrays.asList("one", null, "two"),
				new JsonReader().buf("[\"one\",null,\"two\"]").parseArray(null, String.class));
		assertEquals(Arrays.asList(true, null, false),
				new JsonReader().buf("[true,null,false]").parseArray(null, Boolean.class));
		assertEquals(List.of(true), new JsonReader().buf("[true]").parseArray(null, Boolean.class));
		assertEquals(List.of(true, false),
				new JsonReader().buf("[true/**/,false// end\n]").parseArray(null, Boolean.class));
		assertEquals(Map.of(true, 1, false, 2), new JsonReader().buf("{values:{true:1,false:2}}")
				.parse(BooleanKeys.class).values);
		assertEquals(Arrays.asList(true, null, false, 3, "x", Map.of("k", 1), List.of(2, 3)),
				new JsonReader().buf("[true,null,false,3,\"x\",{k:1},[2,3]]")
						.parseArray(null, Object.class));
		assertEquals(Map.of("a", true, "b", false),
				new JsonReader().buf("{a:true,b:false}").parse(Object.class));
		assertEquals(true, new JsonReader().buf("true").parse(Object.class));
		assertEquals(7, new JsonReader().buf("7").parse(Object.class));
		assertNull(new JsonReader().buf("null").parse(Object.class));
	}

	@Test
	public void typedArrayPreservesCustomScalarParser() throws Exception {
		var json = new Json();
		int[] calls = {0};
		json.getClassMeta(String.class).setParser((reader, meta, field, value, parent) -> {
			calls[0]++;
			return "custom:" + JsonReader.parseStringKey(reader, reader.next());
		});
		assertEquals(Arrays.asList("custom:a", null, "custom:b"),
				new JsonReader().buf("[\"a\",null,\"b\"]").parseArray(json, null, String.class));
		assertEquals(2, calls[0]);
		assertEquals("custom:top", new JsonReader().buf("\"top\"").parse(json, String.class));
		assertEquals(3, calls[0]);
		assertNull(new JsonReader().buf("null").parse(json, String.class));
		assertEquals(3, calls[0]);
		assertEquals(List.of("a"), new JsonReader().buf("[\"a\"]").parseArray(null, String.class));
	}

	@Test
	public void typedObjectsKeepNullInListsAndMaps() throws Exception {
		var value = new JsonReader().buf("{list:[null,{value:7}],map:{a:null,b:{value:8}}}")
				.parse(Container.class);
		assertNotNull(value);
		assertNull(value.list.get(0));
		assertEquals(7, value.list.get(1).value);
		assertTrue(value.map.containsKey("a"));
		assertNull(value.map.get("a"));
		assertEquals(8, value.map.get("b").value);
		assertNull(new JsonReader().buf("null").parse(Node.class));
	}




}
