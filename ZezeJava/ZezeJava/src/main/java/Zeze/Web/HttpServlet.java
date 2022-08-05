package Zeze.Web;

import java.nio.charset.StandardCharsets;
import Zeze.Builtin.Web.BStream;

@SuppressWarnings("RedundantThrows")
public abstract class HttpServlet {
	public void onRequest(HttpExchange r) throws Throwable {
		r.setResponseHeader("Content-Type", "text/plain; charset=utf-8");
		r.sendResponseHeaders(501, "Not Implemented".getBytes(StandardCharsets.UTF_8), true);
	}

	public void onUpload(HttpExchange r, BStream s) throws Throwable {
	}
}
