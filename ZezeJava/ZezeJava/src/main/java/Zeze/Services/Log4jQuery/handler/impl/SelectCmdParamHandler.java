package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;
import Zeze.Services.Log4jQuery.handler.entity.ClazzInfo;
import Zeze.Services.Log4jQuery.handler.entity.SimpleField;

@HandlerCmd("cmd_param")
public class SelectCmdParamHandler implements QueryHandler<String, ClazzInfo> {
		@Override
		public ClazzInfo invoke(String param) {
			QueryHandlerManager.QueryHandleContainer queryHandleContainer = QueryHandlerManager.getQueryHandleContainer(param);
			ClazzInfo clazzInfo = new ClazzInfo();
			if (queryHandleContainer == null){
				return clazzInfo;
			}
			Class paramClass = queryHandleContainer.getParamClass();
			clazzInfo.setClazzName(paramClass.getName());
			if (paramClass.isAssignableFrom(Number.class) || paramClass == Boolean.class || paramClass == String.class){
				clazzInfo.setBaseType(true);
			}else {
				List<SimpleField> fields = queryHandleContainer.getFields();
				clazzInfo.setFields(fields);
			}
			return clazzInfo;
		}

	}
