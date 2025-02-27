package Zeze.Component;

import Zeze.Netty.HttpExchange;
import Zeze.Util.PerfCounter;
import Zeze.Util.ZezeCounter;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Statistics extends AbstractStatistics {
	@SuppressWarnings("RedundantThrows")
	@Override
	protected void OnServletQuery(HttpExchange x) throws Exception {
		var sb = new StringBuilder();

		var counter = ZezeCounter.instance;
		if (counter instanceof PerfCounter) {
			var perfCounter = (PerfCounter)counter;
			sb.append("Procedures:\n");
			for (var p : perfCounter.getProcedureInfoMap().values())
				sb.append("    ").append(p).append("\n");

			sb.append("Tables:\n");
			if (!perfCounter.getTableInfoMap().isEmpty())
				sb.append("    ").append(PerfCounter.TableInfo.getLogTitle()).append('\n');
			for (var t : perfCounter.getTableInfoMap())
				sb.append("    ").append(t).append("\n");
		}

		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}
}
