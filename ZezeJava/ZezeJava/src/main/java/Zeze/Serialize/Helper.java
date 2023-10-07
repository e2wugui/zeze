package Zeze.Serialize;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import Zeze.Net.Binary;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DynamicBean;
import Zeze.Util.Json;
import Zeze.Util.JsonReader;
import Zeze.Util.JsonWriter;
import Zeze.Util.Task;

public class Helper {
	public static Vector4 decodeVector4(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Vector4(x, y, z, w);
	}

	public static Quaternion decodeQuaternion(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		var w = rs.getFloat(_parents_name_ + "w");
		return new Quaternion(x, y, z, w);
	}

	public static Vector3 decodeVector3(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		var z = rs.getFloat(_parents_name_ + "z");
		return new Vector3(x, y, z);
	}

	public static Vector2 decodeVector2(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getFloat(_parents_name_ + "x");
		var y = rs.getFloat(_parents_name_ + "y");
		return new Vector2(x, y);
	}

	public static Vector2Int decodeVector2Int(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		return new Vector2Int(x, y);
	}

	public static Vector3Int decodeVector3Int(ArrayList<String> parents, ResultSet rs) throws SQLException {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		var x = rs.getInt(_parents_name_ + "x");
		var y = rs.getInt(_parents_name_ + "y");
		var z = rs.getInt(_parents_name_ + "z");
		return new Vector3Int(x, y, z);
	}

	public static void encodeVector2(Vector2 value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3(Vector3 value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
	}

	public static void encodeVector4(Vector4 value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeQuaternion(Quaternion value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendFloat(_parents_name_ + "x", value.x);
		st.appendFloat(_parents_name_ + "y", value.y);
		st.appendFloat(_parents_name_ + "z", value.z);
		st.appendFloat(_parents_name_ + "w", value.w);
	}

	public static void encodeVector2Int(Vector2Int value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
	}

	public static void encodeVector3Int(Vector3Int value, ArrayList<String> parents, SQLStatement st) {
		var _parents_name_ = Zeze.Transaction.Bean.parentsToName(parents);
		st.appendInt(_parents_name_ + "x", value.x);
		st.appendInt(_parents_name_ + "y", value.y);
		st.appendInt(_parents_name_ + "z", value.z);
	}

	private static final Json json = Json.instance.clone();
	private static final Json.ClassMeta<DynamicBean> dynamicBeanMeta = json.getClassMeta(DynamicBean.class);

	public static void decodeJsonDynamic(DynamicBean bean, String jsonStr) {
		if (jsonStr == null)
			bean.reset();
		else {
			try {
				JsonReader.local().buf(jsonStr).parse(json, bean, dynamicBeanMeta);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			}
		}
	}

	public static <T> void decodeJsonList(List<T> list, Class<T> valueClass, String jsonStr) {
		list.clear();
		if (jsonStr != null) {
			try {
				JsonReader.local().buf(jsonStr).parseArray(json, list, valueClass);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			}
		}
	}

	public static <T> void decodeJsonSet(Set<T> set, Class<T> valueClass, String jsonStr) {
		set.clear();
		if (jsonStr != null) {
			try {
				JsonReader.local().buf(jsonStr).parseArray(json, set, valueClass);
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void decodeJsonMap(Bean parentBean, String fieldName, Map<?, ?> map, String jsonStr) {
		map.clear();
		if (jsonStr != null) {
			try {
				JsonReader.local().buf('{' + fieldName + ':' + jsonStr + '}').parse(json, parentBean,
						(Class<? super Bean>)parentBean.getClass());
			} catch (ReflectiveOperationException e) {
				Task.forceThrow(e);
			}
		}
	}

	public static String encodeJson(Object obj) {
		return JsonWriter.local().clear().setFlagsAndDepthLimit(0, 16).write(json, obj).toString();
	}
}
