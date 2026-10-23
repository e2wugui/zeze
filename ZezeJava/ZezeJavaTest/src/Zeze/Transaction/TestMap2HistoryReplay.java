package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Builtin.HistoryModule.BLogChanges;
import Zeze.History.Helper;
import Zeze.History.History;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Util.Id128;
import demo.Bean1;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.tflush;
import harness.Fast;
import org.junit.jupiter.api.Test;

/** History must fix the wire payload before listeners project full values from that same commit. */
@Fast
public class TestMap2HistoryReplay {
    private static BSimple bean(int value) {
        var result = new BSimple();
        result.setInt_1(value);
        return result;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void actualHistoryCaptureThenListenerProjectionReplaysMixedAndNestedChanges() throws Exception {
        var app = Map2TestSupport.newApp("Map2HistoryReplay");
        var table = (tflush)requireNonNull(app.getTable("demo_Module1_tflush"));
        var follower = new AtomicReference<BValue>();
        var completedListeners = new AtomicInteger();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                root.getMap41().put(1L, bean(5));
                root.getMap41().put(2L, bean(20));
                root.getSortedmap2().put(1, bean(5));
                root.getSortedmap2().put(2, bean(20));
                var nested = new BValue();
                nested.getList9().add(new Bean1(5));
                root.getMap42Recursive().put(42L, nested);
                follower.set(root.copy());
                return Procedure.Success;
            }, "seed").call());
            Helper.registerTableLogs(table);
            var history = new CaptureHistory(table);
            var historyField = Application.class.getDeclaredField("history");
            historyField.setAccessible(true);
            historyField.set(app, history); // Test-only assembly; the normal transaction still prepares and captures.

            table.getChangeListenerMap().addListener((key, record) -> {
                assertEquals(1, history.prepares);
                assertEquals(1, history.captures, "capture must have completed before listeners run");
                assertEquals(1, requireNonNull(history.data).getChanges().size());
                Binary saved = requireNonNull(history.bytes);
                byte[] capturedBytes = ByteBuffer.Wrap(saved).Copy();
                var map = (LogMap2<Long, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_map41));
                var sorted = (LogSortedMap2<Integer, BSimple>)requireNonNull(record.getVariableLog(tflush.VAR_sortedmap2));
                assertEquals(Set.of(1L), map.getChangedWithKey().keySet());
                assertEquals(Set.of(3L), map.getReplaced().keySet());
                assertEquals(Set.of(2L), map.getRemoved());
                assertEquals(Set.of(1), sorted.getChangedWithKey().keySet());
                assertEquals(Set.of(3), sorted.getReplaced().keySet());
                assertEquals(Set.of(2), sorted.getRemoved());
                var upserts = new HashMap<Long, Integer>();
                record.<Long, BSimple>map2ListenerView(tflush.VAR_map41)
                        .forEachUpsert((k, v) -> upserts.put(k, v.getInt_1()));
                assertEquals(Map.of(1L, 7, 3L, 31), upserts);
                var sortedUpserts = new HashMap<Integer, Integer>();
                record.<Integer, BSimple>sortedMap2ListenerView(tflush.VAR_sortedmap2)
                        .forEachUpsert((k, v) -> sortedUpserts.put(k, v.getInt_1()));
                assertEquals(Map.of(1, 7, 3, 31), sortedUpserts);

                // The unconverted List2 family is nested beneath a converted Map2 consumption node.
                var recursive = (LogMap2<Long, BValue>)requireNonNull(record.getVariableLog(tflush.VAR_map42Recursive));
                assertTrue(recursive.getReplaced().isEmpty());
                LogBean nestedLog = requireNonNull(recursive.getChangedWithKey().get(42L));
                assertTrue(requireNonNull(requireNonNull(nestedLog.getVariables()).get(9)) instanceof LogList2<?>);
                var reencodedRecord = ByteBuffer.Allocate();
                record.encode(reencodedRecord);
                assertArrayEquals(capturedBytes, reencodedRecord.Copy(), "listener views must not change the captured wire tree");
                assertArrayEquals(capturedBytes, ByteBuffer.Wrap(saved).Copy(), "History owns its captured Binary");

                var decoded = new Changes.Record(table);
                var input = ByteBuffer.Wrap(saved);
                decoded.decode(input);
                assertTrue(input.isEmpty());
                assertEquals(Changes.Record.Edit, decoded.getState());
                var target = requireNonNull(follower.get());
                var oldMapItem = requireNonNull(target.getMap41().get(1L));
                var oldSortedItem = requireNonNull(target.getSortedmap2().get(1));
                var oldNested = requireNonNull(target.getMap42Recursive().get(42L));
                var oldListItem = oldNested.getList9().get(0);
                target.followerApply(requireNonNull(decoded.getLogBean()));
                assertSame(oldMapItem, target.getMap41().get(1L));
                assertSame(oldSortedItem, target.getSortedmap2().get(1));
                assertEquals(Set.of(1L, 3L), target.getMap41().keySet());
                assertEquals(Set.of(1, 3), target.getSortedmap2().keySet());
                assertEquals(7, oldMapItem.getInt_1());
                assertEquals(7, oldSortedItem.getInt_1());
                assertEquals(31, requireNonNull(target.getMap41().get(3L)).getInt_1());
                assertEquals(31, requireNonNull(target.getSortedmap2().get(3)).getInt_1());
                assertSame(oldNested, target.getMap42Recursive().get(42L));
                assertSame(oldListItem, oldNested.getList9().get(0));
                assertEquals(7, oldListItem.getV1());
                assertEquals(2, oldNested.getList9().size());
                assertEquals(31, oldNested.getList9().get(1).getV1());
                completedListeners.incrementAndGet();
            });
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var root = table.getOrAdd(1L);
                requireNonNull(root.getMap41().get(1L)).setInt_1(7);
                root.getMap41().remove(2L);
                root.getMap41().put(3L, bean(30));
                requireNonNull(root.getMap41().get(3L)).setInt_1(31);
                requireNonNull(root.getSortedmap2().get(1)).setInt_1(7);
                root.getSortedmap2().remove(2);
                root.getSortedmap2().put(3, bean(30));
                requireNonNull(root.getSortedmap2().get(3)).setInt_1(31);
                var list = requireNonNull(root.getMap42Recursive().get(42L)).getList9();
                list.get(0).setV1(7);
                list.add(new Bean1(30));
                list.get(1).setV1(31);
                return Procedure.Success;
            }, "history-mixed-and-map2-list2").call());
            assertEquals(1, completedListeners.get(), "callback isolation must not hide a failed assertion");
            assertEquals(1, history.prepares);
            assertEquals(1, history.captures);
        } finally {
            app.stop();
        }
    }

    /** Exercises the production capture hook and encoder without an external gid service or history store. */
    private static final class CaptureHistory implements IHistory {
        private int prepares;
        private int captures;
        private BLogChanges.Data data;
        private Binary bytes;
        private final PendingHistory completed;

        private CaptureHistory(Table table) {
            completed = new PendingHistory() {
                @Override public Table getTable() { return table; }
                @Override public void mergeFrom(PendingHistory from) {
                    if (from != this)
                        throw new IllegalArgumentException("foreign capture token");
                }
                @Override public PendingHistory snapshot() { return this; }
                @Override public void encode0() { }
                @Override public void writeOnly(Database.Table target, Database.Transaction transaction) { }
                @Override public void onPersisted() { }
            };
        }

        @Override public void registerLogs(Iterable<Table> tables) { }
        @Override public void afterOpen() { }
        @Override public void close() { }

        @Override
        public PreparedCommit prepareCommit(Procedure procedure) {
            var gid = new Id128(0, ++prepares);
            return changes -> {
                data = History.buildLogChanges(gid, changes, null, null);
                bytes = data.getChanges().isEmpty() ? null : data.getChanges().values().iterator().next();
                ++captures;
                return completed;
            };
        }
    }
}
