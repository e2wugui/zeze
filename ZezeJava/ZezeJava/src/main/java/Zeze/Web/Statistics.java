package Zeze.Web;

import Zeze.Builtin.Web.BStream;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.TableStatistics;

public class Statistics {
	public Statistics(Web web) {
		web.Servlets.put("/zeze/auth", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void onRequest(HttpExchange r) {
				// 默认实现并不验证密码，只是把参数account的值保存到会话中。
				// 当应用需要实现Auth时，继承这个类，并重载auth方法。
				var ss = r.web.getSession(r.getRequestCookie());
				var query = HttpService.parseQuery(r.getRequest().getQuery());
				var account = query.get("account");
				r.setResponseCookie(r.web.putSession(account));
				r.sendTextResponse("auth ok!");
			}
		});

		web.Servlets.put("/zeze/stats", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void onRequest(HttpExchange r) {
				var sb = new StringBuilder();

				sb.append("Procedures:\n");
				for (var p : ProcedureStatistics.getInstance().getProcedures().entrySet()) {
					sb.append("    ").append(p.getKey()).append("\n");
					p.getValue().buildString("        ", sb, "\n");
				}

				sb.append("Tables:\n");
				for (var it = TableStatistics.getInstance().getTables().entryIterator(); it.moveToNext(); ) {
					sb.append("    ").append(it.key()).append("\n");
					it.value().buildString("        ", sb, "\n");
				}

				r.sendTextResponse(sb.toString());
			}
		});

		web.Servlets.put("/zeze/echo", new HttpServlet() {
			@Override
			public void onRequest(HttpExchange r) throws Throwable {
				var ctype = "Content-Type";
				r.setResponseHeader(ctype, r.getRequestHeader(ctype));
				r.sendResponseHeaders(200, r.getRequest().getBody().InternalGetBytesUnsafe(), r.getRequest().isFinish());
			}

			@Override
			public void onUpload(HttpExchange r, BStream s) throws Throwable {
				r.sendResponseBody(s.getBody().InternalGetBytesUnsafe(), s.isFinish());
			}
		});
	}
}
