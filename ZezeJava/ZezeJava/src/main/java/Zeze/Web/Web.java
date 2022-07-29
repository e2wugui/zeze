package Zeze.Web;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.ProviderApp;
import Zeze.Builtin.Web.BSession;
import Zeze.Component.AutoKey;
import Zeze.Net.Binary;

public class Web extends AbstractWeb {

    public final ProviderApp ProviderApp;
    public final ConcurrentHashMap<String, HttpServlet> Servlets = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Long, HttpExchange> Exchanges = new ConcurrentHashMap<>();

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

    @Override
    protected long ProcessAuthOkRequest(Zeze.Builtin.Web.AuthOk r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessRequestRequest(Zeze.Builtin.Web.Request r) throws Throwable {
        var x = new HttpExchange(this, r);
        if (null != Exchanges.putIfAbsent(r.Argument.getExchangeId(), x)) {
            // 重复的ExchangeId的错误，不能（不需要）直接关闭x，否则将会删除已经存在的Exchange。
            var error = ErrorCode(DuplicateExchangeId);
            r.Result.setMessage("DuplicateExchangeId");
            r.SendResultCode(error);
            return error;
        }
        var servlet = Servlets.get(r.Argument.getPath());
        if (null == servlet)
            return x.close(ErrorCode(UnknownPath404), "UnknownPath404", null);

        try {
            servlet.onRequest(x);
            return 0;
        } catch (Throwable ex) {
            return x.close(ErrorCode(ServletException), "ServletException", ex);
        }
    }

    @Override
    protected long ProcessCloseExchangeRequest(Zeze.Builtin.Web.CloseExchange r) throws Throwable {
        Exchanges.remove(r.Argument.getExchangeId());
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessRequestInputStreamRequest(Zeze.Builtin.Web.RequestInputStream r) throws Throwable {
        var x = Exchanges.get(r.Argument.getExchangeId());
        if (null == x) {
            r.SendResultCode(ErrorCode(ExchangeIdNotFound));
            return 0;
        }

        var servlet = Servlets.get(x.request.Argument.getPath());
        if (null == servlet)
            return x.close(ErrorCode(UnknownPath404), "UnknownPath404", null);

        try {
            servlet.onUpload(x, r.Argument);
            r.SendResult();
        } catch (Throwable ex) {
            r.SendResultCode(ErrorCode(OnUploadException));
        }
        return 0;
    }

    private static final String CookieSessionName = "ZEZEWEBSESSIONID=";
    public String putSession(String account) {
        var sessionId = String.valueOf(AutoKey.nextId());
        _tSessions.getOrAdd(sessionId).setAccount(account);
        return CookieSessionName + sessionId;
    }

    public BSession getSession(List<String> cookie) {
        for (var c : cookie) {
            if (c.startsWith(CookieSessionName))
                return _tSessions.get(c.substring(CookieSessionName.length()));
        }
        return null;
    }
}
