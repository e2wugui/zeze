package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Transaction.ProcedureStatistics;
@HandlerCmd("procedure_name_list")
public class SelectProcedureNameListHandler implements QueryHandler<Object, List<String>> {
		@Override
		public List<String> invoke(Object param) {
			List<String> list = ProcedureStatistics.getInstance().selectProcedureNameList();
			return list;
		}

	}
