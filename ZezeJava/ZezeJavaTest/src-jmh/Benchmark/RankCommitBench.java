package Benchmark;

import java.util.concurrent.TimeUnit;

import GameSim.GameConfig;
import GameSim.GameWorld;
import GameSim.OpRecorder;
import Zeze.Builtin.Game.Rank.BValueLong;
import Zeze.Transaction.Procedure;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

@NullMarked
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(1)
public class RankCommitBench {
	@Param({"NONE", "LISTENER"})
	public String consumers;

	private GameWorld world;
	private @Nullable String previousListener;
	private long sequence;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		previousListener = System.getProperty("game.listener");
		System.setProperty("game.listener", String.valueOf("LISTENER".equals(consumers)));
		OpRecorder.init(1);
		world = new GameWorld(new GameConfig());
		try {
			world.start();
			check(world.zeze.newProcedure(() -> {
				for (int i = 0; i < 16; ++i)
					world.rank.updateRank(0, world.damageBoard, i, new BValueLong(i));
				return Procedure.Success;
			}, "RankCommitBench.prefill").call());
		} catch (Throwable ex) {
			world.stop();
			restoreListener();
			throw ex;
		}
	}

	@Benchmark
	public long commit() {
		long score = ++sequence;
		check(world.zeze.newProcedure(() -> {
			world.rank.updateRank(0, world.damageBoard, 0, new BValueLong(score));
			return Procedure.Success;
		}, "RankCommitBench.commit").call());
		return score ^ world.listenerNotifications();
	}

	private static void check(long result) {
		if (result != Procedure.Success)
			throw new IllegalStateException("transaction result=" + result);
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		try {
			world.stop();
		} finally {
			restoreListener();
		}
	}

	private void restoreListener() {
		if (previousListener == null)
			System.clearProperty("game.listener");
		else
			System.setProperty("game.listener", previousListener);
	}
}
