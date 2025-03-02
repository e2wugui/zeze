package Zeze.Util;

import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Transaction.TableKey;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.model.snapshots.Unit;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrometheusCounter implements ZezeCounter {

	private static final int ServiceOutputObserveInterval = PropertiesHelper.getInt("ServiceOutputObserveInterval", 60);

	public static void startHttpServer() {
		startHttpServer(9400);
	}

	public static void startHttpServer(int port) {
		try {
			HTTPServer server = HTTPServer.builder().port(port).buildAndStart();
			logger.info("httpserver listening on port http://localhost:{}/metrics", server.getPort());
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

	private static class ServiceMetric {
		public final Service service;
		public final DistributionDataPoint outputObserve;
		public final ScheduledFuture<?> scheduler;

		private ServiceMetric(Service service, DistributionDataPoint outputObserve, ScheduledFuture<?> scheduler) {
			this.service = service;
			this.scheduler = scheduler;
			this.outputObserve = outputObserve;
		}
	}

	private final ConcurrentHashMap<Object, LongObserver> runTimeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, TableCounter> tableCounterMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolRecvMetric> protocolRecvMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolSendMetric> protocolSendMap = new ConcurrentHashMap<>();
	private final Map<String, ServiceMetric> serviceMap = new HashMap<>();
	private final Object serviceMapMutex = new Object();

	private final Counter procedure_started = Counter.builder().name("procedure_started")
			.labelNames("procedure").register();
	private final Counter procedure_completed = Counter.builder().name("procedure_completed")
			.labelNames("procedure", "result_code").register();
	private final Histogram procedure_duration_seconds = Histogram.builder().name("procedure_duration_seconds")
			.labelNames("procedure", "result_code").register();

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

	private final Histogram service_output_buffer_bytes = Histogram.builder().name("service_output_buffer_bytes")
			.labelNames("service").register();
	private final CounterWithCallback service_recv = CounterWithCallback.builder().name("service_recv")
			.labelNames("service").callback(callback -> {
				List<Service> serviceSnapshot = getServiceSnapshot();
				for (Service service : serviceSnapshot) {
					callback.call(service.getRecvCount(), service.getName());
				}
			}).register();

	private final CounterWithCallback service_recv_bytes = CounterWithCallback.builder().name("service_recv_bytes")
			.labelNames("service").callback(callback -> {
				List<Service> serviceSnapshot = getServiceSnapshot();
				for (Service service : serviceSnapshot) {
					callback.call(service.getRecvSize(), service.getName());
				}
			}).register();

	private final CounterWithCallback service_send = CounterWithCallback.builder().name("service_send")
			.labelNames("service").callback(callback -> {
				List<Service> serviceSnapshot = getServiceSnapshot();
				for (Service service : serviceSnapshot) {
					callback.call(service.getSendCount(), service.getName());
				}
			}).register();

	private final CounterWithCallback service_send_bytes = CounterWithCallback.builder().name("service_send_bytes")
			.labelNames("service").callback(callback -> {
				List<Service> serviceSnapshot = getServiceSnapshot();
				for (Service service : serviceSnapshot) {
					callback.call(service.getSendSize(), service.getName());
				}
			}).register();
	private final CounterWithCallback service_send_raw_bytes = CounterWithCallback.builder().name("service_send_raw_bytes")
			.labelNames("service").callback(callback -> {
				List<Service> serviceSnapshot = getServiceSnapshot();
				for (Service service : serviceSnapshot) {
					callback.call(service.getSendRawSize(), service.getName());
				}
			}).register();


	@Override
	public @NotNull LongCounter allocCounter(@NotNull String name) {
		Counter counter = Counter.builder().name(name).register();
		return counter::inc;
	}

	@Override
	public @NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames) {
		if (labelNames.length == 0) {
			throw new IllegalArgumentException("labelNames empty");
		}
		Counter counter = Counter.builder().name(name).labelNames(labelNames).register();
		return labels -> {
			CounterDataPoint dp = counter.labelValues(labels);
			return dp::inc;
		};
	}

	@Override
	public @NotNull LabeledObserverCreator allocRunTimeObserverCreator(@NotNull String name, @NotNull String... labelNames) {
		if (labelNames.length == 0) {
			throw new IllegalArgumentException("labelNames empty");
		}
		Histogram histogram = Histogram.builder().name(name).unit(Unit.SECONDS).labelNames(labelNames).register();
		return labels -> {
			DistributionDataPoint dp = histogram.labelValues(labels);
			return (amount) -> dp.observe(Unit.nanosToSeconds(amount));
		};
	}

	@Override
	public @NotNull LongObserver getRunTimeObserver(@NotNull Object key) {
		return fastGetOrAdd(runTimeMap, key, (k) -> {
			String name = k instanceof Class ? ((Class<?>)k).getName() : String.valueOf(k);
			Histogram histogram = Histogram.builder().name(name).unit(Unit.SECONDS).register();
			return (amount) -> histogram.observe(Unit.nanosToSeconds(amount));
		});
	}

	@Override
	public void addRunTime(@NotNull Object key, long timeNs) {
		getRunTimeObserver(key).observe(timeNs);
	}

	private static class ServiceOutputObserve implements Action0 {
		private final Service service;
		private final DistributionDataPoint outputObserve;

		private ServiceOutputObserve(Service service, DistributionDataPoint outputObserve) {
			this.service = service;
			this.outputObserve = outputObserve;
		}

		@Override
		public void run() throws Exception {
			service.updateRecvSendSize(); // 为 service counter with callback的相关metric服务
			service.foreach(socket -> outputObserve.observe(socket.getOutputBufferSize()));
		}
	}

	@Override
	public void serviceStart(Service service) {
		synchronized (serviceMapMutex) {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric != null) {// 按说不改有重名的，如果重名忽略后来的
				return;
			}

			DistributionDataPoint outputObserve = service_output_buffer_bytes.labelValues(name);
			long milliSec = ServiceOutputObserveInterval * 1000L;
			ScheduledFuture<?> scheduler = Task.scheduleUnsafe(Random.getInstance().nextLong(milliSec),
					milliSec, new ServiceOutputObserve(service, outputObserve));

			serviceMap.put(name, new ServiceMetric(service, outputObserve, scheduler));
		}
	}

	@Override
	public void serviceStop(Service service) {
		synchronized (serviceMapMutex) {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric == null) { // 不应该出现
				return;
			}

			if (service != metric.service) { //重名，并且是后来者
				return;
			}

			metric.scheduler.cancel(false);
		}
	}

	private List<Service> getServiceSnapshot() {
		synchronized (serviceMapMutex) {
			return serviceMap.values().stream().map(s -> s.service).toList();
		}
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
		return fastGetOrAdd(tableCounterMap, tableId, k -> {
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
		ProtocolRecvMetric metric = fastGetOrAdd(protocolRecvMap, typeId, (k) -> {
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
		ProtocolSendMetric metric = fastGetOrAdd(protocolSendMap, typeId, (k) -> {
			Class<?> kls = Protocol.getClassByTypeId(typeId);
			String name = kls != null ? kls.getName() : String.valueOf(typeId);
			return new ProtocolSendMetric(
					protocol_send.labelValues(name), protocol_send_bytes.labelValues(name));
		});

		metric.total.inc();
		metric.bytes.inc(size);
	}

	static <K, V> V fastGetOrAdd(ConcurrentHashMap<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
		// 若map的读取命中率极高（例如超过90%的get能直接命中）先get应该效率会高点
		V v = map.get(key);
		if (v != null) {
			return v;
		}
		return map.computeIfAbsent(key, mappingFunction);
	}
}
