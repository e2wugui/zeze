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
