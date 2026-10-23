package Zeze.Transaction.Collections;

import java.util.function.Consumer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Locks;
import Zeze.Transaction.Map2TestSupport;
import Zeze.Transaction.Transaction;

/** Source write-intent fixture with a real final savepoint and the production change-export hook. */
public final class Map2DeltaTestSupport {
    private Map2DeltaTestSupport() {
    }

    public static <K, V extends Bean> LogMap2<K, V> map(
            PMap2<K, V> map, Consumer<LogMap1<K, V>> edit) {
        Map2TestSupport.attach(map);
        var transaction = Transaction.create(new Locks());
        transaction.begin();
        try {
            var delta = new LogMap2Delta<K, V>(map.parent(), map.variableId(), map, map.getMap(), map.getMeta());
            transaction.putLog(delta);
            edit.accept(delta);
            delta.commit();
            return delta.createChangeLog();
        } finally {
            Transaction.destroy();
        }
    }

    public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> sorted(
            PSortedMap2<K, V> map, Consumer<LogSortedMap1<K, V>> edit) {
        Map2TestSupport.attach(map);
        var transaction = Transaction.create(new Locks());
        transaction.begin();
        try {
            var delta = new LogSortedMap2Delta<K, V>(map.parent(), map.variableId(), map, map.getMap(), map.getMeta());
            transaction.putLog(delta);
            edit.accept(delta);
            delta.commit();
            return delta.createChangeLog();
        } finally {
            Transaction.destroy();
        }
    }
}
