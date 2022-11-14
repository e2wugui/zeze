package Zeze.Component;

import Zeze.Netty.HttpExchange;
import Zeze.Transaction.ProcedureStatistics;
import Zeze.Transaction.TableStatistics;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Statistics extends AbstractStatistics {
	@Override
	protected void OnServletQuery(HttpExchange x) throws Exception {
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

		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}
}
