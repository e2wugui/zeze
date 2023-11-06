package Zeze.Services.Log4jQuery.handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Zeze.Services.Log4jQuery.handler.entity.SimpleField;
import com.alibaba.fastjson.JSONObject;

public class QueryHandlerManager{

	private static Map<String, QueryHandleContainer> handlerMap = new HashMap<>();
	private static boolean initFinish = false;

	public static void init(){
		List<String> clazzNames = ClazzUtils.getClazzName("Zeze.Services.Log4jQuery.handler.impl", true);
		clazzNames.forEach(clazz ->{
			try {
				Class<?> handlerClass = Class.forName(clazz);
				HandlerCmd handlerCmd = handlerClass.getAnnotation(HandlerCmd.class);
				if (handlerCmd != null){
					String cmd = handlerCmd.value();
					QueryHandleContainer queryHandleContainer = new QueryHandleContainer((QueryHandler)handlerClass.getConstructor(null).newInstance(null));
					handlerMap.put(cmd, queryHandleContainer);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		});


	}


	public static String invokeHandler(String req) throws ClassNotFoundException {
		if (!initFinish){
			init();
			initFinish = true;
		}
		QueryRequest queryRequest = JSONObject.parseObject(req, QueryRequest.class);
		Object param = queryRequest.getParam();
		String cmd = queryRequest.getCmd();
		QueryHandleContainer queryHandler = handlerMap.get(cmd);
		if (queryHandler == null){
			return "";
		}

		Object obj = queryHandler.invoke(param);
		String result = JSONObject.toJSONString(obj);
		return result;
	}


	public static List<String> selectCmdList(){
		List<String> list = new ArrayList<>(handlerMap.keySet());
		return list;
	}

	public static QueryHandleContainer getQueryHandleContainer(String cmd){
		return handlerMap.get(cmd);
	}


	public static class QueryHandleContainer{
		private QueryHandler queryHandler;
		private Class paramClass;
		private List<SimpleField> fields = new ArrayList<>();


		public QueryHandleContainer(QueryHandler queryHandler){
			this.queryHandler = queryHandler;
			Method[] declaredMethods = queryHandler.getClass().getDeclaredMethods();
			for (Method method : declaredMethods){
				if (method.getName() == "invoke"){
					Class<?> parameterType = method.getParameterTypes()[0];
					if (parameterType != Object.class){
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

		public Object invoke(Object o){
			if (paramClass == null || paramClass == Object.class){
				return queryHandler.invoke(null);
			}

			return queryHandler.invoke(cast(paramClass, (String)o));

		}

		private Object cast(Class<?> clazz, String str)  {
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
			case "string":
			case "String":
			case "java.lang.String":
				return str;
			case "float":
			case "java.lang.Float":
				return Float.valueOf(str);
			case "double":
			case "java.lang.Double":
				return Double.valueOf(str);
			case "boolean":
			case "java.lang.Boolean":
				return Boolean.valueOf(str);
			default:
				return JSONObject.parseObject(str, clazz);

			}

		}
	}


}
