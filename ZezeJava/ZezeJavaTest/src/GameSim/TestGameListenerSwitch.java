package GameSim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import harness.Fast;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.Test;

@Fast
@Isolated
public class TestGameListenerSwitch {
	@Test
	public void listenerIsDisabledByDefault() throws Exception {
		var previous = System.getProperty("game.listener");
		try {
			System.clearProperty("game.listener");
			OpRecorder.init(0);
			var world = new GameWorld(new GameConfig());
			world.start();
			try {
				assertListeners(world, 0);
			} finally {
				world.stop();
			}
		} finally {
			restore(previous);
		}
	}

	@Test
	public void listenerCanBeEnabledForEveryTable() throws Exception {
		var previous = System.getProperty("game.listener");
		try {
			System.setProperty("game.listener", "true");
			OpRecorder.init(0);
			var world = new GameWorld(new GameConfig());
			world.start();
			try {
				assertListeners(world, 1);
			} finally {
				world.stop();
			}
		} finally {
			restore(previous);
		}
	}

	private static void assertListeners(GameWorld world, int expected) {
		var tables = world.zeze.getTables().values();
		assertFalse(tables.isEmpty());
		for (var table : tables)
			assertEquals(expected, table.getChangeListenerMap().getListeners().size(), table.getName());
	}

	private static void restore(String previous) {
		if (previous == null)
			System.clearProperty("game.listener");
		else
			System.setProperty("game.listener", previous);
	}
}
