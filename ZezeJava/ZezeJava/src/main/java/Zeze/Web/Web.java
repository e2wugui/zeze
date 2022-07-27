package Zeze.Web;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Arch.ProviderApp;
import Zeze.Net.Binary;

public class Web extends AbstractWeb {

    public final ProviderApp ProviderApp;
    public ConcurrentHashMap<String, HttpServlet> Handles = new ConcurrentHashMap<>();

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
    }

    @Override
    protected long ProcessAuthJsonRequest(Zeze.Builtin.Web.AuthJson r) {
        r.Result.setContentType("text/plain; charset=utf-8");
        r.Result.setBody(new Binary("Handle Not Found".getBytes(StandardCharsets.UTF_8)));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessAuthOkRequest(Zeze.Builtin.Web.AuthOk r) {
        return Zeze.Transaction.Procedure.NotImplement;
    }

    @Override
    protected long ProcessAuthQueryRequest(Zeze.Builtin.Web.AuthQuery r) {
        r.Result.setContentType("text/plain; charset=utf-8");
        r.Result.setBody(new Binary("hello world ProcessAuthQueryRequest".getBytes(StandardCharsets.UTF_8)));
        r.SendResult();
        return 0;
    }

    @Override
    protected long ProcessRequestJsonRequest(Zeze.Builtin.Web.RequestJson r) throws Throwable {
        var handle = Handles.get(r.Argument.getServletName());
        if (null == handle) {
            r.Result.setContentType("text/plain; charset=utf-8");
            r.Result.setBody(new Binary("Handle Not Found".getBytes(StandardCharsets.UTF_8)));
            r.SendResult();
            return 0;
        }
        try {
            handle.handle(r);
        } catch (Throwable ex) {
            try (var out = new ByteArrayOutputStream();
                 var ps = new PrintStream(out)) {
                ex.printStackTrace(ps);
                r.Result.setContentType("text/plain; charset=utf-8");
                r.Result.setBody(new Binary(out.toByteArray()));
                r.SendResult();
            }
        }
        return 0;
    }

    @Override
    protected long ProcessRequestQueryRequest(Zeze.Builtin.Web.RequestQuery r) throws Throwable {
        var handle = Handles.get(r.Argument.getServletName());
        if (null == handle) {
            r.Result.setContentType("text/plain; charset=utf-8");
            r.Result.setBody(new Binary("Handle Not Found".getBytes(StandardCharsets.UTF_8)));
            r.SendResult();
            return 0;
        }
        try {
            handle.handle(r);
        } catch (Throwable ex) {
            try (var out = new ByteArrayOutputStream();
                    var ps = new PrintStream(out)) {
                ex.printStackTrace(ps);
                r.Result.setContentType("text/plain; charset=utf-8");
                r.Result.setBody(new Binary(out.toByteArray()));
                r.SendResult();
            }
        }
        return 0;
    }
}
