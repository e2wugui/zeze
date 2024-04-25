package Zeze.Services.ServiceManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/*
curl -d "server 127.0.0.1:8089;server 127.0.0.1:8088;" 127.0.0.1:8081/upstream/dyhost
*/
public class ExporterNginxHttp implements IExporter {
	private static final @NotNull Logger logger = LogManager.getLogger(ExporterNginxHttp.class);

	private final @NotNull HttpClient httpClient = HttpClient.newBuilder()
			.executor(Executors.newSingleThreadExecutor())
			.build();
	private final @NotNull String url;
	private final long version;

	/**
	 * 构造Nginx配置HTTP输出器。
	 * 当SM信息发生变化，会把服务列表通过http接口，输出Nginx中。
	 *
	 * @param param http-url
	 */
	public ExporterNginxHttp(@NotNull String param, @NotNull String param2) {
		url = param.endsWith("/") ? param : param + "/";
		version = Long.parseLong(param2);
	}

	@Override
	public @NotNull Type getType() {
		return Type.eAll;
	}

	@Override
	public void exportAll(@NotNull String serviceName, @NotNull BServiceInfosVersion all) throws Exception {
		var sb = new StringBuilder();
		var ver0 = all.getInfosVersion().get(version);
		if (ver0 == null)
			return;

		for (var info : ver0.getServiceInfoListSortedByIdentity()) {
			if (info.getPassiveIp().isBlank())
				continue;
			sb.append("server ").append(info.getPassiveIp()).append(':').append(info.getPassivePort()).append(';');
		}
		httpClient.sendAsync(HttpRequest.newBuilder().uri(URI.create(url + serviceName))
				.POST(HttpRequest.BodyPublishers.ofString(sb.toString(), StandardCharsets.UTF_8)).build(), h -> {
			logger.info("res: code={}", h.statusCode());
			return HttpResponse.BodySubscribers.discarding();
		});
	}
}
