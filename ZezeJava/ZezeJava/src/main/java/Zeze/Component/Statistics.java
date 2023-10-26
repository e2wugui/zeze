package Zeze.Component;

import Zeze.Netty.HttpExchange;
import Zeze.Transaction.TableStatistics;
import Zeze.Util.PerfCounter;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Statistics extends AbstractStatistics {
	@SuppressWarnings("RedundantThrows")
	@Override
	protected void OnServletQuery(HttpExchange x) throws Exception {
		var sb = new StringBuilder();

		sb.append("Procedures:\n");
		for (var p : PerfCounter.instance.getProcedureInfoMap().values())
			sb.append("    ").append(p).append("\n");

		sb.append("Tables:\n");
		for (var it = TableStatistics.getInstance().getTables().entryIterator(); it.moveToNext(); ) {
			sb.append("    ").append(it.key()).append("\n");
			it.value().buildString("        ", sb, "\n");
		}

		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}
}
