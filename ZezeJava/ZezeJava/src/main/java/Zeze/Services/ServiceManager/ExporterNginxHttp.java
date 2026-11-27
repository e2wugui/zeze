package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Nginx dyups HTTP 导出器：把 SM 服务地址经 HTTP POST 写入 dyups 接口。
 * <p>等价示例：{@code curl -d "server 127.0.0.1:8089;server 127.0.0.1:8088;" 127.0.0.1:8081/upstream/dyhost}
 */
public class ExporterNginxHttp implements IExporter {
	private static final Logger logger = LogManager.getLogger(ExporterNginxHttp.class);

	// 显式持有 executor，便于 close() 停机释放非守护线程。
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.executor(executor)
			.build();
	private final String url;
	private final long version;
	// 选桶为空告警只输出一次，避免事件高频触发时刷屏。
	private volatile boolean versionBucketEmptyWarned;

	/**
	 * 构造Nginx配置HTTP输出器。
	 * 当SM信息发生变化，会把服务列表通过http接口，输出Nginx中。
	 */
	public ExporterNginxHttp(ExporterConfig config) {
		var param = config.getUrl();
		url = param.endsWith("/") ? param : param + "/";
		version = config.getVersion();
	}

	@Override
	public Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(String serviceName, BServiceInfosVersion all) throws Exception {
		var ver0 = all.getInfos(version);
		if (ver0 == null) {
			// 仅在选定版本桶为空而其他桶非空时告警一次，提示检查 -version 配置。
			if (!versionBucketEmptyWarned) {
				var buckets = new StringBuilder();
				for (var it = all.getInfosIterator(); it.moveToNext(); ) {
					//noinspection DataFlowIssue
					if (!it.value().getSortedIdentities().isEmpty()) {
						if (!buckets.isEmpty())
							buckets.append(',');
						buckets.append(it.key());
					}
				}
				if (!buckets.isEmpty()) {
					versionBucketEmptyWarned = true;
					logger.warn("ExporterNginxHttp: -version={} bucket not found for service '{}', "
									+ "non-empty buckets: [{}]; export skipped, dyups config NOT updated; "
									+ "check -version to match the version services registered with",
							version, serviceName, buckets);
				}
			}
			return;
		}

		var sb = new StringBuilder();
		for (var info : ver0.getSortedIdentities()) {
			if (info.getPassiveIp().isBlank())
				continue;
			sb.append("server ").append(info.getPassiveIp()).append(':').append(info.getPassivePort()).append(';');
		}
		var post = sb.toString();

		logger.info("HttpRequest: url={}, serviceName={}, post={}", url, serviceName, post);
		// 运行于独立后台 worker，采用带超时的同步发送，非 2xx/异常抛给上层统一记账与告警。
		var request = HttpRequest.newBuilder().uri(URI.create(url + serviceName))
				.timeout(Duration.ofSeconds(5))
				.POST(HttpRequest.BodyPublishers.ofString(post, StandardCharsets.UTF_8)).build();
		var resp = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
		if (resp.statusCode() / 100 != 2)
			throw new IOException("dyups register failed: code=" + resp.statusCode());
	}

	@Override
	public void close() {
		// 停机时立即中断在途请求并释放线程。
		executor.shutdownNow();
	}
}
