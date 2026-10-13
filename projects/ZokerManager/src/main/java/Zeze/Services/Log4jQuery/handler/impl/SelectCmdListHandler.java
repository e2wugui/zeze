package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;

/**
 * cmd_list 命令：返回全部已注册查询命令名列表。
 */
@HandlerCmd("cmd_list")
public class SelectCmdListHandler implements QueryHandler<Object, List<String>> {
	@Override
	public List<String> invoke(Object param) {
		return QueryHandlerManager.selectCmdList();
	}
}
