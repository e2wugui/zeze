package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import demo.Module1.BSimple;
import demo.Module1.tflush;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** History encoding and full-value listener projections consume a canonical payload independently. */
@Fast
public class TestMergeChangedAfterEncode {
	private static BSimple bean(int value) {
		var bean = new BSimple();
		bean.setInt_1(value);
		return bean;
	}

	private static ByteBuffer encode(Log log) {
		var out = ByteBuffer.Allocate();
		log.encode(out);
		return ByteBuffer.Wrap(out.Copy());
	}

	@Test
	public void encodingBeforeListenerViewKeepsBothConsumersComplete() throws Exception {
		verifyConsumerOrder(false);
	}

	@Test
	public void listenerViewBeforeEncodingKeepsBothConsumersComplete() throws Exception {
		verifyConsumerOrder(true);
	}

	@SuppressWarnings("unchecked")
	private static void verifyConsumerOrder(boolean viewFirst) throws Exception {
		var app = Map2TestSupport.newApp("Map2ConsumerOrder");
		var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
		var notified = new AtomicInteger();
		try {
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				root.getMap41().put(1L, bean(5));
				root.getSortedmap2().put(1, bean(5));
				return Procedure.Success;
			}, "seed").call());

			table.getChangeListenerMap().addListener((key, record) -> {
				var mapLog = (LogMap2<Long, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_map41));
				var sortedLog = (LogSortedMap2<Integer, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_sortedmap2));
				var mapValues = new HashMap<Long, BSimple>();
				var sortedValues = new HashMap<Integer, BSimple>();
				var mapView = record.<Long, BSimple>map2ListenerView(tflush.VAR_map41);
				var sortedView = record.<Integer, BSimple>sortedMap2ListenerView(tflush.VAR_sortedmap2);
				byte[] initialMap;
				byte[] initialSorted;
				if (viewFirst) {
					mapView.forEachUpsert(mapValues::put);
					sortedView.forEachUpsert(sortedValues::put);
					initialMap = encode(mapLog).Copy();
					initialSorted = encode(sortedLog).Copy();
				} else {
					initialMap = encode(mapLog).Copy();
					initialSorted = encode(sortedLog).Copy();
					mapView.forEachUpsert(mapValues::put);
					sortedView.forEachUpsert(sortedValues::put);
				}
				mapView.forEachUpsert(mapValues::put);
				sortedView.forEachUpsert(sortedValues::put);
				assertEquals(42, requireNonNull(mapValues.get(1L)).getInt_1());
				assertEquals(42, requireNonNull(sortedValues.get(1)).getInt_1());
				assertTrue(mapLog.getReplaced().isEmpty(), "view must not materialize edits into the wire replacement set");
				assertTrue(sortedLog.getReplaced().isEmpty());
				assertEquals(1, mapLog.getChangedWithKey().size());
				assertEquals(1, sortedLog.getChangedWithKey().size());
				assertArrayEquals(initialMap, encode(mapLog).Copy());
				assertArrayEquals(initialSorted, encode(sortedLog).Copy());

				var mapFollower = new PMap2<Long, BSimple>(Long.class, BSimple.class);
				mapFollower.put(1L, bean(5));
				var replay = Map2TestSupport.wireLog(mapFollower);
				replay = Zeze.Transaction.Map2TestSupport.decode(replay, encode(mapLog));
				mapFollower.followerApply(replay);
				assertEquals(42, requireNonNull(mapFollower.get(1L)).getInt_1());
				var sortedFollower = new PSortedMap2<Integer, BSimple>(Integer.class, BSimple.class);
				sortedFollower.put(1, bean(5));
				var sortedReplay = Map2TestSupport.wireLog(sortedFollower);
				sortedReplay = Zeze.Transaction.Map2TestSupport.decode(sortedReplay, encode(sortedLog));
				sortedFollower.followerApply(sortedReplay);
				assertEquals(42, requireNonNull(sortedFollower.get(1)).getInt_1());
				notified.incrementAndGet();
			});

			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var root = table.getOrAdd(1L);
				requireNonNull(root.getMap41().get(1L)).setInt_1(42);
				requireNonNull(root.getSortedmap2().get(1)).setInt_1(42);
				return Procedure.Success;
			}, "edit").call());
			assertEquals(1, notified.get(), "listener assertions must complete, not be swallowed by callback isolation");
		} finally {
			app.stop();
		}
	}
}
