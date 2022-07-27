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
    public ConcurrentHashMap<String, HttpServlet> Servlets = new ConcurrentHashMap<>();
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
    protected long ProcessRequestJsonRequest(Zeze.Builtin.Web.RequestJson r) throws Throwable {
        var servlet = Servlets.get(r.Argument.getServletName());
        if (null == servlet) {
            r.Result.setContentType("text/plain; charset=utf-8");
            r.Result.setBody(new Binary("Servlet Not Found.".getBytes(StandardCharsets.UTF_8)));
            r.SendResult();
            return 0;
        }
        try {
            servlet.handle(this, r);
        } catch (Throwable ex) {
            try (var out = new ByteArrayOutputStream();
                 var ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
                ex.printStackTrace(ps);
                r.Result.setContentType("text/plain; charset=utf-8");
                r.Result.setBody(new Binary(out.toByteArray()));
                r.SendResult();
            }
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

    @Override
    protected long ProcessRequestQueryRequest(Zeze.Builtin.Web.RequestQuery r) throws Throwable {
        var servlet = Servlets.get(r.Argument.getServletName());
        if (null == servlet) {
            r.Result.setContentType("text/plain; charset=utf-8");
            r.Result.setBody(new Binary("Servlet Not Found.".getBytes(StandardCharsets.UTF_8)));
            r.SendResult();
            return 0;
        }
        try {
            servlet.handle(this, r);
        } catch (Throwable ex) {
            try (var out = new ByteArrayOutputStream();
                    var ps = new PrintStream(out, false, StandardCharsets.UTF_8)) {
                ex.printStackTrace(ps);
                r.Result.setContentType("text/plain; charset=utf-8");
                r.Result.setBody(new Binary(out.toByteArray()));
                r.SendResult();
            }
        }
        return 0;
    }
}
