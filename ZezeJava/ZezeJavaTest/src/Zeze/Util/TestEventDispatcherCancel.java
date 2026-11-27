package Zeze.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * EventDispatcher.Canceler 槽位身份契约：
 * 1) 同一 EventHandle 重复注册后，对单个 Canceler 重复 cancel() 不得误删其他注册槽位；
 * 2) 取消靠后的同 handle 注册槽位时，保留靠前槽位的原有执行顺序；
 * 3) 即使两个不同 EventHandle 实例的 equals() 相等，取消其中一个也不得误删另一个。
 */
@Fast
public class TestEventDispatcherCancel {
	private static final EventDispatcher.EventArgument EMPTY_ARG = new EventDispatcher.EventArgument() {
	};

	@Test
	public void repeatedCancelDoesNotRemoveSiblingRegistration() throws Exception {
		var dispatcher = new EventDispatcher("UnitTest.EventDispatcher.cancel");
		var calls = new AtomicInteger();
		EventDispatcher.EventHandle handle = (sender, arg) -> {
			calls.incrementAndGet();
			return 0;
		};

		var c1 = dispatcher.add(EventDispatcher.Mode.RunEmbed, handle);
		var c2 = dispatcher.add(EventDispatcher.Mode.RunEmbed, handle);

		Assertions.assertEquals(0, dispatcher.triggerEmbed(this, EMPTY_ARG));
		Assertions.assertEquals(2, calls.get());

		c1.cancel();
		c1.cancel(); // 重复取消不得影响 c2
		calls.set(0);
		Assertions.assertEquals(0, dispatcher.triggerEmbed(this, EMPTY_ARG));
		Assertions.assertEquals(1, calls.get(), "重复cancel同一Canceler不得移除同handle的其他注册");

		c2.cancel();
		calls.set(0);
		Assertions.assertEquals(0, dispatcher.triggerEmbed(this, EMPTY_ARG));
		Assertions.assertEquals(0, calls.get());
	}

	@Test
	public void cancelingLaterRegistrationPreservesOrder() throws Exception {
		var dispatcher = new EventDispatcher("UnitTest.EventDispatcher.cancelOrder");
		var order = new ArrayList<String>();
		EventDispatcher.EventHandle shared = (sender, arg) -> {
			order.add("shared");
			return 0;
		};
		EventDispatcher.EventHandle middle = (sender, arg) -> {
			order.add("middle");
			return 0;
		};

		var c1 = dispatcher.add(EventDispatcher.Mode.RunEmbed, shared);
		var cMid = dispatcher.add(EventDispatcher.Mode.RunEmbed, middle);
		var c2 = dispatcher.add(EventDispatcher.Mode.RunEmbed, shared);

		c2.cancel();
		Assertions.assertEquals(0, dispatcher.triggerEmbed(this, EMPTY_ARG));
		Assertions.assertEquals(List.of("shared", "middle"), order,
				"取消后注册的槽位不得误删先注册的槽位导致顺序倒置");

		c1.cancel();
		cMid.cancel();
	}

	@Test
	public void cancelingEqualHandleDoesNotRemoveDistinctRegistration() throws Exception {
		var dispatcher = new EventDispatcher("UnitTest.EventDispatcher.cancelEquals");
		var invoked = new ArrayList<String>();
		var h1 = new AlwaysEqualHandle("first", invoked);
		var h2 = new AlwaysEqualHandle("second", invoked);
		Assertions.assertEquals(h1, h2);

		var c1 = dispatcher.add(EventDispatcher.Mode.RunEmbed, h1);
		var c2 = dispatcher.add(EventDispatcher.Mode.RunEmbed, h2);

		c2.cancel();
		Assertions.assertEquals(0, dispatcher.triggerEmbed(this, EMPTY_ARG));
		Assertions.assertEquals(List.of("first"), invoked,
				"取消 h2 不得因 equals 相等而误删先注册的 h1");

		c1.cancel();
	}

	private static final class AlwaysEqualHandle implements EventDispatcher.EventHandle {
		private final String tag;
		private final List<String> invoked;

		private AlwaysEqualHandle(String tag, List<String> invoked) {
			this.tag = tag;
			this.invoked = invoked;
		}

		@Override
		public long invoke(Object sender, EventDispatcher.EventArgument arg) {
			invoked.add(tag);
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof AlwaysEqualHandle;
		}

		@Override
		public int hashCode() {
			return 1;
		}
	}
}
