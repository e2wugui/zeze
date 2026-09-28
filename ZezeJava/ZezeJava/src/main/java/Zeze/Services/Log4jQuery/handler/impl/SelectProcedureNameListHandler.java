package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Util.ZezeCounter;

/**
 * procedure_name_list 命令：返回最近统计周期的过程名列表。
 */
@HandlerCmd("procedure_name_list")
public class SelectProcedureNameListHandler implements QueryHandler<Object, List<String>> {
	@Override
	public List<String> invoke(Object param) {
		return List.copyOf(ZezeCounter.instance.getLast().procedureResults().keySet());
	}
}
