package Zeze.Arch;

import Zeze.Application;
import Zeze.Builtin.LinksInfo.BLinkInfo;
import Zeze.Config;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.ServiceManager.AbstractAgent;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Util.PropertiesHelper;
import io.netty.handler.codec.http.HttpResponseStatus;

public class LinksInfo extends AbstractLinksInfo {
	private final Netty netty = new Netty();
	private final HttpServer httpServer;
	private final AbstractAgent agent;
	private String defaultLinkServiceName;

	public LinksInfo() throws Exception {
		var conf = Config.load();
		agent = Application.createServiceManager(conf, "LinksInfo");
		if (null == agent)
			throw new IllegalStateException("agent is null. check your config for ServiceManager.");
		httpServer = new HttpServer();
		RegisterHttpServlet(httpServer);
	}

	public void start() throws Exception {
		agent.start();
		agent.waitReady();
		httpServer.start(netty, 80);
	}

	public void stop() throws Exception {
		agent.close();
		httpServer.close();
		netty.close();
	}

	public void subscribeLinkService(String linkServiceName) {
		if (null == defaultLinkServiceName)
			defaultLinkServiceName = linkServiceName;
		agent.subscribeService(new BSubscribeInfo(linkServiceName));
	}

	public static void main(String [] args) throws Exception {
		var linksInfo = new LinksInfo();
		linksInfo.start();
		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-link":
				linksInfo.subscribeLinkService(args[++i]);
				break;
			case "-default":
				linksInfo.defaultLinkServiceName = args[++i];
				break;
			}
		}
		try {
			if (linksInfo.defaultLinkServiceName == null)
				throw new IllegalArgumentException("no link service present.");
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} finally {
			linksInfo.stop();
		}
	}

	private void send(HttpExchange x, String sep) {
		var query = x.queryMap();
		var linkServiceName = PropertiesHelper.getString(query, "link", this.defaultLinkServiceName);
		var version = PropertiesHelper.getInt(query, "version", 0);

		var sb = new StringBuilder();
		var subs = agent.getSubscribeStates().get(linkServiceName);
		if (null != subs) {
			var infoVersion = subs.getServiceInfos(version);
			if (null != infoVersion) {
				for (var info : infoVersion.getSortedIdentities()) {
					if (info.getExtraInfo().size() == 0)
						continue; // skip empty extra info
					var extra = new BLinkInfo.Data();
					extra.decode(ByteBuffer.Wrap(info.getExtraInfo()));
					sb.append(extra.getIp()).append(":").append(extra.getPort()).append(sep);
				}
			}
		}
		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}

	@Override
	protected void OnServletLinksTextMultiLine(HttpExchange x) throws Exception {
		send(x, "\n");
	}

	@Override
	protected void OnServletLinksTextSingleLine(HttpExchange x) throws Exception {
		send(x, ";");
	}
}
