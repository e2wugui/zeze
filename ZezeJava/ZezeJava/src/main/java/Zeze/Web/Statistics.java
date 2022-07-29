package Zeze.Web;

import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.TableStatistics;

public class Statistics {
	public Statistics(Web web) {
		web.Servlets.put("/zeze/auth", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void onRequest(HttpExchange r) {
				Statistics.auth(r);
			}
		});
		web.Servlets.put("/zeze/stats", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void onRequest(HttpExchange r) {
				Statistics.handle(r);
			}
		});
	}

	public static void auth(HttpExchange r) {
		// 默认实现并不验证密码，只是把参数account的值保存到会话中。
		// 当应用需要实现Auth时，继承这个类，并重载auth方法。
		var ss = r.web.getSession(r.getRequestHeaders().get("").getValues());
		var query = HttpService.parseQuery(r.getRequest().Argument.getQuery());
		var account = query.get("account");
		r.putResponseHeader("", r.web.putSession(account));
		r.sendResponseHeaders(200, new byte[0],true);
	}

	public static void handle(HttpExchange r) {
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

		r.sendTextResult(sb.toString());
	}
}
