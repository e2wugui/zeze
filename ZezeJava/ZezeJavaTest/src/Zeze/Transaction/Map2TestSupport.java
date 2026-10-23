package Zeze.Transaction;

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
	@SuppressWarnings("unchecked")
	public static <L extends Log> L decode(L template, Zeze.Serialize.IByteBuffer input) {
		Zeze.History.Helper.registerLogs();
		if (template instanceof LogSortedMap2<?, ?> || template instanceof Zeze.Transaction.Collections.LogSortedMap1<?, ?>)
			return (L)Zeze.Transaction.Collections.Map2DeltaTestSupport.decodeSorted((LogBean)template, input);
		return (L)LogRegistry.decode(template.getTypeId(), template.getVariableId(), input);
	}

	public static Zeze.Transaction.Logs.LogIntValue intLog(int id, int value) {
		var bb = ByteBuffer.Allocate();
		bb.WriteInt(value);
		return Zeze.Transaction.Logs.LogIntValue.decode(id, bb);
	}

	public static Zeze.Transaction.Logs.LogLongValue longLog(int id, long value) {
		var bb = ByteBuffer.Allocate();
		bb.WriteLong(value);
		return Zeze.Transaction.Logs.LogLongValue.decode(id, bb);
	}

	private Map2TestSupport() {
	}

	public static Changes changes() {
		var current = Transaction.getCurrent();
		return new Changes(true, null, current != null ? current.lastSavepoint() : new Savepoint());
	}

	/** Unbound wire node with the exact metadata used by this map, including GTable factories. */
	public static <K, V extends Bean> LogMap2<K, V> wireLog(PMap2<K, V> map) {
		Zeze.History.Helper.registerLogMap2Meta(map.getMeta());
		return new LogMap2<>(map.variableId(), null, map.getMeta(), null, null);
	}

	/** Keep the configured comparator; the wire format does not carry it. */
	public static <K extends Comparable<K>, V extends Bean> LogSortedMap2<K, V> wireLog(PSortedMap2<K, V> map) {
		Zeze.History.Helper.registerLogSortedMap2Meta(map.getMeta());
		return new LogSortedMap2<>(map.variableId(), null, map.comparator(), map.getMeta(), null, null);
	}

	/** Exercise an empty decode on an already unbound wire fixture. */
	public static <L extends LogBean> L emptyWirePayload(L log) {
		var empty = ByteBuffer.Allocate();
		empty.WriteUInt(0);
		empty.WriteUInt(0);
		empty.WriteUInt(0);
		return decode(log, empty);
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
		//noinspection DataFlowIssue
		app.addTable(config.getTableConf("demo_Module1_tflush").getDatabaseName(), new tflush());
		app.start();
		return app;
	}

	public static void attach(Bean map) {
		if (!map.isManaged()) {
			var table = new tflush();
			var root = new EmptyBean();
			var record = new Record(root) {
				@Override
				public Table getTable() {
					return table;
				}

				@Override
				public Object getObjectKey() {
					return 1L;
				}

				@Override
				public void setDirty() {
				}

				@Override
				public IGlobalAgent.AcquireResult acquire(int state, boolean fresh, boolean noWait) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void commit(RecordAccessed accessed) {
				}
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
		var payload = map.createLogBean(null);
		payload.addChild(map, childChange);
		return payload;
	}
}
