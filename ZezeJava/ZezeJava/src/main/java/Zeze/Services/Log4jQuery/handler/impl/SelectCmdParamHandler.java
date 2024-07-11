package Zeze.Services.Log4jQuery.handler.impl;

import java.util.List;
import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;
import Zeze.Services.Log4jQuery.handler.entity.ClassInfo;
import Zeze.Services.Log4jQuery.handler.entity.SimpleField;

@HandlerCmd("cmd_param")
public class SelectCmdParamHandler implements QueryHandler<String, ClassInfo> {
		@Override
		public ClassInfo invoke(String param) {
			QueryHandlerManager.QueryHandleContainer queryHandleContainer = QueryHandlerManager.getQueryHandleContainer(param);
			ClassInfo classInfo = new ClassInfo();
			if (queryHandleContainer == null){
				return classInfo;
			}
			Class<?> paramClass = queryHandleContainer.getParamClass();
			classInfo.setClassName(paramClass.getName());
			if (paramClass.isAssignableFrom(Number.class) || paramClass == Boolean.class || paramClass == String.class){
				classInfo.setBaseType(true);
			}else {
				List<SimpleField> fields = queryHandleContainer.getFields();
				classInfo.setFields(fields);
			}
			return classInfo;
		}

	}
