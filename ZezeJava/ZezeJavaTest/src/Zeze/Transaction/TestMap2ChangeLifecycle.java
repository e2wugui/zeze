package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import org.junit.jupiter.api.Test;

@Fast
public class TestMap2ChangeLifecycle {
    private static BSimple bean(int value) {
        var result = new BSimple();
        result.setInt_1(value);
        return result;
    }

    private static ByteBuffer encode(Log log) {
        var out = ByteBuffer.Allocate();
        log.encode(out);
        return ByteBuffer.Wrap(out.Copy());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nestedWritesFieldOnlyAndMixedChangesReplay() throws Exception {
        var app = Map2TestSupport.newApp("Map2ChangeLifecycle");
        var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
        var mapFollower = new PMap2<Long, BSimple>(Long.class, BSimple.class);
        var sortedFollower = new PSortedMap2<Integer, BSimple>(Integer.class, BSimple.class);
        mapFollower.put(1L, bean(5));
        sortedFollower.put(1, bean(5));
        var phase = new AtomicInteger();
        var observed = new AtomicInteger();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                root.getMap41().put(1L, bean(5));
                root.getSortedmap2().put(1, bean(5));
                return Procedure.Success;
            }, "seed").call());
            table.getChangeListenerMap().addListener((key, record) -> {
                var map = (LogMap2<Long, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_map41));
                var sorted = (LogSortedMap2<Integer, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_sortedmap2));
                if (phase.get() == 1) {
                    assertEquals(Set.of(2L, 3L), map.getReplaced().keySet());
                    assertEquals(Set.of(2, 3), sorted.getReplaced().keySet());
                    assertTrue(map.getChangedWithKey().isEmpty());
                    assertTrue(sorted.getChangedWithKey().isEmpty());
                } else if (phase.get() == 2) {
                    assertTrue(map.getReplaced().isEmpty());
                    assertTrue(sorted.getReplaced().isEmpty());
                    assertEquals(Set.of(1L), map.getChangedWithKey().keySet());
                    assertEquals(Set.of(1), sorted.getChangedWithKey().keySet());
                } else {
                    assertEquals(Set.of(1L, 5L), map.getReplaced().keySet());
                    assertEquals(Set.of(1, 5), sorted.getReplaced().keySet());
                    assertEquals(Set.of(2L), map.getRemoved());
                    assertEquals(Set.of(2), sorted.getRemoved());
                    assertTrue(map.getChangedWithKey().isEmpty(), "replaced/removed and stale values must not also emit field deltas");
                    assertTrue(sorted.getChangedWithKey().isEmpty());
                }
                var mapReplay = Map2TestSupport.wireLog(mapFollower);
                mapReplay.decode(encode(map));
                mapFollower.followerApply(mapReplay);
                var sortedReplay = Map2TestSupport.wireLog(sortedFollower);
                sortedReplay.decode(encode(sorted));
                sortedFollower.followerApply(sortedReplay);
                var root = (BValue)requireNonNull(record.getValue());
                assertEquals(root.getMap41().keySet(), mapFollower.keySet());
                assertEquals(root.getSortedmap2().keySet(), sortedFollower.keySet());
                root.getMap41().forEach((k, v) -> assertEquals(v.getInt_1(), requireNonNull(mapFollower.get(k)).getInt_1()));
                root.getSortedmap2().forEach((k, v) -> assertEquals(v.getInt_1(), requireNonNull(sortedFollower.get(k)).getInt_1()));
                observed.incrementAndGet();
            });

            phase.set(1);
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                root.getMap41().put(2L, bean(20));
                root.getSortedmap2().put(2, bean(20));
                assertFalse(Transaction.getCurrent().getLog(root.objectId() + tflush.VAR_map41) instanceof LogMap2<?, ?>);
                assertFalse(Transaction.getCurrent().getLog(root.objectId() + tflush.VAR_sortedmap2) instanceof LogSortedMap2<?, ?>);
                assertEquals(Procedure.Success, app.newProcedure(() -> {
                    root.getMap41().put(3L, bean(30));
                    root.getSortedmap2().put(3, bean(30));
                    return Procedure.Success;
                }, "nested-success").call());
                assertEquals(1, app.newProcedure(() -> {
                    root.getMap41().put(4L, bean(40));
                    root.getSortedmap2().put(4, bean(40));
                    return 1;
                }, "nested-rollback").call());
                assertFalse(root.getMap41().containsKey(4L));
                assertFalse(root.getSortedmap2().containsKey(4));
                return Procedure.Success;
            }, "structure-only").call());

            phase.set(2);
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                requireNonNull(root.getMap41().get(1L)).setInt_1(6);
                requireNonNull(root.getSortedmap2().get(1)).setInt_1(6);
                assertNull(Transaction.getCurrent().getLog(root.objectId() + tflush.VAR_map41));
                assertNull(Transaction.getCurrent().getLog(root.objectId() + tflush.VAR_sortedmap2));
                assertEquals(Procedure.Success, app.newProcedure(() -> {
                    requireNonNull(root.getMap41().get(1L)).setInt_1(7);
                    requireNonNull(root.getSortedmap2().get(1)).setInt_1(7);
                    return Procedure.Success;
                }, "nested-field-success").call());
                assertEquals(1, app.newProcedure(() -> {
                    requireNonNull(root.getMap41().get(1L)).setInt_1(8);
                    requireNonNull(root.getSortedmap2().get(1)).setInt_1(8);
                    return 1;
                }, "nested-field-rollback").call());
                assertEquals(7, requireNonNull(root.getMap41().get(1L)).getInt_1());
                assertEquals(7, requireNonNull(root.getSortedmap2().get(1)).getInt_1());
                return Procedure.Success;
            }, "field-only").call());

            phase.set(3);
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                var old = requireNonNull(root.getMap41().get(1L));
                var sortedOld = requireNonNull(root.getSortedmap2().get(1));
                old.setInt_1(9);
                sortedOld.setInt_1(9);
                assertEquals(Procedure.Success, app.newProcedure(() -> {
                    root.getMap41().putAll(Map.of(1L, bean(9)));
                    root.getSortedmap2().putAll(Map.of(1, bean(9)));
                    return Procedure.Success;
                }, "nested-equal-content-new-instance").call());
                old.setInt_1(99);
                sortedOld.setInt_1(99);
                var removed = requireNonNull(root.getMap41().remove(2L));
                var sortedRemoved = requireNonNull(root.getSortedmap2().remove(2));
                removed.setInt_1(88);
                sortedRemoved.setInt_1(88);
                root.getMap41().put(5L, bean(5));
                root.getSortedmap2().put(5, bean(5));
                requireNonNull(root.getMap41().get(5L)).setInt_1(6);
                requireNonNull(root.getSortedmap2().get(5)).setInt_1(6);
                return Procedure.Success;
            }, "mixed-final-identities").call());
            assertEquals(3, observed.get(), "all callback assertions must run to completion");
        } finally {
            app.stop();
        }
    }

    @Test
    public void collectionOrderKeepsOneCanonicalPayload() throws Exception {
        var app = Map2TestSupport.newApp("Map2CollectOrder");
        var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
        var observed = new AtomicInteger();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                root.getMap41().put(1L, bean(5));
                root.getSortedmap2().put(1, bean(5));
                return Procedure.Success;
            }, "seed").call());
            table.getChangeListenerMap().addListener((key, record) -> {
                var root = (BValue)requireNonNull(record.getValue());
                verifyOrders(root, root.getMap41(), requireNonNull(root.getMap41().get(1L)), tflush.VAR_map41);
                verifyOrders(root, root.getSortedmap2(), requireNonNull(root.getSortedmap2().get(1)), tflush.VAR_sortedmap2);
                observed.incrementAndGet();
            });
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                requireNonNull(root.getMap41().get(1L)).setInt_1(7);
                requireNonNull(root.getSortedmap2().get(1)).setInt_1(7);
                root.getMap41().put(2L, bean(20));
                root.getSortedmap2().put(2, bean(20));
                return Procedure.Success;
            }, "mixed").call());
            assertEquals(1, observed.get());
        } finally {
            app.stop();
        }
    }

    private static void verifyOrders(Bean root, Bean map, Bean item, int variableId) {
        var transaction = requireNonNull(Transaction.getCurrent());
        var delta = requireNonNull(transaction.getLog(root.objectId() + variableId));
        var field = requireNonNull(transaction.getLog(item.objectId() + 1)); // BSimple.int_1
        byte[] prior = null;
        for (boolean structureFirst : new boolean[] {false, true}) {
            var changes = Map2TestSupport.changes();
            if (structureFirst)
                changes.collect(root, delta);
            changes.collect(item, field);
            var canonical = changes.getOrCreateLogBean(map);
            changes.collect(root, delta);
            assertSame(canonical, changes.getOrCreateLogBean(map));
            var rootLog = changes.getOrCreateLogBean(root);
            assertSame(canonical, requireNonNull(rootLog.getVariables()).get(variableId));
            var encoded = encode(canonical).Copy();
            if (prior != null)
                assertArrayEquals(prior, encoded, "structure/field visitation order must not lose either half");
            prior = encoded;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void staleOnlyTransactionKeepsTheSourceRecordAndEmitsNoValueEdits() throws Exception {
        var app = Map2TestSupport.newApp("Map2StaleOnly");
        var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
        var old = new AtomicReference<BSimple>();
        var sortedOld = new AtomicReference<BSimple>();
        var observed = new AtomicInteger();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                old.set(bean(5));
                sortedOld.set(bean(5));
                root.getMap41().put(1L, old.get());
                root.getSortedmap2().put(1, sortedOld.get());
                return Procedure.Success;
            }, "seed").call());
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                root.getMap41().put(1L, bean(9));
                root.getSortedmap2().put(1, bean(9));
                return Procedure.Success;
            }, "replace").call());
            table.getChangeListenerMap().addListener((key, record) -> {
                assertEquals(Changes.Record.Edit, record.getState());
                var root = (BValue)requireNonNull(record.getValue(), "even filtered stale edits must retain the source record");
                var map = (LogMap2<Long, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_map41));
                var sorted = (LogSortedMap2<Integer, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_sortedmap2));
                assertTrue(map.getReplaced().isEmpty());
                assertTrue(map.getRemoved().isEmpty());
                assertTrue(map.getChangedWithKey().isEmpty());
                assertTrue(sorted.getReplaced().isEmpty());
                assertTrue(sorted.getRemoved().isEmpty());
                assertTrue(sorted.getChangedWithKey().isEmpty());
                assertEquals(9, requireNonNull(root.getMap41().get(1L)).getInt_1());
                assertEquals(9, requireNonNull(root.getSortedmap2().get(1)).getInt_1());
                var projected = new AtomicInteger();
                record.<Long, BSimple>map2ListenerView(tflush.VAR_map41).forEachUpsert((k, v) -> projected.incrementAndGet());
                record.<Integer, BSimple>sortedMap2ListenerView(tflush.VAR_sortedmap2).forEachUpsert((k, v) -> projected.incrementAndGet());
                assertEquals(0, projected.get());
                var follower = new PMap2<Long, BSimple>(Long.class, BSimple.class);
                var sortedFollower = new PSortedMap2<Integer, BSimple>(Integer.class, BSimple.class);
                var current = bean(9);
                var sortedCurrent = bean(9);
                follower.put(1L, current);
                sortedFollower.put(1, sortedCurrent);
                var replay = Map2TestSupport.wireLog(follower);
                var sortedReplay = Map2TestSupport.wireLog(sortedFollower);
                replay.decode(encode(map));
                sortedReplay.decode(encode(sorted));
                follower.followerApply(replay);
                sortedFollower.followerApply(sortedReplay);
                assertSame(current, follower.get(1L));
                assertSame(sortedCurrent, sortedFollower.get(1));
                assertEquals(9, current.getInt_1());
                assertEquals(9, sortedCurrent.getInt_1());
                observed.incrementAndGet();
            });
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                table.getOrAdd(1L); // obtain control of the record to which the retained references belong
                requireNonNull(old.get()).setInt_1(99);
                requireNonNull(sortedOld.get()).setInt_1(99);
                return Procedure.Success;
            }, "stale-only").call());
            assertEquals(1, observed.get(), "filtered source logs still form one valid Edit callback");
        } finally {
            app.stop();
        }
    }
}
