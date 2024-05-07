package Zeze.History;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import Zeze.Application;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogOne;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.Log;
import Zeze.Transaction.LogDynamic;
import Zeze.Transaction.Logs.LogBeanKey;
import Zeze.Transaction.Logs.LogBinary;
import Zeze.Transaction.Logs.LogBool;
import Zeze.Transaction.Logs.LogByte;
import Zeze.Transaction.Logs.LogDecimal;
import Zeze.Transaction.Logs.LogDouble;
import Zeze.Transaction.Logs.LogFloat;
import Zeze.Transaction.Logs.LogInt;
import Zeze.Transaction.Logs.LogLong;
import Zeze.Transaction.Logs.LogQuaternion;
import Zeze.Transaction.Logs.LogShort;
import Zeze.Transaction.Logs.LogString;
import Zeze.Transaction.Logs.LogVector2;
import Zeze.Transaction.Logs.LogVector2Int;
import Zeze.Transaction.Logs.LogVector3;
import Zeze.Transaction.Logs.LogVector3Int;
import Zeze.Transaction.Logs.LogVector4;
import Zeze.Util.KV;
import org.jetbrains.annotations.NotNull;

public class Helper {
	public static class DependsResult {
		public final HashSet<Class<?>> allBeans = new HashSet<>();

		public final HashSet<Class<? extends Bean>> beans = new HashSet<>();
		public final HashSet<Class<? extends Serializable>> beanKeys = new HashSet<>();
		public final HashSet<Class<?>> list1 = new HashSet<>();
		public final HashSet<Class<? extends Bean>> list2 = new HashSet<>();
		public final HashSet<KV<Class<?>, Class<?>>> map1 = new HashSet<>();
		public final HashSet<KV<Class<?>, Class<? extends Bean>>> map2 = new HashSet<>();
		public final HashMap<KV<Class<?>, Class<? extends Bean>>,
				KV<ToLongFunction<Bean>, LongFunction<Bean>>> map2Dynamic = new HashMap<>();
		public final HashSet<Class<?>> set1 = new HashSet<>();
	}

	public static void registerAllTableLogs(Application zeze) throws Exception {
		var result = new DependsResult();
		for (var db : zeze.getDatabases().values()) {
			for (var table : db.getTables()) {
				var keyClass = table.getKeyClass();
				if (Serializable.class.isAssignableFrom(keyClass)) {
					// must be BeanKey.
					dependsBean(keyClass, result);
				}
				var valueClass = table.getValueClass();
				dependsBean(valueClass, result);
			}
		}
		for (var beanClass : result.beans)
			registerLogOne(beanClass); // 没做为其他Bean的变量时是不需要注册的。这里区分了。
		for (var beanKeyClass : result.beanKeys)
			registerLogBeanKey(beanKeyClass);
		for (var list1Class : result.list1)
			registerLogList1(list1Class);
		for (var list2Class : result.list2)
			registerLogList2(list2Class);
		for (var map1KV : result.map1)
			registerLogMap1(map1KV.getKey(), map1KV.getValue());
		for (var map2KV : result.map2)
			registerLogMap2(map2KV.getKey(), map2KV.getValue());
		for (var e : result.map2Dynamic.entrySet())
			registerLogMap2Dynamic(e.getKey().getKey(), e.getValue().getKey(), e.getValue().getValue());
		for (var set1Class : result.set1)
			registerLogSet1(set1Class);
	}

