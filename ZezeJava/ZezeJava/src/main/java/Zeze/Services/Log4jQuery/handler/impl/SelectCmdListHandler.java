package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;

@HandlerCmd("cmd_list")
public class SelectCmdListHandler implements QueryHandler<Object, List<String>> {
		@Override
		public List<String> invoke(Object param) {
			List<String> list = QueryHandlerManager.selectCmdList();
			return list;
		}

	}
