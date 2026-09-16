package Zeze.Util;

import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Netty.HttpEndStreamHandle;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpResponseWithBodyStream;
import Zeze.Netty.HttpServer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TableKey;
import Zeze.Transaction.TransactionLevel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.datapoints.DistributionDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.exporter.common.PrometheusHttpExchange;
import io.prometheus.metrics.exporter.common.PrometheusHttpRequest;
import io.prometheus.metrics.exporter.common.PrometheusHttpResponse;
import io.prometheus.metrics.exporter.common.PrometheusScrapeHandler;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrometheusCounter implements ZezeCounter {
	private static final Logger logger = LogManager.getLogger(PrometheusCounter.class);


	/**
	 * service outputBuffSize的一个分布收集间隔。
	 */
	private static final int ServiceOutputObserveInterval = PropertiesHelper.getInt("ServiceOutputObserveInterval", 60);

	public static void addHttpHandler(HttpServer httpServer) {
		httpServer.addHandler("/metrics", 0,
				TransactionLevel.None, DispatchMode.Normal,
				new MetricHandler());
		httpServer.addHandler("/healthy", 0,
				TransactionLevel.None, DispatchMode.Direct,
				new HealthyHandler());
	}

	static class HealthyHandler implements HttpEndStreamHandle {

		@Override
		public void onEndStream(@NotNull HttpExchange x) {
			x.sendPlainText(HttpResponseStatus.OK, "Exporter is healthy.\n");
		}
	}

	static class MetricHandler implements HttpEndStreamHandle {
		private final PrometheusScrapeHandler prometheusScrapeHandler = new PrometheusScrapeHandler();

		@Override
		public void onEndStream(@NotNull HttpExchange x) throws Exception {
			prometheusScrapeHandler.handleRequest(new HttpExchangeAdapter(x));
		}
	}

	static class HttpExchangeAdapter implements PrometheusHttpExchange {
		private final HttpExchange httpExchange;
		private final HttpRequestAdaptor request = new HttpRequestAdaptor();
		private final HttpResponseAdaptor response = new HttpResponseAdaptor();

		class HttpRequestAdaptor implements PrometheusHttpRequest {

			@Override
			public String getQueryString() {
				return httpExchange.query();
			}

			@Override
			public Enumeration<String> getHeaders(String name) {
				HttpRequest req = httpExchange.request();
				if (req == null) {
					return Collections.emptyEnumeration();
				}
				return Collections.enumeration(req.headers().getAll(name));
			}

			@Override
			public String getMethod() {
				HttpRequest req = httpExchange.request();
				if (req == null) {
					return "";
				}
				return req.method().name();
			}

			@Override
			public String getRequestPath() {
				return httpExchange.path();
			}
		}

		class HttpResponseAdaptor implements PrometheusHttpResponse {
			private final Map<String, Object> headers = new LinkedHashMap<>();

			@Override
			public void setHeader(String name, String value) {
				headers.put(name, value);
			}

			@Override
			public OutputStream sendHeadersAndGetBody(int statusCode, int contentLength) {
				return HttpResponseWithBodyStream.sendHeadersAndGetBody(httpExchange.context(),
						HttpResponseStatus.valueOf(statusCode),
						headers,
						contentLength);
			}
		}

		public HttpExchangeAdapter(@NotNull HttpExchange x) {
			httpExchange = x;
		}

		@Override
		public PrometheusHttpRequest getRequest() {
			return request;
		}

		@Override
		public PrometheusHttpResponse getResponse() {
			return response;
		}

		@Override
		public void handleException(IOException e) {
			httpExchange.send500(e);
		}

		@Override
		public void handleException(RuntimeException e) {
			httpExchange.send500(e);
		}

		@Override
		public void close() {
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

	private final ConcurrentHashMap<Long, LongCounter[]> tableCounterMap = new ConcurrentHashMap<>();
	// handle按name缓存复用：协议等热路径每次执行都新建Procedure，不缓存则每次执行重复
	// new PromProcedureCounter及其构造期的4次labelValues解析（DataPoint挂collector永不淘汰，长持无副作用）。
	private final ConcurrentHashMap<String, ProcedureCounter> procedureCounterMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, LongObserver> runTimeMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, DistributionDataPoint> taskDurationMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolRecvMetric> protocolRecvMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, DistributionDataPoint> protocolDispatchMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ProtocolSendMetric> protocolSendMap = new ConcurrentHashMap<>();
	private final Map<String, ServiceMetric> serviceMap = new HashMap<>();
	private final ReentrantLock serviceMapMutex = new ReentrantLock();

	private final Histogram task_duration_seconds = Histogram.builder().name("task_duration_seconds")
			.labelNames("task").register();

	private final Counter procedure_started = Counter.builder().name("procedure_started")
			.labelNames("procedure").register();
	private final Counter procedure_completed = Counter.builder().name("procedure_completed")
			.labelNames("procedure", "result_code").register();
	private final Histogram procedure_duration_seconds = Histogram.builder().name("procedure_duration_seconds")
			.labelNames("procedure", "result_code").register();
	private final Counter procedure_redo = Counter.builder().name("procedure_redo")
			.labelNames("procedure").register();
	private final Counter procedure_redo_and_release_lock = Counter.builder().name("procedure_redo_and_release_lock")
			.labelNames("procedure").register();
	private final Histogram procedure_many_locks = Histogram.builder().name("procedure_many_locks")
			.labelNames("procedure").register();

	private final Counter database_table_operation = Counter.builder().name("database_table_operation")
			.labelNames("table", "operation").register();

	private final Counter protocol_recv_bytes = Counter.builder().name("protocol_recv_bytes")
			.labelNames("protocol").register();
	private final Histogram protocol_duration_seconds = Histogram.builder().name("protocol_duration_seconds")
			.labelNames("protocol").register();
	private final Counter protocol_send = Counter.builder().name("protocol_send")
			.labelNames("protocol").register();
	private final Histogram protocol_dispatch_seconds = Histogram.builder().name("protocol_dispatch_seconds")
			.labelNames("protocol").unit(Unit.SECONDS).register();
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
		Counter counter = Counter.builder().name(PrometheusNaming.sanitizeMetricName(name)).register();
		return counter::inc;
	}

	@Override
	public @NotNull LabeledCounterCreator allocLabeledCounterCreator(@NotNull String name, @NotNull String... labelNames) {
		if (labelNames.length == 0) {
			throw new IllegalArgumentException("labelNames empty");
		}
		// sanitizeMetricName：调用方传入的 name（如带点号的类名）含非法字符时，Prometheus 的
		// MetricMetadata.validate() 会抛 IllegalArgumentException 进调用方热路径；这里统一替换非法字符。
		Counter counter = Counter.builder().name(PrometheusNaming.sanitizeMetricName(name)).labelNames(labelNames).register();
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
		Histogram histogram = Histogram.builder().name(PrometheusNaming.sanitizeMetricName(name)).unit(Unit.SECONDS).labelNames(labelNames).register();
		return labels -> {
			DistributionDataPoint dp = histogram.labelValues(labels);
			return (amount) -> dp.observe(Unit.nanosToSeconds(amount));
		};
	}

	@Override
	public @NotNull LongObserver getRunTimeObserver(@NotNull Object key) {
		// key归一化：Class取类名、其余toString，缓存与统计统一按字符串名聚合
		var name = key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key);
		return fastGetOrAdd(runTimeMap, name, k -> {
			Histogram histogram = Histogram.builder().name(PrometheusNaming.sanitizeMetricName(k)).unit(Unit.SECONDS).register();
			return (LongObserver)amount -> histogram.observe(Unit.nanosToSeconds(amount));
		});
	}

	@Override
	public void addTaskRunTime(@NotNull Object key, long timeNs) {
		var task = key instanceof Class ? ((Class<?>)key).getName() : String.valueOf(key);
		fastGetOrAdd(taskDurationMap, task, task_duration_seconds::labelValues)
				.observe(Unit.nanosToSeconds(timeNs));
	}

	private record ServiceOutputObserve(Service service, DistributionDataPoint outputObserve) implements Action0 {

		@Override
			public void run() throws Exception {
				service.updateRecvSendSize(); // 为 service counter with callback的相关metric服务
				service.foreach(socket -> {
					if (socket instanceof TcpSocket)
						outputObserve.observe(((TcpSocket)socket).getOutputBufferSize());
				});
			}
		}

	@Override
	public void init() {
		try {
			JvmMetrics.builder().register(); // 标准JVM指标
		} catch (NoClassDefFoundError e) { // instrumentation-jvm为compileOnly，运行时缺失则跳过
			logger.warn("prometheus-metrics-instrumentation-jvm not present, skip jvm metrics", e);
		}
	}

	@Override
	public void serviceStart(@NotNull Service service) {
		serviceMapMutex.lock();
		try {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric != null) {// 按说不改有重名的，如果重名忽略后来的
				return;
			}

			DistributionDataPoint outputObserve = service_output_buffer_bytes.labelValues(name);
			long milliSec = ServiceOutputObserveInterval * 1000L;
			ScheduledFuture<?> scheduler = TaskSpec.ofAction(new ServiceOutputObserve(service, outputObserve))
					.schedulePeriodNow(Random.getInstance().nextLong(milliSec), milliSec);

			serviceMap.put(name, new ServiceMetric(service, outputObserve, scheduler));
		} finally {
			serviceMapMutex.unlock();
		}
	}

	@Override
	public void serviceStop(@NotNull Service service) {
		serviceMapMutex.lock();
		try {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric == null) { // 不应该出现
				return;
			}

			if (service != metric.service) { //重名，并且是后来者
				return;
			}

			serviceMap.remove(name);
			service_output_buffer_bytes.remove(name);
			metric.scheduler.cancel(false);
		} finally {
			serviceMapMutex.unlock();
		}
	}

	private List<Service> getServiceSnapshot() {
		serviceMapMutex.lock();
		try {
			return serviceMap.values().stream().map(s -> s.service).collect(Collectors.toList());
		} finally {
			serviceMapMutex.unlock();
		}
	}

	@Override
	public @NotNull ProcedureCounter allocProcedureCounter(@NotNull String name) {
		return fastGetOrAdd(procedureCounterMap, name, PromProcedureCounter::new);
	}

	// 每name一次预绑定全部DataPoint；end的result_code维度在handle内按码缓存
	// （基数被ResultCodeCap封顶，默认20+other），调用路径零labelValues解析零分配。
	private final class PromProcedureCounter implements ProcedureCounter {
		private final @NotNull String name;
		private final @NotNull CounterDataPoint started;
		private final @NotNull CounterDataPoint redo;
		private final @NotNull CounterDataPoint redoAndReleaseLock;
		private final @NotNull DistributionDataPoint manyLocks;
		private final @NotNull ResultCodeCap codeCap = new ResultCodeCap();
		private final @NotNull LongConcurrentHashMap<Completed> completedByCode = new LongConcurrentHashMap<>();
		private volatile @Nullable Completed otherCompleted;

		private record Completed(@NotNull CounterDataPoint count, @NotNull DistributionDataPoint duration) {
		}

		PromProcedureCounter(@NotNull String name) {
			this.name = name;
			started = procedure_started.labelValues(name);
			redo = procedure_redo.labelValues(name);
			redoAndReleaseLock = procedure_redo_and_release_lock.labelValues(name);
			manyLocks = procedure_many_locks.labelValues(name);
		}

		private @NotNull Completed newCompleted(@NotNull String codeLabel) {
			return new Completed(procedure_completed.labelValues(name, codeLabel),
					procedure_duration_seconds.labelValues(name, codeLabel));
		}

		private @NotNull Completed completed(long resultCode) {
			var c = completedByCode.get(resultCode);
			if (c != null)
				return c;
			// result_code基数封顶：超出上限归入other，防业务错误码撑爆series
			if (codeCap.accept(resultCode))
				return completedByCode.computeIfAbsent(resultCode, code -> newCompleted(String.valueOf(code)));
			var o = otherCompleted;
			if (o == null)
				otherCompleted = o = newCompleted(ResultCodeCap.OTHER);
			return o;
		}

		@Override
		public void start() {
			started.inc();
		}

		@Override
		public void end(long resultCode, long timeNs) {
			var c = completed(resultCode);
			c.count().inc();
			c.duration().observe(Unit.nanosToSeconds(timeNs));
		}

		@Override
		public void redo() {
			redo.inc();
		}

		@Override
		public void redoAndReleaseLock() {
			redoAndReleaseLock.inc();
		}

		@Override
		public void manyLocks(int count) {
			manyLocks.observe(count);
		}
	}

	@Override
	public @NotNull LongCounter tableCounter(long tableId, @NotNull TableMetric metric) {
		// 每表一次构建全部DataPoint并缓存，调用路径零分配
		var counters = fastGetOrAdd(tableCounterMap, tableId, k -> {
			var tableName = TableKey.tables.get(tableId);
			var table = tableName != null ? tableName : String.valueOf(tableId);
			var arr = new LongCounter[TableMetric.values().length];
			for (var m : TableMetric.values())
				arr[m.ordinal()] = database_table_operation.labelValues(table, m.key)::inc;
			return arr;
		});
		return counters[metric.ordinal()];
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
	public void addRecvDispatchTime(long typeId, long timeNs) {
		fastGetOrAdd(protocolDispatchMap, typeId, k -> {
			Class<?> kls = Protocol.getClassByTypeId(typeId);
			String name = kls != null ? kls.getName() : String.valueOf(typeId);
			return protocol_dispatch_seconds.labelValues(name);
		}).observe(Unit.nanosToSeconds(timeNs));
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
