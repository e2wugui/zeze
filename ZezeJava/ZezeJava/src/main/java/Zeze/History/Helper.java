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
import Zeze.Transaction.Collections.BeanKeyMeta;
import Zeze.Transaction.Collections.List1Meta;
import Zeze.Transaction.Collections.List2Meta;
import Zeze.Transaction.Collections.LogList1;
import Zeze.Transaction.Collections.LogList2;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSet1;
import Zeze.Transaction.Collections.LogOne;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pcollections.Empty;

public class Helper {
	private static final Logger logger = LogManager.getLogger(Helper.class);

	/** dynamic家族：工厂对+来源（宿主bean类#变量名），供同typeId多家族留痕。 */
	public static final class DynamicFamily {
		public final KV<ToLongFunction<Bean>, LongFunction<Bean>> factories;
		public final String where;

		DynamicFamily(@NotNull KV<ToLongFunction<Bean>, LongFunction<Bean>> factories, @NotNull String where) {
			this.factories = factories;
			this.where = where;
		}
	}

	public static class DependsResult {
		public final HashSet<Class<?>> allBeans = new HashSet<>();
		public final HashSet<Class<? extends Bean>> beans = new HashSet<>();
		public final HashSet<Class<? extends Serializable>> beanKeys = new HashSet<>();
		public final HashSet<Class<?>> list1 = new HashSet<>();
		public final HashSet<Class<? extends Bean>> list2 = new HashSet<>();
		public final HashSet<KV<ToLongFunction<Bean>, LongFunction<Bean>>> list2Dynamic = new HashSet<>();
		public final HashSet<KV<Class<?>, Class<?>>> map1 = new HashSet<>();
		public final HashSet<KV<Class<?>, Class<? extends Bean>>> map2 = new HashSet<>();
		public final HashMap<KV<Class<?>, Class<? extends Bean>>, DynamicFamily> map2Dynamic = new HashMap<>();
		public final HashSet<Map1Meta<?, ?>> map1Metas = new HashSet<>();
		public final HashSet<Map2Meta<?, ? extends Bean>> map2Metas = new HashSet<>();
		public final HashSet<Class<?>> set1 = new HashSet<>();
		public final HashSet<KV<Class<? extends Comparable<?>>, Class<?>>> sortedMap1 = new HashSet<>();
		public final HashSet<KV<Class<? extends Comparable<?>>, Class<? extends Bean>>> sortedMap2 = new HashSet<>();
		public final HashMap<KV<Class<? extends Comparable<?>>, Class<? extends Bean>>, DynamicFamily>
				sortedMap2Dynamic = new HashMap<>();
		public final HashSet<SortedMap1Meta<? extends Comparable<?>, ?>> sortedMap1Metas = new HashSet<>();
		public final HashSet<SortedMap2Meta<? extends Comparable<?>, ? extends Bean>> sortedMap2Metas = new HashSet<>();
		// GTable外层typeId/name只含rowClass不含列/值身份：同(kind,rowClass)不同列/值类型
		// 共享typeId且Log.register先到先得——冲突登记启动fail-fast，否则回放端用别人的
		// 行工厂解码整行日志。
		public final HashMap<String, String> gtableIdentities = new HashMap<>();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void registerAllTableLogs(@NotNull Application zeze) throws Exception {
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
		for (var list2Dynamic : result.list2Dynamic)
			registerLogList2Dynamic(list2Dynamic.getKey(), list2Dynamic.getValue());
		for (var map1KV : result.map1)
			registerLogMap1(map1KV.getKey(), map1KV.getValue());
		for (var map2KV : result.map2)
			registerLogMap2(map2KV.getKey(), map2KV.getValue());
		for (var e : result.map2Dynamic.entrySet())
			registerLogMap2Dynamic(e.getKey().getKey(), e.getValue().factories.getKey(), e.getValue().factories.getValue());
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
		for (var e : result.sortedMap2Dynamic.entrySet()) {
			registerLogSortedMap2Dynamic((Class<? extends Comparable>)e.getKey().getKey(),
					e.getValue().factories.getKey(), e.getValue().factories.getValue());
		}
		for (var meta : result.sortedMap1Metas)
			registerLogSortedMap1Meta((SortedMap1Meta<? extends Comparable, ?>)meta);
		for (var meta : result.sortedMap2Metas)
			registerLogSortedMap2Meta((SortedMap2Meta<? extends Comparable, ? extends Bean>)meta);
		registerLogs();
	}

