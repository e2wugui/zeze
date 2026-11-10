package Zeze.History;

import java.math.BigDecimal;
import java.util.HashSet;

import Zeze.Application;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.BeanKey;
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogOneValue;
import Zeze.Transaction.Collections.LogOneMeta;
import Zeze.Transaction.Collections.LogBean;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.LogSortedMap2;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.Collections.Set1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Collections.SortedMap2Meta;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.LogRegistry;
import Zeze.Transaction.LogDynamicValue;
import Zeze.Transaction.Logs.LogBeanKeyValue;
import Zeze.Transaction.Logs.LogBinaryValue;
import Zeze.Transaction.Logs.LogBoolValue;
import Zeze.Transaction.Logs.LogByteValue;
import Zeze.Transaction.Logs.LogDecimalValue;
import Zeze.Transaction.Logs.LogDoubleValue;
import Zeze.Transaction.Logs.LogFloatValue;
import Zeze.Transaction.Logs.LogIntValue;
import Zeze.Transaction.Logs.LogLongValue;
import Zeze.Transaction.Logs.LogQuaternionValue;
import Zeze.Transaction.Logs.LogShortValue;
import Zeze.Transaction.Logs.LogStringValue;
import Zeze.Transaction.Logs.LogVector2Value;
import Zeze.Transaction.Logs.LogVector2IntValue;
import Zeze.Transaction.Logs.LogVector3Value;
import Zeze.Transaction.Logs.LogVector3IntValue;
import Zeze.Transaction.Logs.LogVector4Value;
import Zeze.Transaction.Table;
import Zeze.Util.KVNotNull;
import org.jspecify.annotations.Nullable;

/**
 * 回放端日志注册助手：扫描表（启动期全部表，或后启开的单表）的键值依赖，
 * 注册各集合/变量类型 Log 的解码工厂。
 */
public class Helper {
	static void registerRuntimeBeanLogs(Iterable<Class<? extends Bean>> batch) {
		var result = new DependsResult();
		for (var beanClass : batch) {
			try {
				dependsBean(beanClass, result);
			} catch (Exception e) {
				throw new IllegalStateException("cannot register runtime bean logs: " + beanClass.getName(), e);
			}
		}
		applyRegistrations(result);
	}

	static final class DependsResult {
		final HashSet<Class<?>> allBeans = new HashSet<>();
		final HashSet<Class<? extends Bean>> beans = new HashSet<>();
		final HashSet<Class<? extends Serializable>> beanKeys = new HashSet<>();
		final HashSet<Class<?>> list1 = new HashSet<>();
		final HashSet<Class<? extends Bean>> list2 = new HashSet<>();
		final HashSet<KVNotNull<Class<?>, Class<?>>> map1 = new HashSet<>();
		final HashSet<KVNotNull<Class<?>, Class<? extends Bean>>> map2 = new HashSet<>();
		final HashSet<Map1Meta<?, ?>> map1Metas = new HashSet<>();
		final HashSet<Map2Meta<?, ? extends Bean>> map2Metas = new HashSet<>();
		final HashSet<Class<?>> set1 = new HashSet<>();
		final HashSet<KVNotNull<Class<? extends Comparable<?>>, Class<?>>> sortedMap1 = new HashSet<>();
		final HashSet<KVNotNull<Class<? extends Comparable<?>>, Class<? extends Bean>>> sortedMap2 = new HashSet<>();
		final HashSet<SortedMap1Meta<? extends Comparable<?>, ?>> sortedMap1Metas = new HashSet<>();
		final HashSet<SortedMap2Meta<? extends Comparable<?>, ? extends Bean>> sortedMap2Metas = new HashSet<>();
	}

	public static void registerAllTableLogs(Application zeze) throws Exception {
		registerTableLogs(zeze.getTables().values());
	}

	public static void registerTableLogs(Iterable<Table> tables) throws Exception {
		var result = new DependsResult();
		for (var table : tables)
			dependsTable(table, result);
		applyRegistrations(result);
	}

	/** 动态开表时注册该表的日志依赖，重复注册同一描述符是幂等的。 */
	public static void registerTableLogs(Table table) throws Exception {
		var result = new DependsResult();
		dependsTable(table, result);
		applyRegistrations(result);
	}

