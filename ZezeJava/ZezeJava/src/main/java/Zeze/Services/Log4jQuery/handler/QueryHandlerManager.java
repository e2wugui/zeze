package Zeze.Services.Log4jQuery.handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Zeze.Services.Log4jQuery.handler.entity.SimpleField;
import com.alibaba.fastjson.JSONObject;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryHandlerManager {
	private static final Logger logger = LogManager.getLogger(QueryHandlerManager.class);

	private static final Map<String, QueryHandleContainer> handlerMap = new HashMap<>();
	private static boolean initFinish = false;

	public static void init() {
		List<String> clazzNames = ClazzUtils.getClazzName("Zeze.Services.Log4jQuery.handler.impl", true);
		clazzNames.forEach(clazz -> {
			try {
				Class<?> handlerClass = Class.forName(clazz);
				HandlerCmd handlerCmd = handlerClass.getAnnotation(HandlerCmd.class);
				if (handlerCmd != null) {
					String cmd = handlerCmd.value();
					var instance = (QueryHandler<?, ?>)handlerClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
					QueryHandleContainer queryHandleContainer = new QueryHandleContainer(instance);
					handlerMap.put(cmd, queryHandleContainer);
				}
			} catch (Exception e) {
				logger.error("", e);
			}
		});
	}

	public static String invokeHandler(String req) throws ReflectiveOperationException {
		if (!initFinish) {
			init();
			initFinish = true;
		}
		var queryRequest = JsonReader.local().buf(req).parse(QueryRequest.class);
		Object param = queryRequest != null ? queryRequest.getParam() : null;
		String cmd = queryRequest != null ? queryRequest.getCmd() : null;
		QueryHandleContainer queryHandler = handlerMap.get(cmd);
		if (queryHandler == null) {
			return "";
		}

		Object obj = queryHandler.invoke(param);
		return JsonWriter.local().clear().write(obj).toString();
	}

	public static List<String> selectCmdList() {
		return new ArrayList<>(handlerMap.keySet());
	}


	public static QueryHandleContainer getQueryHandleContainer(String cmd){
		return handlerMap.get(cmd);
	}


	public static class QueryHandleContainer {
		private final QueryHandler<?, ?> queryHandler;
		private Class<?> paramClass;
		private List<SimpleField> fields = new ArrayList<>();
		public QueryHandleContainer(QueryHandler<?, ?> queryHandler) {
			this.queryHandler = queryHandler;
			Method[] declaredMethods = queryHandler.getClass().getDeclaredMethods();
			for (Method method : declaredMethods) {
				if (method.getName().equals("invoke")) {
					Class<?> parameterType = method.getParameterTypes()[0];
					if (parameterType != Object.class) {
						paramClass = parameterType;
						Field[] paramFields = paramClass.getDeclaredFields();
						for (Field field : paramFields) {
							String name = field.getName();
							Class<?> type = field.getType();
							fields.add(new SimpleField(name, type.getName()));
						}
					}
				}
			}

		}

		public Class getParamClass() {
			return paramClass;
		}

		public List<SimpleField> getFields() {
			return fields;
		}



		@SuppressWarnings("unchecked")
		public Object invoke(Object o) throws ReflectiveOperationException {
			if (paramClass == null || paramClass == Object.class) {
				return queryHandler.invoke(null);
			}
			return ((QueryHandler<Object, Object>)queryHandler).invoke(cast(paramClass, (String)o));
		}

		private static Object cast(Class<?> clazz, String str) throws ReflectiveOperationException {
			if (clazz == String.class) {
				return str;
			}
			String className = clazz.getName();
			switch (className) {
			case "int":
			case "java.lang.Integer":
				return Integer.valueOf(str);
			case "long":
			case "java.lang.Long":
				return Long.valueOf(str);
			case "byte":
			case "java.lang.Byte":
				return Byte.valueOf(str);
			case "short":
			case "java.lang.Short":
				return Short.valueOf(str);
			case "float":
			case "java.lang.Float":
				return Float.valueOf(str);
			case "double":
			case "java.lang.Double":
				return Double.valueOf(str);
			case "boolean":
			case "java.lang.Boolean":
				return Boolean.valueOf(str);
			case "char":
			case "java.lang.Character":
				return (char)Integer.parseInt(str);
			default:
				return JsonReader.local().buf(str).parse(clazz);
			}
		}
	}
}
