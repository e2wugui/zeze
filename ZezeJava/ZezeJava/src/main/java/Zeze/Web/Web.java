package Zeze.Web;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.ProviderApp;
import Zeze.Arch.ProviderService;
import Zeze.Builtin.Web.BSession;
import Zeze.Component.AutoKey;
import Zeze.Net.Protocol;
import Zeze.Util.LongConcurrentHashMap;

public class Web extends AbstractWeb {
	// private static final Logger logger = LogManager.getLogger(Web.class);
	private static final String CookieSessionName = "ZEZEWEBSESSIONID=";

	private final ProviderApp ProviderApp;
	public final ConcurrentHashMap<String, HttpServlet> Servlets = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, LongConcurrentHashMap<HttpExchange>> LinkExchanges = new ConcurrentHashMap<>();
	private AutoKey AutoKey;

	public Web(ProviderApp app) {
		ProviderApp = app;
		// 这里注册了所有的协议，向浏览器开放。
		// 这些协议仅用于Linkd转发Http请求，基于协议的客户端不能直接访问。需要保护。
		RegisterProtocols(ProviderApp.ProviderService);
		RegisterZezeTables(ProviderApp.Zeze);
	}

	@Override
	public void UnRegister() {
		UnRegisterZezeTables(ProviderApp.Zeze);
		UnRegisterProtocols(ProviderApp.ProviderService);
	}

	public void Start() {
		// 加入内建模块，最终将向ServiceManager注册这个模块；
		// 这样，Linkd负载选择的时候才能找到这个模块。
		ProviderApp.BuiltinModules.put(getFullName(), this);
		AutoKey = ProviderApp.Zeze.GetAutoKey("Zeze.Web.Session");
	}

	final LongConcurrentHashMap<HttpExchange> Exchanges(Protocol<?> context) {
		var linkName = ProviderService.GetLinkName(context.getSender());
		return LinkExchanges.computeIfAbsent(linkName, (k) -> new LongConcurrentHashMap<>());
	}

	@Override
	protected long ProcessRequestRequest(Zeze.Builtin.Web.Request r) {
		var x = new HttpExchange(this, r);
		if (null != Exchanges(r).putIfAbsent(r.Argument.getExchangeId(), x)) {
			// 重复的ExchangeId的错误，不能（不需要）直接关闭x，否则将会删除已经存在的Exchange。
			r.Result.setMessage("DuplicateExchangeId");
			return ErrorCode(DuplicateExchangeId);
		}
		var servlet = Servlets.get(r.Argument.getPath());
		if (null == servlet)
			return x.close(ErrorCode(UnknownPath404), "UnknownPath404", null, false);

		try {
			servlet.onRequest(x);
			if (r.Argument.isFinish())
				x.closeRequestBody();
			// r.SendResult 在 HttpExchange.sendResponseHeaders中调用。
			return 0;
		} catch (Throwable ex) {
			return x.close(ErrorCode(ServletException), "ServletException", ex, false);
		}
	}

	@Override
	protected long ProcessCloseExchangeRequest(Zeze.Builtin.Web.CloseExchange r) {
		Exchanges(r).remove(r.Argument.getExchangeId());
		r.SendResult();
		return 0;
	}

	@Override
	protected long ProcessRequestInputStreamRequest(Zeze.Builtin.Web.RequestInputStream r) {
		var x = Exchanges(r).get(r.Argument.getExchangeId());
		if (null == x) {
			return ErrorCode(ExchangeIdNotFound);
		}

		var servlet = Servlets.get(x.getRequest().getPath());
		if (null == servlet)
			return x.close(ErrorCode(UnknownPath404), "UnknownPath404", null, false);

		try {
			servlet.onUpload(x, r.Argument);
			if (r.Argument.isFinish())
				x.closeRequestBody();
			r.SendResult(); // onUpload 只处理数据，在这里发送结果。这个跟servlet.onRequest处理不同。
			return 0;
		} catch (Throwable ex) {
			return x.close(ErrorCode(OnUploadException), "OnUploadException", ex, false);
		}
	}

	public String putSession(String account) {
		var sessionId = String.valueOf(AutoKey.nextId());
		_tSessions.getOrAdd(sessionId).setAccount(account);
		return CookieSessionName + sessionId;
	}

	public BSession getSession(List<String> cookie) {
		if (cookie == null)
			return null;
		for (var c : cookie) {
			if (c.startsWith(CookieSessionName))
				return _tSessions.get(c.substring(CookieSessionName.length()));
		}
		return null;
	}
}