	private static void dependsTable(Table table, DependsResult result) throws Exception {
		var keyClass = table.getKeyClass();
		if (Serializable.class.isAssignableFrom(keyClass)) {
			// must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = table.getValueClass();
		dependsBean(valueClass, result);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void applyRegistrations(DependsResult result) {
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
		for (var meta : result.map1Metas)
			registerLogMap1Meta(meta);
		for (var meta : result.map2Metas)
			registerLogMap2Meta(meta);
		for (var set1Class : result.set1)
			registerLogSet1(set1Class);
		for (var kv : result.sortedMap1)
			registerLogSortedMap1((Class<? extends Comparable>)kv.getKey(), kv.getValue());
		for (var kv : result.sortedMap2)
			registerLogSortedMap2((Class<? extends Comparable>)kv.getKey(), kv.getValue());
		for (var meta : result.sortedMap1Metas)
			registerLogSortedMap1Meta((SortedMap1Meta<? extends Comparable, ?>)meta);
		for (var meta : result.sortedMap2Metas)
			registerLogSortedMap2Meta((SortedMap2Meta<? extends Comparable, ? extends Bean>)meta);
		registerLogs();
	}

	@SuppressWarnings("unchecked")
	static void dependsBean(Class<?> beanClass, DependsResult result) throws Exception {
		if (result.allBeans.add(beanClass)) {
			var ctor = beanClass.getDeclaredConstructor();
			ctor.setAccessible(true);
			var obj = ctor.newInstance();
			var loader = beanClass.getClassLoader();
			if (obj instanceof BeanKey beanKey) {
				result.beanKeys.add((Class<? extends Serializable>)beanClass);
				for (var v : beanKey.variables()) {
					var type = v.getType();
					if (!isBuiltinType(type))
						dependsBean(Class.forName(type, true, loader), result);
				}
			} else if (obj instanceof Bean bean) {
				var declaredDynamic = new HashSet<Integer>();
				var dynamicDependencies = new HashSet<Class<?>>();
				LogRegistry.registerDynamicFields(bean.getClass(), declaredDynamic::add, dynamicDependencies::add);
				for (var dependency : dynamicDependencies)
					dependsBean(dependency, result);
				result.beans.add((Class<? extends Bean>)beanClass);
				for (var v : bean.variables()) {
					var type = v.getType();
					if (declaredDynamic.contains(v.getId())) {
						// 动态集合的键类型仍须注册。
						for (var key : v.getKey().split(",")) {
							if (!key.isBlank() && getBuiltinBoxingClass(key.trim()) == null)
								dependsBean(Class.forName(key.trim(), true, loader), result);
						}
						continue;
					}
					if (type.equals("dynamic") || (v.getValue().equals("dynamic") && switch (type) {
						case "list", "array", "map", "sortedmap", "gtable" -> true;
						default -> false;
					}))
						throw new IllegalStateException("missing dynamic log field: " + beanClass.getName() + '#' + v.getName());
					switch (type) {
					case "list":
					case "array":
						dependsList(v.getValue(), loader, result);
						break;
					case "map":
						dependsMap(v.getKey(), v.getValue(), loader, result);
						break;
					case "sortedmap":
						dependsSortedMap(v.getKey(), v.getValue(), loader, result);
						break;
					case "set":
						dependsSet(v.getValue(), loader, result);
						break;
					case "gtable":
						var keys = v.getKey().split(",");
						dependsGTable(keys[0].trim(), keys[1].trim(), v.getValue(), loader, result);
						break;
					default:
						if (!isBuiltinType(type))
							dependsBean(Class.forName(type, true, loader), result);
						break;
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	static void dependsList(String valueType, @Nullable ClassLoader loader, DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass != null) {
			result.list1.add(valueClass);
			return;
		}
		valueClass = Class.forName(valueType, true, loader);
		dependsBean(valueClass, result);
		// 选族须与生成器一致（Gen/Gen/java/TypeName.cs：IsNormalBean ? '2' : '1'）：beankey
		// 不是 normal bean，值集合生成"1"族（PList1/LogList1）；按"2"族注册的 typeId 线上
		// 不出现，回放端解码抛 unknown log typeId。
		if (BeanKey.class.isAssignableFrom(valueClass))
			result.list1.add(valueClass);
		else
			result.list2.add((Class<? extends Bean>)valueClass);
	}

	@SuppressWarnings("unchecked")
	static void dependsSortedMap(String keyType, String valueType, @Nullable ClassLoader loader, DependsResult result)
		throws Exception {
		var keyClass = (Class<? extends Comparable<?>>)getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = (Class<? extends Comparable<?>>)Class.forName(keyType, true, loader); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType, true, loader); // bean or beanKey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成"1"族（PSortedMap1/LogSortedMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass))
				result.sortedMap1.add(KVNotNull.create(keyClass, valueClass));
			else
				result.sortedMap2.add(KVNotNull.create(keyClass, (Class<? extends Bean>)valueClass));
		} else {
			result.sortedMap1.add(KVNotNull.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	static void dependsMap(String keyType, String valueType, @Nullable ClassLoader loader, DependsResult result)
		throws Exception {
		var keyClass = getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = Class.forName(keyType, true, loader); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType, true, loader); // bean or beankey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成"1"族（PMap1/LogMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass))
				result.map1.add(KVNotNull.create(keyClass, valueClass));
			else
				result.map2.add(KVNotNull.create(keyClass, (Class<? extends Bean>)valueClass));
		} else {
			result.map1.add(KVNotNull.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	static void dependsGTable(String key1Type, String key2Type, String valueType, @Nullable ClassLoader loader,
							  DependsResult result) throws Exception {
		var key1Class = getBuiltinBoxingClass(key1Type);
		if (key1Class == null) {
			key1Class = Class.forName(key1Type, true, loader); // must be BeanKey.
			dependsBean(key1Class, result);
		}
		var key2Class = getBuiltinBoxingClass(key2Type);
		if (key2Class == null) {
			key2Class = Class.forName(key2Type, true, loader); // must be BeanKey.
			dependsBean(key2Class, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType, true, loader); // bean or beanKey
			dependsBean(valueClass, result);
			// 选族须与生成器一致（IsNormalBean ? '2' : '1'）：beankey 值生成 GTable1（内层 BeanMap1）。
			if (BeanKey.class.isAssignableFrom(valueClass)) {
				var factory = GTable1.getFactory(key1Class, key2Class, valueClass);
				result.map2Metas.add(factory.getPmapMeta());
				result.map1Metas.add(factory.getBmapMeta());
			} else {
				var factory = GTable2.getFactory(key1Class, key2Class, (Class<? extends Bean>)valueClass);
				result.map2Metas.add(factory.getPmapMeta());
				result.map2Metas.add(factory.getBmapMeta());
			}
		} else {
			var factory = GTable1.getFactory(key1Class, key2Class, valueClass);
			result.map2Metas.add(factory.getPmapMeta());
			result.map1Metas.add(factory.getBmapMeta());
		}
	}

	private static void dependsSet(String valueType, @Nullable ClassLoader loader, DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType, true, loader); // must be beanKey
			dependsBean(valueClass, result);
		}
		result.set1.add(valueClass);
	}

	public static @Nullable Class<?> getBuiltinBoxingClass(String type) {
		return switch (type) {
			//@formatter:off
		case "bool"->Boolean.class;
		case "byte"->Byte.class;
		case "short"->Short.class;
		case "int"->Integer.class;
		case "long"->Long.class;
		case "float"->Float.class;
		case "double"->Double.class;
		case "binary"->Zeze.Net.Binary.class;
		case "string"->String.class;
		case "decimal"->BigDecimal.class;
		case "vector2"->Zeze.Serialize.Vector2.class;
		case "vector2int"->Zeze.Serialize.Vector2Int.class;
		case "vector3"->Zeze.Serialize.Vector3.class;
		case "vector3int"->Zeze.Serialize.Vector3Int.class;
		case "vector4"->Zeze.Serialize.Vector4.class;
		case "quaternion"->Zeze.Serialize.Quaternion.class;
		case "dynamic"->DynamicBean.class;
		default -> null;
		};
	}

	public static boolean isBuiltinType(String type) {
		return getBuiltinBoxingClass(type) != null;
	}

	// 工厂闭包直接捕获meta：解码是热路径（follower复制/history回放），勿在lambda内重查XxxMeta.get。
	public static <T extends Serializable> void registerLogBeanKey(Class<T> beanClass) {
		var meta = BeanKeyMeta.get(beanClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogBeanKeyValue.decode(varId, meta, bb));
	}

	public static <T> void registerLogList1(Class<T> valueClass) {
		var meta = List1Meta.get(valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogList1.decode(varId, meta, bb));
	}

	public static <V extends Bean> void registerLogList2(Class<V> valueClass) {
		var meta = List2Meta.get(valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogList2.decode(varId, meta, bb));
	}

	public static <K, V> void registerLogMap1(Class<K> keyClass, Class<V> valueClass) {
		var meta = Map1Meta.get(keyClass, valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogMap1.decode(varId, meta, bb));
	}

	public static <K, V extends Bean> void registerLogMap2(Class<K> keyClass, Class<V> valueClass) {
		var meta = Map2Meta.get(keyClass, valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogMap2.decode(varId, meta, bb));
	}

	public static <K, V> void registerLogMap1Meta(Map1Meta<K, V> meta) {
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogMap1.decode(varId, meta, bb));
	}

	public static <K, V extends Bean> void registerLogMap2Meta(Map2Meta<K, V> meta) {
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogMap2.decode(varId, meta, bb));
	}

	public static <V> void registerLogSet1(Class<V> valueClass) {
		var meta = Set1Meta.get(valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogSet1.decode(varId, meta, bb));
	}

	public static <V extends Bean> void registerLogOne(Class<V> beanClass) {
		var meta = LogOneMeta.get(beanClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogOneValue.decode(varId, meta, bb));
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1(Class<K> keyClass,
																		  Class<V> valueClass) {
		var meta = SortedMap1Meta.get(keyClass, valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogSortedMap1.decode(varId, meta, bb));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2(Class<K> keyClass,
																					   Class<V> valueClass) {
		var meta = SortedMap2Meta.get(keyClass, valueClass);
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogSortedMap2.decode(varId, meta, bb));
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1Meta(SortedMap1Meta<K, V> meta) {
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogSortedMap1.decode(varId, meta, bb));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2Meta(SortedMap2Meta<K, V> meta) {
		LogRegistry.register(meta.logTypeId, meta.name, (varId, bb) -> LogSortedMap2.decode(varId, meta, bb));
	}

	public static void registerLogs() {
		LogRegistry.register(LogBoolValue.TYPE_ID, "LogBool", LogBoolValue::decode);
		LogRegistry.register(LogByteValue.TYPE_ID, "LogByte", LogByteValue::decode);
		LogRegistry.register(LogShortValue.TYPE_ID, "LogShort", LogShortValue::decode);
		LogRegistry.register(LogIntValue.TYPE_ID, "LogInt", LogIntValue::decode);
		LogRegistry.register(LogLongValue.TYPE_ID, "LogLong", LogLongValue::decode);
		LogRegistry.register(LogFloatValue.TYPE_ID, "LogFloat", LogFloatValue::decode);
		LogRegistry.register(LogDoubleValue.TYPE_ID, "LogDouble", LogDoubleValue::decode);
		LogRegistry.register(LogBinaryValue.TYPE_ID, "LogBinary", LogBinaryValue::decode);
		LogRegistry.register(LogStringValue.TYPE_ID, "LogString", LogStringValue::decode);
		LogRegistry.register(LogDecimalValue.TYPE_ID, "LogDecimal", LogDecimalValue::decode);
		LogRegistry.register(LogVector2Value.TYPE_ID, "LogVector2", LogVector2Value::decode);
		LogRegistry.register(LogVector2IntValue.TYPE_ID, "LogVector2Int", LogVector2IntValue::decode);
		LogRegistry.register(LogVector3Value.TYPE_ID, "LogVector3", LogVector3Value::decode);
		LogRegistry.register(LogVector3IntValue.TYPE_ID, "LogVector3Int", LogVector3IntValue::decode);
		LogRegistry.register(LogVector4Value.TYPE_ID, "LogVector4", LogVector4Value::decode);
		LogRegistry.register(LogQuaternionValue.TYPE_ID, "LogQuaternion", LogQuaternionValue::decode);
		LogRegistry.register(LogBean.TYPE_ID, "LogBean", LogBean::decode);
		LogRegistry.register(LogDynamicValue.TYPE_ID, "LogDynamic", LogDynamicValue::decode);
	}
}
