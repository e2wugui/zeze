package Zeze.Netty;

import java.util.HashMap;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * 向 Consul 注册 HttpServer 服务，便于第三方（比如Nginx)发现。
 * todo 为了支持更多注册和管理，可能需要抽象一个接口。先不写。
 */
public class Consul {
	// 这个path对所有的httpserver都一样。
	private static final String PassiveKeepAlivePath = "/Zeze_Netty_Consul_PassiveKeepAlivePath";
	private final ConsulClient client;
	private final HashMap<HttpServer, Registration> services = new HashMap<>(); // guarded by this

	private record Registration(String serviceId, HttpHandler handler) {
	}

	public Consul(String consulHost) {
		this(new ConsulClient(consulHost));
	}

	public Consul(ConsulClient client) {
		this.client = client;
	}

	public ConsulClient getClient() {
		return client;
	}

	public synchronized void register(String serviceName, HttpServer httpServer) throws Exception {
		var ip = httpServer.getExportIp();
		var port = httpServer.getPort();

		var serviceId = "@" + ip + ":" + port + "@" + serviceName; // see todo below
		if (services.containsKey(httpServer))
			throw new IllegalStateException("duplicate register " + serviceId);

		var newService = new NewService();
		newService.setAddress(ip);
		newService.setPort(port);
		newService.setId(serviceId);
		newService.setName(serviceName); // todo 这是服务名，一个服务名下有多个Id？

		// 健康检查 URL 仅含标准 host[:port]/path，IPv6 字面量需加方括号。
		var host = ip.contains(":") ? "[" + ip + "]" : ip;
		var checker = new NewService.Check();
		checker.setHttp((httpServer.sslCtx != null ? "https://" : "http://") + host + ":" + port + PassiveKeepAlivePath);
		checker.setInterval("10s");
		checker.setDeregisterCriticalServiceAfter("5m"); // 进程失联后自动注销，兜底残留注册
		newService.setCheck(checker);
		/* NewService.Check 其余可选配置项示例（摘自 Consul 文档，按需设置）：
			{
			"check": {
			"id": "api",
			"name": "HTTP API on port 5000",
			"http": "https://localhost:5000/health",
			"tls_skip_verify": false,
			"method": "POST",
			"header": {"x-foo":["bar", "baz"]},
			"interval": "10s",
			"timeout": "1s"
			}
		 */
		var handler = new HttpHandler(1024, null, null, Consul::passiveKeepAlive);
		httpServer.addHandler(PassiveKeepAlivePath, handler);
		try {
			client.agentServiceRegister(newService);
		} catch (RuntimeException e) {
			httpServer.removeHandler(PassiveKeepAlivePath, handler);
			throw e;
		}
		services.put(httpServer, new Registration(serviceId, handler));
	}

	public synchronized void stop() {
		for (var e : services.entrySet()) {
			var httpServer = e.getKey();
			var registration = e.getValue();
			var serviceId = registration.serviceId();
			try {
				client.agentServiceDeregister(serviceId);
			} catch (RuntimeException ex) {
				// 逐个尽力注销，单个失败不中断其余清理。
				Netty.logger.error("consul deregister {}", serviceId, ex);
			}
			httpServer.removeHandler(PassiveKeepAlivePath, registration.handler());
		}
		services.clear();
	}

	private static void passiveKeepAlive(HttpExchange x) {
		x.sendPlainText(HttpResponseStatus.OK, ""); // todo consul被动保活结果有没有什么规定
	}
}
