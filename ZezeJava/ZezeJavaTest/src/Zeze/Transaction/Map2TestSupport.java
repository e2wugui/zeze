package Zeze.Transaction;

import static java.util.Objects.requireNonNull;

import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.PMap2;
import Zeze.Transaction.Collections.PSortedMap2;
import demo.Module1.tflush;
import harness.FastServerIds;

/** Small source-collection fixture; wire-only tests may populate the canonical keyed payload directly. */
public final class Map2TestSupport {
    private Map2TestSupport() {
    }

    public static Changes changes() {
        return new Changes(true, null);
    }

    /** Unbound wire node with the exact metadata used by this map, including GTable factories. */
    public static <K, V extends Bean> LogMap2<K, V> wireLog(PMap2<K, V> map) {
        return new LogMap2<>(null, map.variableId(), null, map.getMap(), map.getMeta());
    }

    /** Keep the configured comparator; the wire format does not carry it. */
    public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> wireLog(PSortedMap2<K, V> map) {
        return new LogSortedMap2<>(null, map.variableId(), null, map.getMap(), map.getMeta());
    }

    /** Exercise an empty decode on an already unbound wire fixture. */
    public static <L extends LogBean> L emptyWirePayload(L log) {
        var empty = ByteBuffer.Allocate();
        empty.WriteUInt(0);
        empty.WriteUInt(0);
        empty.WriteUInt(0);
        log.decode(empty);
        return log;
    }

    static Application newApp(String name) throws Exception {
        var id = FastServerIds.takeoverPoolNext();
        var config = new Config();
        config.setServiceManager("disable");
        config.setServerId(id);
        config.setDefaultTableConf(new Config.TableConf());
        var database = new Config.DatabaseConf();
        database.setDatabaseUrl(name + '_' + id);
        config.getDatabaseConfMap().put("", database);
        var app = new Application(name + id, config);
        app.setSchemas(new demo.Schemas());
        app.addTable(requireNonNull(config.getTableConf("demo_Module1_tflush")).getDatabaseName(), new tflush());
        app.start();
        return app;
    }

    public static void attach(Bean map) {
        if (!map.isManaged()) {
            var table = new tflush();
            var root = new EmptyBean();
            var record = new Record(root) {
                @Override public Table getTable() { return table; }
                @Override public Object getObjectKey() { return 1L; }
                @Override public void setDirty() { }
                @Override public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
                    throw new UnsupportedOperationException();
                }
                @Override public void commit(RecordAccessed accessed) { }
            };
            var info = new Record.RootInfo(record, new TableKey(table.getId(), 1L));
            root.initRootInfo(info, null);
            map.variableId(1);
            map.initRootInfo(info, root);
        }
    }

    static LogBean collect(Bean map, LogBean childChange) {
        attach(map);
        var changes = changes();
        var payload = map.createLogBean();
        payload.collect(changes, map, childChange);
        return payload;
    }
}
