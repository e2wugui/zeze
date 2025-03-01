package Zeze.Util;

import Zeze.Net.Protocol;
import Zeze.Transaction.TableKey;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.snapshots.Unit;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrometheusCounter implements ZezeCounter {

	public static void startHttpServer() {
		startHttpServer(9400);
	}

	public static void startHttpServer(int port) {
		try {
			HTTPServer server = HTTPServer.builder().port(port).buildAndStart();
			System.out.println("HTTPServer listening on port http://localhost:" + server.getPort() + "/metrics");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static class ProtocolRecvMetric {
		public final CounterDataPoint bytes;
		public final DistributionDataPoint processDuration;

		public ProtocolRecvMetric(CounterDataPoint bytes, DistributionDataPoint processDuration) {
			this.bytes = bytes;
			this.processDuration = processDuration;
		}
	}

	private static class ProtocolSendMetric {
		public final CounterDataPoint total;
		public final CounterDataPoint bytes;

		public ProtocolSendMetric(CounterDataPoint total, CounterDataPoint bytes) {
			this.total = total;
			this.bytes = bytes;
		}
	}

	private final ConcurrentHashMap<Object, LongObserver> runTimeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, TableCounter> tableCounterMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolRecvMetric> protocolRecvMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolSendMetric> protocolSendMap = new ConcurrentHashMap<>();

	private final Counter procedure_started = Counter.builder().name("procedure_started")
			.labelNames("name").register();
	private final Counter procedure_completed = Counter.builder().name("procedure_completed")
			.labelNames("name", "result_code").register();
	private final Histogram procedure_duration_seconds = Histogram.builder().name("procedure_duration_seconds")
			.labelNames("name", "result_code").register();

	private final Counter database_table_operation = Counter.builder().name("database_table_operation")
			.labelNames("table", "operation").register();

	private final Counter protocol_recv_bytes = Counter.builder().name("protocol_recv_bytes")
			.labelNames("protocol").register();
	private final Histogram protocol_duration_seconds = Histogram.builder().name("protocol_duration_seconds")
			.labelNames("protocol").register();
	private final Counter protocol_send = Counter.builder().name("protocol_send")
			.labelNames("protocol").register();
	private final Counter protocol_send_bytes = Counter.builder().name("protocol_send_bytes")
			.labelNames("protocol").register();

	@Override
	public @NotNull LongCounter allocCounter(@NotNull String name) {
		Counter counter = Counter.builder().name(name).register();
		return counter::inc;
	}

	@Override
	public @NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames) {
		Counter counter = Counter.builder().name(name).labelNames(labelNames).register();
		return labels -> {
			CounterDataPoint dp = counter.labelValues(labels);
			return dp::inc;
		};
	}

	@Override
	public @NotNull LabeledObserverCreator allocLabeledObserverCreator(@NotNull String name, @NotNull String... labelNames) {
		Histogram histogram = Histogram.builder().name(name).labelNames(labelNames).register();
		return labels -> {
			DistributionDataPoint dp = histogram.labelValues(labels);
			return dp::observe;
		};
	}

	@Override
	public @NotNull LongObserver getRunTimeObserver(@NotNull Object key) {
		String name = key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key);
		Histogram histogram = Histogram.builder().nativeOnly().name(name).register();
		return (amount) -> histogram.observe(Unit.nanosToSeconds(amount));
	}

	@Override
	public void addRunTime(@NotNull Object key, long timeNs) {
		LongObserver observer = runTimeMap.computeIfAbsent(key, (k) -> {
			String name = k instanceof Class ? ((Class<?>)k).getName() : String.valueOf(k);
			Histogram histogram = Histogram.builder().name(name).register();
			return (amount) -> histogram.observe(Unit.nanosToSeconds(amount));
		});
		observer.observe(timeNs);
	}

	@Override
	public void procedureStart(@NotNull String name) {
		procedure_started.labelValues(name).inc();
	}

	@Override
	public void procedureEnd(@NotNull String name, long resultCode, long timeNs) {
		procedure_completed.labelValues(name, String.valueOf(resultCode)).inc();
		procedure_duration_seconds.labelValues(name, String.valueOf(resultCode)).observe(Unit.nanosToSeconds(timeNs));
	}

	@Override
	public @NotNull TableCounter getOrAddTableInfo(long tableId) {
		return tableCounterMap.computeIfAbsent(tableId, k -> {
			String tableName = TableKey.tables.get(tableId);
			String table = tableName != null ? tableName : String.valueOf(tableId);
			LongCounter readLock = database_table_operation.labelValues(table, "readLock")::inc;
			LongCounter writeLock = database_table_operation.labelValues(table, "writeLock")::inc;
			LongCounter storageGet = database_table_operation.labelValues(table, "storageGet")::inc;
			LongCounter tryReadLock = database_table_operation.labelValues(table, "tryReadLock")::inc;
			LongCounter tryWriteLock = database_table_operation.labelValues(table, "tryWriteLock")::inc;
			LongCounter acquireShare = database_table_operation.labelValues(table, "acquireShare")::inc;
			LongCounter acquireModify = database_table_operation.labelValues(table, "acquireModify")::inc;
			LongCounter acquireInvalid = database_table_operation.labelValues(table, "acquireInvalid")::inc;
			LongCounter reduceInvalid = database_table_operation.labelValues(table, "reduceInvalid")::inc;
			LongCounter redo = database_table_operation.labelValues(table, "redo")::inc;

			return new TableCounter() {
				@Override
				public @NotNull LongCounter readLock() {
					return readLock;
				}

				@Override
				public @NotNull LongCounter writeLock() {
					return writeLock;
				}

				@Override
				public @NotNull LongCounter storageGet() {
					return storageGet;
				}

				@Override
				public @NotNull LongCounter tryReadLock() {
					return tryReadLock;
				}

				@Override
				public @NotNull LongCounter tryWriteLock() {
					return tryWriteLock;
				}

				@Override
				public @NotNull LongCounter acquireShare() {
					return acquireShare;
				}

				@Override
				public @NotNull LongCounter acquireModify() {
					return acquireModify;
				}

				@Override
				public @NotNull LongCounter acquireInvalid() {
					return acquireInvalid;
				}

				@Override
				public @NotNull LongCounter reduceInvalid() {
					return reduceInvalid;
				}

				@Override
				public @NotNull LongCounter redo() {
					return redo;
				}
			};
		});
	}

	@Override
	public void addRecvSizeTime(long typeId, @Nullable Class<?> cls, int size, long timeNs) {
		ProtocolRecvMetric metric = protocolRecvMap.computeIfAbsent(typeId, (k) -> {
			Class<?> kls = (cls != null) ? cls : Protocol.getClassByTypeId(typeId);
			String name = kls != null ? kls.getName() : String.valueOf(typeId);
			return new ProtocolRecvMetric(
					protocol_recv_bytes.labelValues(name), protocol_duration_seconds.labelValues(name));

		});
		metric.bytes.inc(size);
		metric.processDuration.observe(Unit.nanosToSeconds(timeNs));
	}

	@Override
	public void addSendSize(long typeId, int size) {
		ProtocolSendMetric metric = protocolSendMap.computeIfAbsent(typeId, (k) -> {
			Class<?> kls = Protocol.getClassByTypeId(typeId);
			String name = kls != null ? kls.getName() : String.valueOf(typeId);
			return new ProtocolSendMetric(
					protocol_send.labelValues(name), protocol_send_bytes.labelValues(name));

		});
		metric.total.inc();
		metric.bytes.inc(size);
	}
}
