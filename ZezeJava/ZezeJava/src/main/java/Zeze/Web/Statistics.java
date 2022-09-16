package Zeze.Web;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Web.BStream;
import Zeze.Net.Binary;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.TableStatistics;

public class Statistics {
	public Statistics(Web web) {
		web.servlets.put("/zeze/auth", new HttpServlet() {
			// 只实现query参数模式。
			@Override
			public void onRequest(HttpExchange r) {
				// 默认实现并不验证密码，只是把参数account的值保存到会话中。
				// 当应用需要实现Auth时，继承这个类，并重载auth方法。
				// var ss = r.getWeb().getSession(r.getRequestCookie());
				var query = HttpService.parseQuery(r.getRequest().getQuery());
				var account = query.get("account");
				r.setResponseCookie(r.getWeb().putSession(account));
				r.sendTextResponse("auth ok!");
			}
		});

		web.servlets.put("/zeze/stats", new HttpServlet() {
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

		web.servlets.put("/zeze/echo", new HttpServlet() {
			// 由于HttpClient发送完Request前不会读取Response，所以这里没法实现成边读边写的模式。
			// 现在上传数据保存在HttpServlet中，所以这个请求不能并发。
			final List<Binary> uploadData = new ArrayList<>();
			int downloadIndex = 0;
			MessageDigest md5;

			@Override
			public void onRequest(HttpExchange r) throws Throwable {
				var ctype = "Content-Type";
				r.setResponseHeader(ctype, r.getRequestHeader(ctype));
				var data = r.getRequest().getBody();
				r.sendResponseHeaders(200, data.bytesUnsafe(), r.getRequest().isFinish());
				md5 = MessageDigest.getInstance("md5");
				md5.update(data.bytesUnsafe());
				HttpExchange.logger.info("new echo: " + r.getRequest().getExchangeId());
			}

			@Override
			public void onUpload(HttpExchange r, BStream s) {
				md5.update(s.getBody().bytesUnsafe());
				uploadData.add(s.getBody());
				if (s.isFinish()) {
					//HttpExchange.logger.info(Zeze.Util.BitConverter.toString(md5.digest()), new Exception());
					if (uploadData.size() > downloadIndex) {
						var data = uploadData.get(downloadIndex++);
						r.sendResponseBodyAsync(data.bytesUnsafe(),
								uploadData.size() == downloadIndex, this::processResponseResult);
					}
				}
			}

			private long processResponseResult(HttpExchange r, long rc) {
				if (rc == 0 && downloadIndex < uploadData.size()) {
					var data = uploadData.get(downloadIndex++);
					r.sendResponseBodyAsync(data.bytesUnsafe(),
							uploadData.size() == downloadIndex, this::processResponseResult);
					return 0;
				}
				uploadData.clear();
				downloadIndex = 0;
				return rc;
			}
		});
	}
}
