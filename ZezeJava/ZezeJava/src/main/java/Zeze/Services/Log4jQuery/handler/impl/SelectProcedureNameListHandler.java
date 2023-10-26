package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Util.PerfCounter;

@HandlerCmd("procedure_name_list")
public class SelectProcedureNameListHandler implements QueryHandler<Object, List<String>> {
	@Override
	public List<String> invoke(Object param) {
		return PerfCounter.instance.getProcedureInfoMap().keySet().stream().toList();
	}
}
