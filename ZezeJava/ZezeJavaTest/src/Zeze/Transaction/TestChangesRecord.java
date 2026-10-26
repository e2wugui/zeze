package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@Fast
public class TestChangesRecord {
	private static ByteBuffer edit(@Nullable LogBean root) {
		var bb = ByteBuffer.Allocate();
		bb.WriteUInt(Changes.Record.Edit);
		bb.WriteUInt(root != null ? 1 : 0);
		if (root != null)
			root.encode(bb);
		return bb;
	}

	private static LogBean rootLog(int value) {
		var root = new LogBean(0, null);
		root.getVariablesOrNew().put(1, Map2TestSupport.intLog(1, value));
		return root;
	}

	@Test
	public void emptyAndSingleRootEditsRoundTrip() {
		Helper.registerLogs();
		for (boolean hasRoot : new boolean[]{false, true}) {
			var input = edit(hasRoot ? rootLog(7) : null);
			var expected = input.Copy();
			var record = new Changes.Record(new tflush());
			record.decode(input);
			assertEquals(Changes.Record.Edit, record.getState());
			assertTrue(input.isEmpty());
			assertNull(record.getValue());
			if (hasRoot)
				assertEquals(7, requireNonNull(record.getVariableLog(1)).intValue());
			else
				assertNull(record.getLogBean());
			var output = ByteBuffer.Allocate();
			record.encode(output);
			assertArrayEquals(expected, output.Copy());
		}
	}

	@Test
	public void decodingAnotherStateDropsPreviousValueAndRoot() {
		Helper.registerLogs();
		var record = new Changes.Record(new tflush());
		var value = new BValue();
		value.setInt_1(3);
		var put = ByteBuffer.Allocate();
		put.WriteUInt(Changes.Record.Put);
		value.encode(put);
		record.decode(put);
		assertEquals(3, ((BValue)requireNonNull(record.getValue())).getInt_1());

		record.decode(edit(rootLog(7)));
		assertNull(record.getValue());
		assertEquals(7, requireNonNull(record.getVariableLog(1)).intValue());
		record.decode(edit(null));
		assertNull(record.getLogBean());

		record.decode(edit(rootLog(9)));
		var remove = ByteBuffer.Allocate();
		remove.WriteUInt(Changes.Record.Remove);
		record.decode(remove);
		assertEquals(Changes.Record.Remove, record.getState());
		assertNull(record.getLogBean());
		assertNull(record.getValue());
	}

	@Test
	public void editRejectsMultipleRecordRoots() {
		var input = ByteBuffer.Allocate();
		input.WriteUInt(Changes.Record.Edit);
		input.WriteUInt(2);
		assertThrows(IllegalArgumentException.class, () -> new Changes.Record(new tflush()).decode(input));
	}

	@Test
	public void staleRootsDoNotReplaceTheCurrentPutOrEdit() throws Exception {
		Helper.registerLogs();
		var app = Map2TestSupport.newApp("changesRecordRoots");
		try {
			var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
			var old = new AtomicReference<BValue>();
			var fresh = new BValue();
			var phase = new AtomicInteger();
			var callbacks = new AtomicInteger();
			table.getChangeListenerMap().addListener((key, record) -> {
				if (phase.get() == 0)
					return;
				var encoded = ByteBuffer.Allocate();
				record.encode(encoded);
				var decoded = new Changes.Record(table);
				decoded.decode(encoded);
				assertTrue(encoded.isEmpty());
				assertSame(fresh, record.getValue());
				if (phase.get() == 1) {
					assertEquals(Changes.Record.Put, record.getState());
					assertNull(record.getLogBean());
					assertEquals(30, ((BValue)requireNonNull(decoded.getValue())).getInt_1());
				} else {
					assertEquals(Changes.Record.Edit, record.getState());
					assertSame(fresh, requireNonNull(record.getLogBean()).getThis());
					var follower = new BValue();
					follower.setInt_1(30);
					follower.followerApply(requireNonNull(decoded.getLogBean()));
					assertEquals(40, follower.getInt_1());
				}
				callbacks.incrementAndGet();
			});
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				var value = table.getOrAdd(1L);
				value.setInt_1(1);
				old.set(value);
				return Procedure.Success;
			}, "seed").call());
			phase.set(1);
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.get(1L);
				requireNonNull(old.get()).setInt_1(10);
				table.put(1L, fresh);
				requireNonNull(old.get()).setInt_1(20);
				fresh.setInt_1(30);
				return Procedure.Success;
			}, "replaceRoot").call());
			phase.set(2);
			assertEquals(Procedure.Success, app.newProcedure(() -> {
				assertSame(fresh, table.get(1L));
				requireNonNull(old.get()).setInt_1(99);
				fresh.setInt_1(40);
				return Procedure.Success;
			}, "editCurrentAndStaleRoots").call());
			assertEquals(2, callbacks.get());
		} finally {
			app.stop();
		}
	}
}
