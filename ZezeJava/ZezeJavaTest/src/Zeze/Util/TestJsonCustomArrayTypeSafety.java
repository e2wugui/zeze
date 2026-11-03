package Zeze.Util;

import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestJsonCustomArrayTypeSafety {
	public static class Item {
		public int value;
	}

	public static class Holder {
		public Item @Nullable [] items;
	}

	@Test
	public void unsupportedArrayKeepsItsDeclaredType() {
		assertSame(Item[].class, Json.instance.getClassMeta(Holder.class).fieldMetas[0].klass);
		var holder = new Holder();
		assertThrows(InstantiationException.class, () -> new JsonReader().buf("{items:[{value:7}]}").parse(holder));
		assertNull(holder.items);
	}
}