	@SuppressWarnings("unchecked")
	public static void dependsBean(@NotNull Class<?> beanClass, @NotNull DependsResult result) throws Exception {
		if (result.allBeans.add(beanClass)) {
			var obj = beanClass.getConstructor((Class<?>[])null).newInstance((Object[])null);
			if (obj instanceof BeanKey beanKey) {
				result.beanKeys.add((Class<? extends Serializable>)beanClass);
				for (var v : beanKey.variables()) {
					var type = v.getType();
					if (!isBuiltinType(type))
						dependsBean(Class.forName(type), result);
				}
			} else if (obj instanceof Bean bean) {
				result.beans.add((Class<? extends Bean>)beanClass);
				for (var v : bean.variables()) {
					var type = v.getType();
					switch (type) {
					case "list":
					case "array":
						dependsList(beanClass, v, v.getValue(), result);
						break;
					case "map":
						dependsMap(beanClass, v, v.getKey(), v.getValue(), result);
						break;
					case "sortedmap":
						dependsSortedMap(beanClass, v, v.getKey(), v.getValue(), result);
						break;
					case "set":
						dependsSet(v.getValue(), result);
						break;
					case "gtable":
						var keys = v.getKey().split(",");
						dependsGTable(beanClass, v, keys[0].trim(), keys[1].trim(), v.getValue(), result);
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
	public static void dependsList(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String valueType,
								   @NotNull DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass != null) {
			if (valueClass == DynamicBean.class) {
				try {
					var db = (DynamicBean)beanClass.getMethod("newDynamicBean_"
							+ Character.toUpperCase(v.getName().charAt(0))
							+ v.getName().substring(1), (Class<?>[])null).invoke(null, (Object[])null);
					result.list2Dynamic.add(KV.create(db.getGetBean(), db.getCreateBean()));
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				}
			} else
				result.list1.add(valueClass);
			return;
		}
		valueClass = Class.forName(valueType);
		dependsBean(valueClass, result);
		result.list2.add((Class<? extends Bean>)valueClass);
	}

	// 反射调用宿主bean的newDynamicBean_<VarName>取该变量的dynamic工厂对。
	private static DynamicFamily newDynamicFamily(@NotNull Class<?> beanClass, @NotNull BVariable.Data v) {
		try {
			var db = (DynamicBean)beanClass.getMethod("newDynamicBean_"
					+ Character.toUpperCase(v.getName().charAt(0))
					+ v.getName().substring(1), (Class<?>[])null).invoke(null, (Object[])null);
			return new DynamicFamily(KV.create(db.getGetBean(), db.getCreateBean()),
					beanClass.getName() + '#' + v.getName());
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// 【FND8-30】dynamic集合的logTypeId不含值工厂身份：同(keyClass,DynamicBean)的第二个
	// 家族与首个同typeId，Log.register先到先得，后注册家族的日志在回放端用别人的create工厂
	// 解码（显式Bean:id编号重叠时静默解出错误bean，默认编号抛incompatible中断回放）。
	// 原computeIfAbsent静默丢弃后续家族——改为warn留痕（含两个宿主bean类名与变量名），
	// 语义冲突的启动error需要每变量的specialTypeId→beanClass映射表（生成器侧暴露，另行跟进）。
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void putDynamicFamily(@NotNull HashMap families, @NotNull Object key,
										 @NotNull Class<?> beanClass, @NotNull BVariable.Data v) {
		var family = newDynamicFamily(beanClass, v);
		var exist = (DynamicFamily)families.putIfAbsent(key, family);
		if (exist != null && (exist.factories.getKey() != family.factories.getKey()
				|| exist.factories.getValue() != family.factories.getValue()))
			logger.warn("dynamic collection family dropped: same log typeId with different factories."
					+ " keep={} drop={} key=({},{})。回放端按先注册家族的工厂解码，显式Bean:id编号重叠时"
					+ "将静默解出错误类型的bean（FND8-30）",
					exist.where, family.where, ((KV)key).getKey(), ((KV)key).getValue());
	}

	@SuppressWarnings("unchecked")
	public static void dependsSortedMap(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String keyType,
										@NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var keyClass = (Class<? extends Comparable<?>>)getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = (Class<? extends Comparable<?>>)Class.forName(keyType); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		var is2 = valueClass == null;
		if (is2) {
			valueClass = Class.forName(valueType); // bean or beanKey
			dependsBean(valueClass, result);
			result.sortedMap2.add(KV.create(keyClass, (Class<? extends Bean>)valueClass));
		} else if (valueClass == DynamicBean.class) {
			putDynamicFamily(result.sortedMap2Dynamic, KV.create(keyClass, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			result.sortedMap1.add(KV.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsMap(@NotNull Class<?> beanClass, @NotNull BVariable.Data v, @NotNull String keyType,
								  @NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var keyClass = getBuiltinBoxingClass(keyType);
		if (keyClass == null) {
			keyClass = Class.forName(keyType); // must be BeanKey.
			dependsBean(keyClass, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		var is2 = valueClass == null;
		if (is2) {
			valueClass = Class.forName(valueType); // bean or beankey
			dependsBean(valueClass, result);
			result.map2.add(KV.create(keyClass, (Class<? extends Bean>)valueClass));
		} else if (valueClass == DynamicBean.class) {
			putDynamicFamily(result.map2Dynamic, KV.create(keyClass, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			result.map1.add(KV.create(keyClass, valueClass));
		}
	}

	@SuppressWarnings("unchecked")
	public static void dependsGTable(@NotNull Class<?> beanClass,
									 @NotNull BVariable.Data v,
									 @NotNull String key1Type,
									 @NotNull String key2Type,
									 @NotNull String valueType,
									 @NotNull DependsResult result) throws Exception {
		var key1Class = getBuiltinBoxingClass(key1Type);
		if (key1Class == null) {
			key1Class = Class.forName(key1Type); // must be BeanKey.
			dependsBean(key1Class, result);
		}
		var key2Class = getBuiltinBoxingClass(key2Type);
		if (key2Class == null) {
			key2Class = Class.forName(key2Type); // must be BeanKey.
			dependsBean(key2Class, result);
		}
		var valueClass = getBuiltinBoxingClass(valueType);
		var is2 = valueClass == null;
		if (is2) {
			valueClass = Class.forName(valueType); // bean or beanKey
			dependsBean(valueClass, result);
			checkGTableIdentity(result, "GTable2", key1Class, key2Class, valueClass.getName());
			var factory = GTable2.getFactory(key1Class, key2Class, (Class<? extends Bean>)valueClass);
			result.map2Metas.add(factory.getPmapMeta());
			result.map2Metas.add(factory.getBmapMeta());
		} else if (valueClass == DynamicBean.class) {
			// 【FND8-33 A3】先取每变量的get/create工厂，再经dynamic重载构建
			//（三参版对DynamicBean必抛：无无参构造器）。
			var family = newDynamicFamily(beanClass, v);
			// dynamic值的闭包身份由(beanClass,变量名)定位：同row不同家族的GTable同样共享外层typeId。
			checkGTableIdentity(result, "GTable2", key1Class, key2Class,
					"dynamic:" + beanClass.getName() + "." + v.getName());
			var factory = GTable2.getFactory(key1Class, key2Class, family.factories.getKey(), family.factories.getValue());
			result.map2Metas.add(factory.getPmapMeta());
			putDynamicFamily(result.map2Dynamic, KV.create(key2Class, (Class<? extends Bean>)valueClass), beanClass, v);
		} else {
			checkGTableIdentity(result, "GTable1", key1Class, key2Class, valueClass.getName());
			var factory = GTable1.getFactory(key1Class, key2Class, valueClass);
			result.map2Metas.add(factory.getPmapMeta());
			result.map1Metas.add(factory.getBmapMeta());
		}
	}

	// 同(kind,rowClass)只允许一种(列,值)身份：冲突时Log.register静默保留先注册者，回放端
	// 整行日志被错误工厂解码——fail-fast优于静默损坏。
	private static void checkGTableIdentity(@NotNull DependsResult result, @NotNull String kind,
											@NotNull Class<?> rowClass, @NotNull Class<?> colClass,
											@NotNull String valueIdentity) {
		var key = kind + ':' + rowClass.getName();
		var identity = colClass.getName() + '|' + valueIdentity;
		var saved = result.gtableIdentities.putIfAbsent(key, identity);
		if (saved != null && !saved.equals(identity))
			throw new IllegalStateException("GTable duplicate outer logTypeId: " + kind
					+ " row=" + rowClass.getName()
					+ " has different col/value types: {" + saved.replace('|', ',')
					+ "} vs {" + identity.replace('|', ',')
					+ "}。Log.register同名先到先得，History回放将用错误的行工厂解码整行日志"
					+ "——请为其中一个表改用不同的row类型");
	}

	public static void dependsSet(@NotNull String valueType, @NotNull DependsResult result) throws Exception {
		var valueClass = getBuiltinBoxingClass(valueType);
		if (valueClass == null) {
			valueClass = Class.forName(valueType); // must be beanKey
			dependsBean(valueClass, result);
		}
		result.set1.add(valueClass);
	}

	public static @Nullable Class<?> getBuiltinBoxingClass(@NotNull String type) {
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

	public static boolean isBuiltinType(@NotNull String type) {
		return getBuiltinBoxingClass(type) != null;
	}

	// 工厂闭包直接捕获meta：解码是热路径（follower复制/history回放），勿在lambda内重查XxxMeta.get。
	public static <T extends Serializable> void registerLogBeanKey(@NotNull Class<T> beanClass) {
		var meta = BeanKeyMeta.get(beanClass);
		Log.register(varId -> new LogBeanKey<>(varId, meta));
	}

	public static <T> void registerLogList1(@NotNull Class<T> valueClass) {
		var meta = List1Meta.get(valueClass);
		Log.register(varId -> new LogList1<>(null, varId, null, Empty.vector(), meta));
	}

	public static <V extends Bean> void registerLogList2(@NotNull Class<V> valueClass) {
		var meta = List2Meta.get(valueClass);
		Log.register(varId -> new LogList2<>(null, varId, null, Empty.vector(), meta));
	}

	public static void registerLogList2Dynamic(@NotNull ToLongFunction<Bean> get,
											   @NotNull LongFunction<Bean> create) {
		var meta = List2Meta.createDynamic(get, create);
		Log.register(varId -> new LogList2<>(null, varId, null, Empty.vector(), meta));
	}

	public static <K, V> void registerLogMap1(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var meta = Map1Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogMap1<>(null, varId, null, Empty.map(), meta));
	}

	public static <K, V extends Bean> void registerLogMap2(@NotNull Class<K> keyClass, @NotNull Class<V> valueClass) {
		var meta = Map2Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta));
	}

	public static <K> void registerLogMap2Dynamic(@NotNull Class<K> keyClass,
												  @NotNull ToLongFunction<Bean> get,
												  @NotNull LongFunction<Bean> create) {
		var meta = Map2Meta.createDynamic(keyClass, get, create);
		Log.register(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta));
	}

	public static <K, V> void registerLogMap1Meta(@NotNull Map1Meta<K, V> meta) {
		Log.register(varId -> new LogMap1<>(null, varId, null, Empty.map(), meta));
	}

	public static <K, V extends Bean> void registerLogMap2Meta(@NotNull Map2Meta<K, V> meta) {
		Log.register(varId -> new LogMap2<>(null, varId, null, Empty.map(), meta));
	}

	public static <V> void registerLogSet1(@NotNull Class<V> valueClass) {
		var meta = Set1Meta.get(valueClass);
		Log.register(varId -> new LogSet1<>(null, varId, null, Empty.set(), meta));
	}

	public static <V extends Bean> void registerLogOne(@NotNull Class<V> beanClass) {
		var meta = LogOneMeta.get(beanClass);
		Log.register(varId -> new LogOne<>(varId, meta));
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1(@NotNull Class<K> keyClass,
																		  @NotNull Class<V> valueClass) {
		var meta = SortedMap1Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogSortedMap1<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2(@NotNull Class<K> keyClass,
																					   @NotNull Class<V> valueClass) {
		var meta = SortedMap2Meta.get(keyClass, valueClass);
		Log.register(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>> void registerLogSortedMap2Dynamic(@NotNull Class<K> keyClass,
																			  @NotNull ToLongFunction<Bean> get,
																			  @NotNull LongFunction<Bean> create) {
		var meta = SortedMap2Meta.createDynamic(keyClass, get, create);
		Log.register(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>, V> void registerLogSortedMap1Meta(@NotNull SortedMap1Meta<K, V> meta) {
		Log.register(varId -> new LogSortedMap1<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static <K extends Comparable<K>, V extends Bean> void registerLogSortedMap2Meta(@NotNull SortedMap2Meta<K, V> meta) {
		Log.register(varId -> new LogSortedMap2<>(null, varId, null, Empty.sortedMap(), meta));
	}

	public static void registerLogs() {
		Log.register(LogBool::new);
		Log.register(LogByte::new);
		Log.register(LogShort::new);
		Log.register(LogInt::new);
		Log.register(LogLong::new);
		Log.register(LogFloat::new);
		Log.register(LogDouble::new);
		Log.register(LogBinary::new);
		Log.register(LogString::new);
		Log.register(LogDecimal::new);
		Log.register(LogVector2::new);
		Log.register(LogVector2Int::new);
		Log.register(LogVector3::new);
		Log.register(LogVector3Int::new);
		Log.register(LogVector4::new);
		Log.register(LogQuaternion::new);
		Log.register(varId -> new LogBean(null, varId, null));
		Log.register(varId -> new LogDynamic(null, varId, null));
	}
}
