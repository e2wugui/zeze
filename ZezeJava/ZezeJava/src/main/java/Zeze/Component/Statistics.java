package Zeze.Component;

import Zeze.Netty.HttpExchange;
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
		sb.append("    ").append(PerfCounter.TableInfo.getLogTitle()).append('\n');
		for (var t : PerfCounter.instance.getTableInfoMap())
			sb.append("    ").append(t).append("\n");

		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}
}
