package Zeze.Services.Log4jQuery.handler.impl;

import java.util.ArrayList;
import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Util.PerfCounter;

@HandlerCmd("procedure_results")
public class SelectProcedureResultsHandler implements QueryHandler<String, List<SelectProcedureResultsHandler.ResultLog>> {
	public static final class ResultLog {
		private long result;
		private long sum;

		public ResultLog() {
		}

		public ResultLog(long result, long sum) {
			this.result = result;
			this.sum = sum;
		}

		public long getResult() {
			return result;
		}

		public void setResult(long result) {
			this.result = result;
		}

		public long getSum() {
			return sum;
		}

		public void setSum(long sum) {
			this.sum = sum;
		}
	}

	@Override
	public List<ResultLog> invoke(String param) {
		var pInfo = PerfCounter.instance.getProcedureInfo(param);
		if (pInfo == null)
			return List.of();
		var resultLogs = new ArrayList<ResultLog>();
		for (var it = pInfo.getResultMapLast().entryIterator(); it.moveToNext(); )
			resultLogs.add(new ResultLog(it.key(), it.value().sum()));
		return resultLogs;
	}
}
