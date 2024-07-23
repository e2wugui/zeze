package Zeze.Serialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Helper {
	public static @NotNull Vector2 decodeVector2(@NotNull ArrayList<String> parents,
												 @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		return new Vector2(x, y);
	}

	public static @NotNull Vector3 decodeVector3(@NotNull ArrayList<String> parents,
												 @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		return new Vector3(x, y, z);
	}

	public static @NotNull Vector4 decodeVector4(@NotNull ArrayList<String> parents,
												 @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Vector4(x, y, z, w);
	}

	public static @NotNull Quaternion decodeQuaternion(@NotNull ArrayList<String> parents,
													   @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Quaternion(x, y, z, w);
	}

	public static @NotNull Vector2Int decodeVector2Int(@NotNull ArrayList<String> parents,
													   @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		return new Vector2Int(x, y);
	}

	public static @NotNull Vector3Int decodeVector3Int(@NotNull ArrayList<String> parents,
													   @NotNull ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		var z = rs.getInt(_parents_name_ + "z");
		return new Vector3Int(x, y, z);
	}

	public static void encodeVector2(@NotNull Vector2 value, @NotNull ArrayList<String> parents,
									 @NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3(@NotNull Vector3 value, @NotNull ArrayList<String> parents,
									 @NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
	}

	public static void encodeVector4(@NotNull Vector4 value, @NotNull ArrayList<String> parents,
									 @NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeQuaternion(@NotNull Quaternion value, @NotNull ArrayList<String> parents,
										@NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeVector2Int(@NotNull Vector2Int value, @NotNull ArrayList<String> parents,
										@NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3Int(@NotNull Vector3Int value, @NotNull ArrayList<String> parents,
										@NotNull SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
		st.appendInt(_parents_name_ + "z", value.z);
	}

	private static final @NotNull Json json = Json.instance; // .clone();
	private static final @NotNull Json.ClassMeta<DynamicBean> dynamicBeanMeta = json.getClassMeta(DynamicBean.class);

	public static void decodeJsonDynamic(@NotNull DynamicBean bean, @Nullable String jsonStr) {
		if (jsonStr == null)
			bean.reset();
		else {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parse(json, bean, dynamicBeanMeta);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static <T> void decodeJsonList(@NotNull List<T> list, @NotNull Class<T> valueClass,
										  @Nullable String jsonStr) {
		list.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parseArray(json, list, valueClass);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static <T> void decodeJsonSet(@NotNull Set<T> set, @NotNull Class<T> valueClass, @Nullable String jsonStr) {
		set.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf(jsonStr).parseArray(json, set, valueClass);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void decodeJsonMap(@NotNull Bean parentBean, @NotNull String fieldName, @NotNull Map<?, ?> map,
									 @Nullable String jsonStr) {
		map.clear();
		if (jsonStr != null) {
			var jr = JsonReader.local();
			try {
				jr.buf('{' + fieldName + ':' + jsonStr + '}').parse(json, parentBean,
						(Class<? super Bean>)parentBean.getClass());
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			} finally {
				jr.reset();
			}
		}
	}

	public static @NotNull String encodeJson(@Nullable Object obj) {
		var jw = JsonWriter.local();
		try {
			return jw.clear().setFlagsAndDepthLimit(0, 16).write(json, obj).toString();
		} finally {
			jw.clear();
		}
	}
}
