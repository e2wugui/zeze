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
import org.jetbrains.annotations.NotNull;

/*
curl -d "server 127.0.0.1:8089;server 127.0.0.1:8088;" 127.0.0.1:8081/upstream/dyhost
*/
public class ExporterNginxHttp implements IExporter {
	private static final @NotNull Logger logger = LogManager.getLogger(ExporterNginxHttp.class);

	// 提为字段：HttpClient（jdk17）自身没有关闭接口，其executor线程若不显式关闭，
	// 进程停机时线程泄漏（非守护线程阻止退出）。由close()统一关闭，Exporter.stop()触发。
	private final @NotNull ExecutorService executor = Executors.newSingleThreadExecutor();
	private final @NotNull HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.executor(executor)
			.build();
	private final @NotNull String url;
	private final long version;
	// FND7-61：选桶不匹配只warn一次（事件风暴下不刷屏）；onEdit由one-by-one单worker串行
	// 调用，volatile仅防御对调用线程模型的隐含依赖（对齐Exporter.failedServices风格）。
	private volatile boolean versionBucketEmptyWarned;

	/**
	 * 构造Nginx配置HTTP输出器。
	 * 当SM信息发生变化，会把服务列表通过http接口，输出Nginx中。
	 */
	public ExporterNginxHttp(@NotNull ExporterConfig config) {
		var param = config.getUrl();
		url = param.endsWith("/") ? param : param + "/";
		version = config.getVersion();
	}

	@Override
	public @NotNull Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(@NotNull String serviceName, @NotNull BServiceInfosVersion all) throws Exception {
		var ver0 = all.getInfos(version);
		if (ver0 == null) {
			// FND7-61可观测性：-version是选桶（SM按BServiceInfo注册时的version分桶，导出只取
			// 指定桶），桶不匹配时导出被静默跳过、dyups配置停在旧值且无任何留痕。选定桶为空
			// 且其他桶非空时warn一次（列出非空桶号）提示检查-version；全空（服务全部下线的
			// 过渡态）保持静默，语义与NginxConfig路径"无可导出地址"的info日志一致。
			if (!versionBucketEmptyWarned) {
				var buckets = new StringBuilder();
				for (var it = all.getInfosIterator(); it.moveToNext(); ) {
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
		// FND6-27：改同步+超时——原sendAsync+whenComplete仅覆盖传输异常，dyups返回非2xx仍只有
		// INFO日志，且无超时配置时future永不完成，更新静默丢失。exportAll运行于triggerOnChanged
		// 的one-by-one后台worker（不在IO/RPC线程），可安全阻塞；失败（传输/超时/非2xx）抛异常，
		// 由Exporter.onEdit既有的逐服务catch统一记录，本类不重复记error。
		var request = HttpRequest.newBuilder().uri(URI.create(url + serviceName))
				.timeout(Duration.ofSeconds(5))
				.POST(HttpRequest.BodyPublishers.ofString(post, StandardCharsets.UTF_8)).build();
		var resp = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
		if (resp.statusCode() / 100 != 2)
			throw new IOException("dyups register failed: code=" + resp.statusCode());
	}

	@Override
	public void close() {
		// 选shutdownNow而非shutdown+awaitTermination：停机路径不期望长等——在途请求可能正
		// 卡在超时等待中，shutdownNow直接中断它（send抛InterruptedException，停机语义可接受），
		// 线程立即释放，无需等待请求自然结束。
		executor.shutdownNow();
	}
}
