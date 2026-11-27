package Zeze.Serialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Collections.Meta2;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.GTable.GTable1;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.Task;
import org.jspecify.annotations.Nullable;

/** 生成代码的编解码辅助：向量/四元数的 SQL 列装卸载，以及各容器类型的 JSON 解码。 */
public class Helper {
	public static Vector2 decodeVector2(ArrayList<String> parents,
												 ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		return new Vector2(x, y);
	}

	public static Vector3 decodeVector3(ArrayList<String> parents,
												 ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		return new Vector3(x, y, z);
	}

	public static Vector4 decodeVector4(ArrayList<String> parents,
												 ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Vector4(x, y, z, w);
	}

	public static Quaternion decodeQuaternion(ArrayList<String> parents,
													   ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Quaternion(x, y, z, w);
	}

	public static Vector2Int decodeVector2Int(ArrayList<String> parents,
													   ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		return new Vector2Int(x, y);
	}

	public static Vector3Int decodeVector3Int(ArrayList<String> parents,
													   ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		var z = rs.getInt(_parents_name_ + "z");
		return new Vector3Int(x, y, z);
	}

	public static void encodeVector2(Vector2 value, ArrayList<String> parents,
									 SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3(Vector3 value, ArrayList<String> parents,
									 SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
	}

	public static void encodeVector4(Vector4 value, ArrayList<String> parents,
									 SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeQuaternion(Quaternion value, ArrayList<String> parents,
										SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeVector2Int(Vector2Int value, ArrayList<String> parents,
										SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3Int(Vector3Int value, ArrayList<String> parents,
										SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
		st.appendInt(_parents_name_ + "z", value.z);
	}

	private static final Json json = Json.instance;
	private static final Json.ClassMeta<DynamicBean> dynamicBeanMeta = json.getClassMeta(DynamicBean.class);

	public static void decodeJsonDynamic(DynamicBean bean, @Nullable String jsonStr) {
		if (jsonStr == null)
			bean.reset();
		else {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parse(json, bean, dynamicBeanMeta);
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static <T> void decodeJsonList(List<T> list, Class<T> valueClass,
										  @Nullable String jsonStr) {
		list.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parseArray(json, list, valueClass);
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static <T> void decodeJsonSet(Set<T> set, Class<T> valueClass, @Nullable String jsonStr) {
		set.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parseArray(json, set, valueClass);
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	/**
	 * 以 {fieldName:json} 包装并通过反射字段名匹配，将 SQL 列 JSON 解码进宿主 bean 的 map 字段。
	 * fieldName 须与 fieldNameFilter 过滤后的宿主字段名一致，否则 JsonReader 会静默跳过；不确定字段名时请用 decodeJsonTypedMap。
	 */
	@SuppressWarnings("unchecked")
	public static void decodeJsonMap(Bean parentBean, String fieldName, Map<?, ?> map,
									 @Nullable String jsonStr) {
		map.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf('{' + fieldName + ':' + jsonStr + '}').parse(json, parentBean,
						(Class<? super Bean>)parentBean.getClass());
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	// 按 Meta2 的键值真实类型缓存 FieldMeta，直接解析 JSON map，避免反射字段名匹配失败或泛型擦除。
	private static final ConcurrentHashMap<Meta2<?, ?>, Json.FieldMeta> jsonMapFieldMetas
			= new ConcurrentHashMap<>();

	public static <K, V> void decodeJsonTypedMap(Map<?, ?> map, Meta2<K, V> meta,
												 @Nullable String jsonStr) {
		map.clear();
		if (jsonStr == null)
			return;
		var fm = jsonMapFieldMetas.computeIfAbsent(meta, __ -> {
			try {
				// dummyField 仅占位 FieldMeta.field；非内建键类型经回退工厂获取 keyReader。
				var dummyField = Helper.class.getDeclaredField("jsonMapFieldMetas");
				return new Json.FieldMeta(0x30 + Json.ClassMeta.getType(meta.valueClass), 0, "Map",
						meta.valueClass, Json.ClassMeta.getDefCtor(meta.valueClass),
						Json.ClassMeta.getKeyReaderOrFallback(json, meta.keyClass, "BeanMap map key"), dummyField);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		});
		var jr = JsonReader.local();
		try {
			@SuppressWarnings("unchecked")
			var m = (Map<Object, Object>)map;
			if (jr.buf(jsonStr).next() == '{')
				jr.parseMap0(m, json.getClassMeta(map.getClass()), fm);
		} catch (ReflectiveOperationException e) {
			throw Task.forceThrow(e);
		} finally {
			jr.reset();
		}
	}

	@SuppressWarnings("unchecked")
	public static void decodeJsonGTable1(Bean parentBean, String fieldName,
										 Object map, @Nullable String jsonStr) {
		((GTable1<?, ?, ?>)map).clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf('{' + fieldName + ':' + jsonStr + '}').parse(json, parentBean,
						(Class<? super Bean>)parentBean.getClass());
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void decodeJsonGTable2(Bean parentBean, String fieldName,
										 Object map, @Nullable String jsonStr) {
		((GTable2<?, ?, ?, ?>)map).clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf('{' + fieldName + ':' + jsonStr + '}').parse(json, parentBean,
						(Class<? super Bean>)parentBean.getClass());
			} catch (ReflectiveOperationException e) {
				throw Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static String encodeJson(@Nullable Object obj) {
		var jw = JsonWriter.local();
		int previousFlags = jw.getFlags();
		int previousDepth = jw.getDepthLimit();
		try {
			return jw.clear().setFlagsAndDepthLimit(JsonWriter.FLAG_THROW_ON_DEPTH_LIMIT, 16)
					.write(json, obj).toString();
		} finally {
			jw.clear().setFlagsAndDepthLimit(previousFlags, previousDepth);
		}
	}
}
