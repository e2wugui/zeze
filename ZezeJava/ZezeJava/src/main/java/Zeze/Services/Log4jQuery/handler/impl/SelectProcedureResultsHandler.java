package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Transaction.ProcedureStatistics;

@HandlerCmd("procedure_results")
public class SelectProcedureResultsHandler implements QueryHandler<String, List<ProcedureStatistics.ResultLog>> {
		@Override
		public List<ProcedureStatistics.ResultLog> invoke(String param) {
			List<ProcedureStatistics.ResultLog> resultLogs = ProcedureStatistics.getInstance().selectProcedureResults(param);
			return resultLogs;
		}
	}
