package rocksdynamic.Fixture;

import Zeze.Raft.RocksRaft.Bean;
import org.jspecify.annotations.Nullable;

public class DynamicFixture extends AbstractDynamicFixture {
	public static long identifyCustomPayload(Bean bean) {
		return bean instanceof CustomPayload ? 71 : 72;
	}

	public static @Nullable Bean createCustomPayload(long typeId) {
		if (typeId == 71)
			return new CustomPayload();
		if (typeId == 72)
			return new Quantity();
		return null;
	}
}
