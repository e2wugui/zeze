package Zeze.Transaction;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.GTable.BeanMap1;
import Zeze.Transaction.GTable.BeanMap2;
import demo.ModuleGTable.BValue2;
import demo.ModuleGTable.Bean1;
import demo.ModuleGTable.Bean1ReadOnly;
import demo.ModuleGTable.tGTable;
import demo.ModuleGTable.tGTable2;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

/** Exercise row and cell structure changes through actual savepoints and generated root replay. */
@Fast
public class TestGTableTransactionReplay {
    private static Application newApp() throws Exception {
        int id = FastServerIds.takeoverPoolNext();
        var config = new Config();
        config.setServerId(id);
        config.setServiceManager("disable");
        config.setDefaultTableConf(new Config.TableConf());
        var database = new Config.DatabaseConf();
        database.setDatabaseUrl("GTableTransactionReplay_" + id);
        config.getDatabaseConfMap().put("", database);
        var app = new Application("GTableTransactionReplay" + id, config);
        app.setSchemas(new demo.Schemas());
        app.addTable("", new tGTable());
        app.addTable("", new tGTable2());
        app.start();
        Helper.registerAllTableLogs(app);
        return app;
    }

    private static Changes.Record decode(Table table, Changes.Record source) {
        var encoded = ByteBuffer.Allocate();
        source.encode(encoded);
        var decoded = new Changes.Record(table);
        decoded.decode(encoded);
        assertTrue(encoded.isEmpty());
        return decoded;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void newRowsRemovedRowsAndExistingRowCellsReplayForBothGTableFamilies() throws Exception {
        var app = newApp();
        var scalarTable = (tGTable)requireNonNull(app.getTable("demo_ModuleGTable_tGTable"));
        var beanTable = (tGTable2)requireNonNull(app.getTable("demo_ModuleGTable_tGTable2"));
        var scalarFollower = new AtomicReference<demo.ModuleGTable.BValue>();
        var beanFollower = new AtomicReference<BValue2>();
        var completed = new AtomicInteger();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var scalar = scalarTable.getOrAdd(1L);
                scalar.getGTable().put(1, 11, 5);
                scalar.getGTable().put(1, 12, 6);
                scalar.getGTable().put(2, 21, 20);
                var beans = beanTable.getOrAdd(1L);
                beans.getGTable().put(1, 11, new Bean1(5));
                beans.getGTable().put(1, 12, new Bean1(6));
                beans.getGTable().put(2, 21, new Bean1(20));
                scalarFollower.set(scalar.copy());
                beanFollower.set(beans.copy());
                return Procedure.Success;
            }, "seed-both-gtables").call());

            scalarTable.getChangeListenerMap().addListener((key, record) -> {
                var outer = (LogMap2<Integer, BeanMap1<Integer, Integer>>)requireNonNull(record.getVariableLog(tGTable.VAR_gTable));
                assertEquals(Set.of(3), outer.getReplaced().keySet());
                assertEquals(Set.of(2), outer.getRemoved());
                assertEquals(Set.of(1), outer.getChangedWithKey().keySet());
                var rowChange = requireNonNull(outer.getChangedWithKey().get(1));
                var inner = (LogMap1<Integer, Integer>)requireNonNull(requireNonNull(rowChange.getVariables()).get(1));
                assertEquals(Set.of(11, 13), inner.getReplaced().keySet());
                assertEquals(Set.of(12), inner.getRemoved());
                var target = requireNonNull(scalarFollower.get());
                var oldRow = target.getGTable().getPMap2().get(1);
                target.followerApply(requireNonNull(decode(scalarTable, record).getLogBean()));
                assertSame(oldRow, target.getGTable().getPMap2().get(1));
                assertEquals(Set.of(1, 3), target.getGTable().rowKeySet());
                assertEquals(Set.of(11, 13), target.getGTable().row(1).keySet());
                assertEquals(7, target.getGTable().get(1, 11));
                assertEquals(8, target.getGTable().get(1, 13));
                assertEquals(30, target.getGTable().get(3, 31));
                completed.incrementAndGet();
            });
            beanTable.getChangeListenerMap().addListener((key, record) -> {
                var outer = (LogMap2<Integer, BeanMap2<Integer, Bean1, Bean1ReadOnly>>)requireNonNull(record.getVariableLog(tGTable2.VAR_gTable));
                assertEquals(Set.of(3), outer.getReplaced().keySet());
                assertEquals(Set.of(2), outer.getRemoved());
                assertEquals(Set.of(1), outer.getChangedWithKey().keySet());
                var rowChange = requireNonNull(outer.getChangedWithKey().get(1));
                var inner = (LogMap2<Integer, Bean1>)requireNonNull(requireNonNull(rowChange.getVariables()).get(1));
                assertEquals(Set.of(13), inner.getReplaced().keySet());
                assertEquals(Set.of(12), inner.getRemoved());
                assertEquals(Set.of(11), inner.getChangedWithKey().keySet());
                var target = requireNonNull(beanFollower.get());
                var oldRow = target.getGTable().getPMap2().get(1);
                var oldCell = requireNonNull(target.getGTable().get(1, 11));
                target.followerApply(requireNonNull(decode(beanTable, record).getLogBean()));
                assertSame(oldRow, target.getGTable().getPMap2().get(1));
                assertSame(oldCell, target.getGTable().get(1, 11));
                assertEquals(Set.of(1, 3), target.getGTable().rowKeySet());
                assertEquals(Set.of(11, 13), target.getGTable().row(1).keySet());
                assertEquals(7, oldCell.getIntVar());
                assertEquals(8, requireNonNull(target.getGTable().get(1, 13)).getIntVar());
                assertEquals(30, requireNonNull(target.getGTable().get(3, 31)).getIntVar());
                completed.incrementAndGet();
            });

            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var scalar = scalarTable.getOrAdd(1L).getGTable();
                scalar.put(1, 11, 7);
                scalar.remove(1, 12);
                scalar.put(1, 13, 8);
                scalar.put(3, 31, 30);
                scalar.remove(2, 21); // removing the final cell must remove the outer row
                var beans = beanTable.getOrAdd(1L).getGTable();
                requireNonNull(beans.get(1, 11)).setIntVar(7);
                beans.remove(1, 12);
                beans.put(1, 13, new Bean1(8));
                beans.put(3, 31, new Bean1(30));
                beans.remove(2, 21);
                assertEquals(1, app.newProcedure(() -> {
                    scalar.put(1, 14, 40);
                    scalar.put(4, 41, 40);
                    beans.put(1, 14, new Bean1(40));
                    beans.put(4, 41, new Bean1(40));
                    return 1;
                }, "nested-row-and-cell-rollback").call());
                assertFalse(scalar.contains(1, 14));
                assertFalse(scalar.containsRow(4));
                assertFalse(beans.contains(1, 14));
                assertFalse(beans.containsRow(4));
                return Procedure.Success;
            }, "mixed-row-and-cell-structure").call());
            assertEquals(2, completed.get(), "both listener assertions and generated root replays must finish");
        } finally {
            app.stop();
        }
    }

    @Test
    public void managedGetTracksReplacementAndNestedRowRecreation() throws Exception {
        var app = newApp();
        var scalarTable = (tGTable)requireNonNull(app.getTable("demo_ModuleGTable_tGTable"));
        var beanTable = (tGTable2)requireNonNull(app.getTable("demo_ModuleGTable_tGTable2"));
        var committedScalarRow = new AtomicReference<BeanMap1<Integer, Integer>>();
        var committedBeanRow = new AtomicReference<BeanMap2<Integer, Bean1, Bean1ReadOnly>>();
        var committedCell = new AtomicReference<Bean1>();
        try {
            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var scalar = scalarTable.getOrAdd(1L).getGTable();
                var beans = beanTable.getOrAdd(1L).getGTable();
                assertTrue(scalar.getPMap2().isManaged());
                assertTrue(beans.getPMap2().isManaged());
                scalar.put(1, 11, 5);
                var firstCell = new Bean1(5);
                beans.put(1, 11, firstCell);
                assertEquals(5, scalar.get(1, 11), "get must see a managed put before commit");
                assertSame(firstCell, beans.get(1, 11));
                return Procedure.Success;
            }, "direct-get-seed").call());

            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var scalar = scalarTable.getOrAdd(1L).getGTable();
                var beans = beanTable.getOrAdd(1L).getGTable();
                var originalScalarRow = requireNonNull(scalar.getPMap2().get(1));
                var originalBeanRow = requireNonNull(beans.getPMap2().get(1));
                var oldCell = requireNonNull(beans.get(1, 11));
                assertEquals(5, scalar.put(1, 11, 7));
                var replacement = new Bean1(7);
                assertSame(oldCell, beans.put(1, 11, replacement));
                assertEquals(7, scalar.get(1, 11), "get must see an uncommitted replacement");
                assertSame(replacement, beans.get(1, 11));

                assertEquals(1, app.newProcedure(() -> {
                    assertSame(originalScalarRow, scalar.getPMap2().remove(1));
                    assertSame(originalBeanRow, beans.getPMap2().remove(1));
                    assertNull(scalar.get(1, 11));
                    assertNull(beans.get(1, 11));
                    scalar.put(1, 12, 70);
                    var rolledBackCell = new Bean1(70);
                    beans.put(1, 12, rolledBackCell);
                    assertNotSame(originalScalarRow, scalar.getPMap2().get(1));
                    assertNotSame(originalBeanRow, beans.getPMap2().get(1));
                    assertEquals(70, scalar.get(1, 12));
                    assertSame(rolledBackCell, beans.get(1, 12));
                    return 1;
                }, "direct-get-recreate-rollback").call());
                assertSame(originalScalarRow, scalar.getPMap2().get(1));
                assertSame(originalBeanRow, beans.getPMap2().get(1));
                assertEquals(7, scalar.get(1, 11), "rollback must restore the outer savepoint's value");
                assertSame(replacement, beans.get(1, 11));
                assertNull(scalar.get(1, 12));
                assertNull(beans.get(1, 12));

                assertEquals(Procedure.Success, app.newProcedure(() -> {
                    assertEquals(7, scalar.remove(1, 11));
                    assertSame(replacement, beans.remove(1, 11));
                    assertFalse(scalar.containsRow(1));
                    assertFalse(beans.containsRow(1));
                    assertNull(scalar.get(1, 11));
                    assertNull(beans.get(1, 11));
                    scalar.put(1, 12, 9);
                    var finalCell = new Bean1(9);
                    beans.put(1, 12, finalCell);
                    assertEquals(9, scalar.get(1, 12));
                    assertSame(finalCell, beans.get(1, 12));
                    committedScalarRow.set(requireNonNull(scalar.getPMap2().get(1)));
                    committedBeanRow.set(requireNonNull(beans.getPMap2().get(1)));
                    committedCell.set(finalCell);
                    return Procedure.Success;
                }, "direct-get-recreate-success").call());
                assertNotSame(originalScalarRow, scalar.getPMap2().get(1));
                assertNotSame(originalBeanRow, beans.getPMap2().get(1));
                assertEquals(9, scalar.get(1, 12));
                assertSame(committedCell.get(), beans.get(1, 12));
                assertNull(scalar.get(1, 11));
                assertNull(beans.get(1, 11));
                return Procedure.Success;
            }, "direct-get-nested-savepoints").call());

            assertEquals(Procedure.Success, app.newProcedure(() -> {
                var scalar = requireNonNull(scalarTable.get(1L)).getGTable();
                var beans = requireNonNull(beanTable.get(1L)).getGTable();
                assertSame(committedScalarRow.get(), scalar.getPMap2().get(1));
                assertSame(committedBeanRow.get(), beans.getPMap2().get(1));
                assertSame(committedCell.get(), beans.get(1, 12));
                assertEquals(9, scalar.get(1, 12));
                assertEquals(9, requireNonNull(beans.get(1, 12)).getIntVar());
                assertNull(scalar.get(1, 11));
                assertNull(beans.get(1, 11));
                return Procedure.Success;
            }, "direct-get-verify-committed").call());
        } finally {
            app.stop();
        }
    }
}
