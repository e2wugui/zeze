package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Util.PerfCounter;
import Zeze.Util.ZezeCounter;

@HandlerCmd("procedure_name_list")
public class SelectProcedureNameListHandler implements QueryHandler<Object, List<String>> {
	@Override
	public List<String> invoke(Object param) {
		var counter = ZezeCounter.instance;
		return counter instanceof PerfCounter
				? List.copyOf(((PerfCounter)counter).getProcedureInfoMap().keySet())
				: List.of();
	}
}
