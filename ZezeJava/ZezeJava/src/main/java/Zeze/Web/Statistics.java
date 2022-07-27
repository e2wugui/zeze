package Zeze.Web;

import java.nio.charset.StandardCharsets;
import Zeze.Builtin.Web.RequestQuery;
import Zeze.Net.Binary;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.Table;
import Zeze.Transaction.TableStatistics;

public class Statistics {
	public Statistics(Web web) {
		web.Servlets.put("/zeze/auth", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void handle(Web web, RequestQuery r) throws Throwable {
				Statistics.this.auth(web, r);
			}
		});
		web.Servlets.put("/zeze/stats", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void handle(Web web, RequestQuery r) throws Throwable {
				Statistics.this.handle(web, r);
			}
		});
	}

	public void auth(Web web, RequestQuery r) {
		// 默认实现并不验证密码，只是把参数accout的值保存到会话中。
		// 当应用需要实现Auth时，继承这个类，并重载auth方法。
		var ss = web.getSession(r.Argument.getCookie());
		var account = r.Argument.getQuery().get("account");
		r.Result.getCookie().add(web.putSession(account));
		r.SendResult();
	}

	public void handle(Web web, RequestQuery r) {
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

		r.Result.setContentType("text/plain; charset=utf-8");
		r.Result.setBody(new Binary(sb.toString().getBytes(StandardCharsets.UTF_8)));
		r.SendResult();
	}
}
