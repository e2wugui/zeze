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

import org.jspecify.annotations.Nullable;

/** ZezeCounter 的 Prometheus 实现：注册指标并通过 /metrics HTTP 暴露。 */
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
		public void onEndStream(HttpExchange x) {
			x.sendPlainText(HttpResponseStatus.OK, "Exporter is healthy.\n");
		}
	}

	static class MetricHandler implements HttpEndStreamHandle {
		private final PrometheusScrapeHandler prometheusScrapeHandler = new PrometheusScrapeHandler();

		@Override
		public void onEndStream(HttpExchange x) throws Exception {
			prometheusScrapeHandler.handleRequest(new HttpExchangeAdapter(x));
		}
	}

	static class HttpExchangeAdapter implements PrometheusHttpExchange {
		private final HttpExchange httpExchange;
		private final HttpRequestAdaptor request = new HttpRequestAdaptor();
		private final HttpResponseAdaptor response = new HttpResponseAdaptor();

		class HttpRequestAdaptor implements PrometheusHttpRequest {

			@Override
			public @Nullable String getQueryString() {
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
				return HttpResponseWithBodyStream.sendHeadersAndGetBody(httpExchange,
					HttpResponseStatus.valueOf(statusCode),
					headers,
					contentLength);
			}
		}

		public HttpExchangeAdapter(HttpExchange x) {
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

	private record ProtocolRecvMetric(CounterDataPoint bytes,
									  DistributionDataPoint processDuration) {
	}

	private record ProtocolSendMetric(CounterDataPoint total,
									  CounterDataPoint bytes) {
	}

	private record ServiceMetric(Service service,
								 DistributionDataPoint outputObserve,
								 ScheduledFuture<?> scheduler) {
	}

	private final ConcurrentHashMap<Long, LongCounter[]> tableCounterMap = new ConcurrentHashMap<>();
	// 按 name 缓存复用句柄，避免热路径重复创建与 labelValues 解析
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
	public LongCounter allocCounter(String name) {
		Counter counter = Counter.builder().name(PrometheusNaming.sanitizeMetricName(name)).register();
		return counter::inc;
	}

	@Override
	public LabeledCounterCreator allocLabeledCounterCreator(String name, String... labelNames) {
		if (labelNames.length == 0) {
			throw new IllegalArgumentException("labelNames empty");
		}
		// 规范化指标名，避免含非法字符（如点号）触发校验异常
		Counter counter = Counter.builder().name(PrometheusNaming.sanitizeMetricName(name)).labelNames(labelNames).register();
		return labels -> {
			CounterDataPoint dp = counter.labelValues(labels);
			return dp::inc;
		};
	}

	@Override
	public LabeledObserverCreator allocRunTimeObserverCreator(String name, String... labelNames) {
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
	public LongObserver getRunTimeObserver(String name) {
		// 以 task 标签承载名称维度，统一归入单一指标族且无指标名合法性约束
		return fastGetOrAdd(runTimeMap, name, k -> amount
			-> task_duration_seconds.labelValues(k).observe(Unit.nanosToSeconds(amount)));
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
	public void serviceStart(Service service) {
		serviceMapMutex.lock();
		try {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric != null) { // 重名时忽略后注册者
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
	public void serviceStop(Service service) {
		serviceMapMutex.lock();
		try {
			String name = service.getName();
			ServiceMetric metric = serviceMap.get(name);
			if (metric == null) {
				return;
			}

			if (service != metric.service) { // 重名且为后注册者，忽略
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
	public ProcedureCounter allocProcedureCounter(String name) {
		return fastGetOrAdd(procedureCounterMap, name, PromProcedureCounter::new);
	}

	// 预绑定固定维度 DataPoint，result_code 维度按码缓存并受 ResultCodeCap 基数封顶
	private final class PromProcedureCounter implements ProcedureCounter {
		private final String name;
		private final CounterDataPoint started;
		private final CounterDataPoint redo;
		private final CounterDataPoint redoAndReleaseLock;
		private final DistributionDataPoint manyLocks;
		private final ResultCodeCap codeCap = new ResultCodeCap();
		private final LongConcurrentHashMap<Completed> completedByCode = new LongConcurrentHashMap<>();
		private volatile @Nullable Completed otherCompleted;

		private record Completed(CounterDataPoint count, DistributionDataPoint duration) {
		}

		PromProcedureCounter(String name) {
			this.name = name;
			started = procedure_started.labelValues(name);
			redo = procedure_redo.labelValues(name);
			redoAndReleaseLock = procedure_redo_and_release_lock.labelValues(name);
			manyLocks = procedure_many_locks.labelValues(name);
		}

		private Completed newCompleted(String codeLabel) {
			return new Completed(procedure_completed.labelValues(name, codeLabel),
				procedure_duration_seconds.labelValues(name, codeLabel));
		}

		private Completed completed(long resultCode) {
			var c = completedByCode.get(resultCode);
			if (c != null)
				return c;
			// result_code基数封顶：超出上限归入other，防业务错误码撑爆series
			if (codeCap.accept(resultCode))
				//noinspection DataFlowIssue
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
	public LongCounter tableCounter(long tableId, TableMetric metric) {
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
	public Snapshot snapshotAndReset() {
		return Snapshot.EMPTY;
	}

	@Override
	public Snapshot getLastSnapshot() {
		return Snapshot.EMPTY;
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
		// 高命中率路径先无锁 get，减少 computeIfAbsent 开销
		V v = map.get(key);
		if (v != null) {
			return v;
		}
		return map.computeIfAbsent(key, mappingFunction);
	}
}
