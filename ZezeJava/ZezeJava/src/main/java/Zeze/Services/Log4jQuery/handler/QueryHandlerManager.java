package Zeze.Services.Log4jQuery.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Zeze.Services.Log4jQuery.handler.entity.SimpleField;
import Zeze.Util.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class QueryHandlerManager {
	private static final @NotNull Logger logger = LogManager.getLogger(QueryHandlerManager.class);
	private static final Map<String, QueryHandleContainer> handlerMap = new HashMap<>();
	private static boolean initFinish = false;

	public static void init() {
		var classNames = ClassUtils.getClassNames("Zeze.Services.Log4jQuery.handler.impl", true);
		classNames.forEach(clazz -> {
			try {
				var handlerClass = Class.forName(clazz);
				var handlerCmd = handlerClass.getAnnotation(HandlerCmd.class);
				if (handlerCmd != null) {
					var instance = (QueryHandler<?, ?>)handlerClass.getConstructor((Class<?>[])null)
							.newInstance((Object[])null);
					handlerMap.put(handlerCmd.value(), new QueryHandleContainer(instance));
				}
			} catch (Exception e) {
				logger.error("init exception:", e);
			}
		});
	}

	public static @NotNull String invokeHandler(@NotNull String req) throws ReflectiveOperationException {
		if (!initFinish) {
			init();
			initFinish = true;
		}
		var queryRequest = Json.parse(req, QueryRequest.class);
		String cmd = queryRequest != null ? queryRequest.getCmd() : null;
		Object param = queryRequest != null ? queryRequest.getParam() : null;
		QueryHandleContainer queryHandler = handlerMap.get(cmd);
		return queryHandler != null ? Json.toCompactString(queryHandler.invoke(param)) : "";
	}

	public static List<String> selectCmdList() {
		return new ArrayList<>(handlerMap.keySet());
	}

	public static QueryHandleContainer getQueryHandleContainer(String cmd) {
		return handlerMap.get(cmd);
	}

	public static class QueryHandleContainer {
		private final @NotNull QueryHandler<?, ?> queryHandler;
		private Class<?> paramClass;
		private final List<SimpleField> fields = new ArrayList<>();

		public QueryHandleContainer(@NotNull QueryHandler<?, ?> queryHandler) {
			this.queryHandler = queryHandler;
			for (var method : queryHandler.getClass().getDeclaredMethods()) {
				if (method.getName().equals("invoke")) {
					var paramClass = method.getParameterTypes()[0];
					if (paramClass != Object.class) {
						this.paramClass = paramClass;
						for (var field : paramClass.getDeclaredFields())
							fields.add(new SimpleField(field.getName(), field.getType().getName()));
					}
				}
			}
		}

		public Class<?> getParamClass() {
			return paramClass;
		}

		public @NotNull List<SimpleField> getFields() {
			return fields;
		}

		@SuppressWarnings("unchecked")
		public Object invoke(Object o) throws ReflectiveOperationException {
			if (paramClass == null || paramClass == Object.class)
				return queryHandler.invoke(null);
			return ((QueryHandler<Object, Object>)queryHandler).invoke(cast(paramClass, (String)o));
		}

		private static Object cast(@NotNull Class<?> clazz, String str) throws ReflectiveOperationException {
			if (clazz == String.class)
				return str;
			switch (clazz.getName()) {
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
				return Json.parse(str, clazz);
			}
		}
	}
}
