package Zeze.Component;

import Zeze.Netty.HttpExchange;
import Zeze.Util.ZezeCounter;
import io.netty.handler.codec.http.HttpResponseStatus;

public class Statistics extends AbstractStatistics {
	@SuppressWarnings("RedundantThrows")
	@Override
	protected void OnServletQuery(HttpExchange x) throws Exception {
		var sb = new StringBuilder();
		var snap = ZezeCounter.instance.getLast();

		sb.append("Procedures:\n");
		snap.procedureResults().forEach((name, results) -> {
			long total = 0;
			long succ = 0;
			for (var e : results.entrySet()) {
				total += e.getValue();
				if (e.getKey() == 0)
					succ = e.getValue();
			}
			sb.append("    ").append(name).append(':')
					.append(total != 0 ? succ * 100 / total : 0).append('%');
			results.forEach((rc, count) -> sb.append(", ").append(rc).append(':').append(count));
			sb.append('\n');
		});

		sb.append("Tables:\n");
		snap.tableResults().forEach((name, metrics) -> {
			sb.append("    ").append(name);
			metrics.forEach((metric, count) -> {
				if (count != 0)
					sb.append(' ').append(metric).append('=').append(count);
			});
			sb.append('\n');
		});

		x.sendPlainText(HttpResponseStatus.OK, sb.toString());
	}
}
