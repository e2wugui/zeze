package Zeze.Onz;

import java.lang.reflect.Field;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A terminal coordinator must reject restart before starting its network services. */
@Fast
public class TestStoppedServerRejectsStart {
	private static final class ObservedService extends OnzServerService {
		int startCalls;

		ObservedService() {
			super(new Config());
		}

		@Override
		public void start() {
			startCalls++;
			throw new IllegalStateException("network start was reached");
		}
	}

	@Test
	public void stoppedCoordinatorRejectsStartBeforeNetworkSideEffects() throws Exception {
		var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		var server = (OnzServer)((Unsafe)unsafeField.get(null)).allocateInstance(OnzServer.class);
		var service = new ObservedService();
		set(server, "stopped", true); // The terminal state left by stop(); no live services or databases.
		set(server, "service", service);
		assertThrows(IllegalStateException.class, server::start);
		assertEquals(0, service.startCalls, "A stopped coordinator must not revive any network resource");
	}

	private static void set(OnzServer server, String name, Object value) throws Exception {
		Field field = OnzServer.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(server, value);
	}
}