	@SuppressWarnings("unchecked")
	public static void dependsBean(Class<?> beanClass, DependsResult result) throws Exception {
		if (result.allBeans.add(beanClass)) {
			var obj = beanClass.getConstructor().newInstance();
			if (obj instanceof BeanKey) {
				result.beanKeys.add((Class<? extends Serializable>)beanClass);
				var beanKey = (BeanKey)obj;
				for (var v : beanKey.variables()) {
					var type = v.getType();
					if (!isBuiltinType(type))
						dependsBean(Class.forName(type), result);
				}
			} else if (obj instanceof Bean) {
				result.beans.add((Class<? extends Bean>)beanClass);
				var bean = (Bean)obj;
				for (var v : bean.variables()) {
					var type = v.getType();
					switch (type) {
					case "list":
					case "array":
						dependsList(v.getValue(), result);
						break;
					case "map":
						dependsMap(beanClass, v, v.getKey(), v.getValue(), result);
						break;
					case "set":
						dependsSet(v.getValue(), result);
						break;
					default:
						if (!isBuiltinType(type))
							dependsBean(Class.forName(type), result);
						break;
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsList(String valueType, DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (null != valueClass) {
			result.list1.add(valueClass);
			return;
		}
		valueClass = Class.forName(valueType);
		dependsBean(valueClass, result);
		result.list2.add((Class<? extends Bean>)valueClass);
	}

	@SuppressWarnings("unchecked")
	public static void dependsMap(Class<?> beanClass, BVariable.Data v,
								  String keyType, String valueType,
								  DependsResult result) throws Exception {
		var keyClass = getBuiltinBoxingClass(keyType);
		if (null == keyClass) {
			keyClass = Class.forName(keyType); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		var is2 = null == valueClass;
		if (is2) {
			valueClass = Class.forName(valueType); // bean or beanKey
			dependsBean(valueClass, result);
			result.map2.add(KV.create(keyClass, (Class<? extends Bean>)valueClass));
		} else if (valueClass == DynamicBean.class) {
			result.map2Dynamic.computeIfAbsent(KV.create(keyClass, (Class<? extends Bean>)valueClass), (key) -> {
				try {
					var db = (DynamicBean)beanClass.getMethod("newDynamicBean_"
							+ Character.toUpperCase(v.getName().charAt(0))
							+ v.getName().substring(1)).invoke(null);
					return KV.create(db.getGetBean(), db.getCreateBean());
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			});
		} else {
			result.map1.add(KV.create(keyClass, valueClass));
		}
	}

	public static void dependsSet(String valueType, DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (null == valueClass) {
			valueClass = Class.forName(valueType); // must be beanKey
			dependsBean(valueClass, result);
		}
		result.set1.add(valueClass);
	}

	public static Class<?> getBuiltinBoxingClass(String type) {
		switch (type) {
		//@formatter:off
		case "binary": return Zeze.Net.Binary.class;
		case "bool": return Boolean.class;
		case "byte": return Byte.class;
		case "decimal": return BigDecimal.class;
		case "double": return Double.class;
		case "float": return Float.class;
		case "int": return Integer.class;
		case "long": return Long.class;
		case "quaternion": return Zeze.Serialize.Quaternion.class;
		case "short": return Short.class;
		case "string": return String.class;
		case "vector2": return Zeze.Serialize.Vector2.class;
		case "vector2int": return Zeze.Serialize.Vector2Int.class;
		case "vector3": return Zeze.Serialize.Vector3.class;
		case "vector3int": return Zeze.Serialize.Vector3Int.class;
		case "vector4": return Zeze.Serialize.Vector4.class;
		case "dynamic": return Zeze.Transaction.DynamicBean.class;
		//@formatter:on
		}
		return null;
	}

	public static boolean isBuiltinType(String type) {
		return getBuiltinBoxingClass(type) != null;
	}

	public static <T extends Serializable> void registerLogBeanKey(@NotNull Class<T> beanClass) {
		Log.register(() -> new LogBeanKey<>(beanClass));
	}

	public static <T> void registerLogList1(@NotNull Class<T> valueClass) {
		Log.register(() -> new LogList1<>(valueClass));
	}

	public static <V extends Bean> void registerLogList2(@NotNull Class<V> beanClass) {
		Log.register(() -> new LogList2<>(beanClass));
	}

	public static <K, V> void registerLogMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		Log.register(() -> new LogMap1<>(keyClass, valueClass));
	}

	public static <K, V extends Bean> void registerLogMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		Log.register(() -> new LogMap2<>(keyClass, valueClass));
	}

	public static <K, V extends Bean> void registerLogMap2Dynamic(
			@NotNull Class<K> keyClass,
			@NotNull ToLongFunction<Bean> get,
			@NotNull LongFunction<Bean> create) {
		Log.register(() -> new LogMap2<>(keyClass, get, create));
	}

	public static <V> void registerLogSet1(@NotNull Class<V> valueClass) {
		Log.register(() -> new LogSet1<>(valueClass));
	}

	public static <V extends Bean> void registerLogOne(@NotNull Class<V> beanClass) {
		Log.register(() -> new LogOne<>(beanClass));
	}

	public static void registerLogs() {
		Log.register(LogBinary::new);
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogDecimal::new);
		Log.register(LogDouble::new);
		Log.register(LogFloat::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogQuaternion::new);
		Log.register(LogShort::new);
		Log.register(LogString::new);
		Log.register(LogVector2::new);
		Log.register(LogVector2Int::new);
		Log.register(LogVector3::new);
		Log.register(LogVector3Int::new);
		Log.register(LogVector4::new);
		Log.register(LogDynamic::new);
		Log.register(LogBean::new);
	}

}
