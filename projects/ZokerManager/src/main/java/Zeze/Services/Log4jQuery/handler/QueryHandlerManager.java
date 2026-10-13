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

/**
 * 查询处理器管理器：扫描 impl 包按 @HandlerCmd 注册处理器，将 JSON 请求按 cmd 分发调用。
 */
public class QueryHandlerManager {
	private static final @NotNull Logger logger = LogManager.getLogger(QueryHandlerManager.class);
	private static final Map<String, QueryHandleContainer> handlerMap = new HashMap<>();

	static {
		// 类初始化锁保证仅执行一次且安全发布：懒初始化+非volatile标志在多客户端并发首查时
		// 会并发执行init、并发put同一个HashMap（丢条目/结构损坏），且initFinish不可见会反复init。
		init();
	}

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
		var queryRequest = Json.parse(req, QueryRequest.class);
		String cmd = queryRequest != null ? queryRequest.getCmd() : null;
		Object param = queryRequest != null ? queryRequest.getParam() : null;
		QueryHandleContainer queryHandler = handlerMap.get(cmd);
		// 未知/缺失cmd必须显式错误应答：静默空串与"合法空结果"不可区分（resultCode恒0），
		// 调用方无从感知请求畸形还是真无数据。
		if (queryHandler == null)
			return Json.toCompactString(Map.of("error", cmd == null ? "missing cmd" : "unknown cmd: " + cmd));
		return Json.toCompactString(queryHandler.invoke(param));
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
			// param通道按字符串契约承载：QueryRequest<T>泛型擦除为Object，客户端直接发JSON对象/数字时
			// JsonReader绑成HashMap/Long，直接强转(String)即CCE；非字符串形态先归一为JSON文本再cast。
			var str = o == null ? null : o instanceof String s ? s : Json.toCompactString(o);
			return ((QueryHandler<Object, Object>)queryHandler).invoke(cast(paramClass, str));
		}

		private static Object cast(@NotNull Class<?> clazz, String str) {
			if (clazz == String.class)
				return str;
			return switch (clazz.getName()) {
				case "int", "java.lang.Integer" -> Integer.valueOf(str);
				case "long", "java.lang.Long" -> Long.valueOf(str);
				case "byte", "java.lang.Byte" -> Byte.valueOf(str);
				case "short", "java.lang.Short" -> Short.valueOf(str);
				case "float", "java.lang.Float" -> Float.valueOf(str);
				case "double", "java.lang.Double" -> Double.valueOf(str);
				case "boolean", "java.lang.Boolean" -> Boolean.valueOf(str);
				case "char", "java.lang.Character" -> (char)Integer.parseInt(str);
				default -> Json.parse(str, clazz);
			};
		}
	}
}
