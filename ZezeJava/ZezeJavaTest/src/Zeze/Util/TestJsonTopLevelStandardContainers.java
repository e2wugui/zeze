package Zeze.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestJsonTopLevelStandardContainers {
	@Test
	public void concreteListReadsItsWrittenElements() {
		var source = new ArrayList<>(List.of(1, "two", List.of(3)));
		assertEquals(source, Json.parse(Json.toCompactString(source), ArrayList.class));
	}

	@Test
	public void concreteMapReadsItsWrittenEntries() {
		var source = new HashMap<>(Map.of("one", 1, "nested", Map.of("two", 2)));
		assertEquals(source, Json.parse(Json.toCompactString(source), HashMap.class));
	}

	@Test
	public void existingContainersUseTheExplicitContainerParsingSemantics() throws ReflectiveOperationException {
		var list = new ArrayList<>(List.of(1));
		assertSame(list, new JsonReader().buf("[2]").parse(list));
		assertEquals(List.of(1, 2), list);
		var map = new HashMap<>(Map.of("one", 1));
		assertSame(map, new JsonReader().buf("{two:2}").parse(map));
		assertEquals(Map.of("one", 1, "two", 2), map);
	}

	@Test
	public void registeredParserStillTakesPrecedence() throws ReflectiveOperationException {
		var json = new Json();
		json.getClassMeta(ArrayList.class).setParser((reader, meta, field, obj, parent) -> new ArrayList<>(List.of(7)));
		assertEquals(List.of(7), new JsonReader().buf("[2]").parse(json, ArrayList.class));
	}
}
