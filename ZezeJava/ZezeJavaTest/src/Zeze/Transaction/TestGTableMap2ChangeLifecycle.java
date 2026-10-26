package Zeze.Transaction;

import Zeze.Transaction.Collections.LogMapDelta;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.BeanMap2;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Logs.LogInt;
import demo.Game.BItem;
import demo.Game.BItemReadOnly;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** Exercise the actual source collection chain through a GTable row and its inner map. */
@Fast
public class TestGTableMap2ChangeLifecycle {
	static {
		Zeze.Transaction.LogRegistry.register(LogInt.TYPE_ID, "LogInt", LogInt::decode);
	}

	private static byte[] encode(Log log) {
		var out = ByteBuffer.Allocate();
		log.encode(out);
		return out.Copy();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void beanCellEditBuildsBothCanonicalMapLayersAndReplaysInPlace() {
		var factory = GTable2.<Long, String, BItem, BItemReadOnly>getFactory(Long.class, String.class, BItem.class);
		Helper.registerLogMap2Meta(factory.getPmapMeta());
		Helper.registerLogMap2Meta(factory.getBmapMeta());
		var source = new GTable2<Long, String, BItem, BItemReadOnly>(factory);
		var sourceValue = new BItem();
		sourceValue.setCount(7); // committed scalar value; collect receives its corresponding field intent below
		source.put(1L, "column", sourceValue);
		var outer = source.getPMap2();
		Map2TestSupport.attach(outer);
		var changes = Map2TestSupport.changes();
		changes.collect(sourceValue, Map2TestSupport.intLog(2, 7));
		var outerLog = (LogMap2<Long, BeanMap2<String, BItem, BItemReadOnly>>)changes.getOrCreateLogBean(outer);
		var rowLog = requireNonNull(outerLog.getChangedWithKey().get(1L));
		var innerLog = (LogMap2<String, BItem>)requireNonNull(requireNonNull(rowLog.getVariables()).get(1));
		assertSame(outerLog, changes.getOrCreateLogBean(outer));
		assertSame(innerLog, changes.getOrCreateLogBean(requireNonNull(outer.get(1L)).getPMap2()));
		assertEquals(1, innerLog.getChangedWithKey().size());
		assertTrue(outerLog.getReplaced().isEmpty());
		assertTrue(innerLog.getReplaced().isEmpty());
		var encoded = encode(outerLog);
		assertArrayEquals(encoded, encode(outerLog));

		var input = ByteBuffer.Wrap(encoded);

		var decoded = (LogMap2<Long, BeanMap2<String, BItem, BItemReadOnly>>)Zeze.Transaction.LogRegistry.decode(factory.getPmapMeta().logTypeId, 1, input);
		assertTrue(input.isEmpty());
		var follower = new GTable2<Long, String, BItem, BItemReadOnly>(factory);
		var oldValue = new BItem();
		oldValue.setCount(5);
		follower.put(1L, "column", oldValue);
		var oldRow = follower.getPMap2().get(1L);
		follower.getPMap2().followerApply(decoded);
		assertSame(oldRow, follower.getPMap2().get(1L));
		assertSame(oldValue, follower.get(1L, "column"));
		assertEquals(7, oldValue.getCount());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void scalarRowMapStillExportsThroughTheCanonicalOuterMap() {
		var factory = GTable1.getFactory(Long.class, String.class, Integer.class);
		Helper.registerLogMap2Meta(factory.getPmapMeta());
		Helper.registerLogMap1Meta(factory.getBmapMeta());
		var source = new GTable1<Long, String, Integer>(factory);
		source.put(1L, "column", 5);
		var outer = source.getPMap2();
		Map2TestSupport.attach(outer);
		var row = requireNonNull(outer.get(1L));
		var inner = row.getPMap1();
		var innerLog = (LogMapDelta<String, Integer>)inner.createLog(ChangeCaptureMode.DELTA);
		innerLog.put("column", 42);
		innerLog.commit();
		var finalSavepoint = new Savepoint();
		finalSavepoint.putLog(innerLog);
		var changes = new Changes(true, null, finalSavepoint);
		changes.collect(innerLog);
		var outerLog = (LogMap2<Long, BeanMap1<String, Integer>>)changes.getOrCreateLogBean(outer);
		assertEquals(1, outerLog.getChangedWithKey().size());
		assertTrue(outerLog.getReplaced().isEmpty());
		var decoded = (LogMap2<Long, BeanMap1<String, Integer>>)Zeze.Transaction.LogRegistry.decode(factory.getPmapMeta().logTypeId, 1, ByteBuffer.Wrap(encode(outerLog)));
		var follower = new GTable1<Long, String, Integer>(factory);
		follower.put(1L, "column", 5);
		var oldRow = follower.getPMap2().get(1L);
		follower.getPMap2().followerApply(decoded);
		assertSame(oldRow, follower.getPMap2().get(1L));
		assertEquals(42, follower.get(1L, "column"));
	}
}
