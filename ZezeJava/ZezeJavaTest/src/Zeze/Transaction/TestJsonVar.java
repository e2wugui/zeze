package Zeze.Trans;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.NullMarked;

import com.alibaba.fastjson2.JSONObject;
import demo.App;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@NullMarked
public class TestJsonVar {
	@BeforeEach
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testJsonObject() {
		requireNonNull(App.getInstance().Zeze).newProcedure(() -> {
			var bValue = requireNonNull(App.getInstance().demo_Module1).getTable1().getOrAdd(7678L);
			bValue.getJsonObject_JSON_OBJECT().put("key", "value");
			return 0;
		}, "jsonObject.put").call();
		requireNonNull(App.getInstance().Zeze).newProcedure(() -> {
			var bValue = requireNonNull(App.getInstance().demo_Module1).getTable1().getOrAdd(7678L);
			System.out.println(bValue.getJsonObject());
			Assertions.assertEquals("{\"key\":\"value\"}", bValue.getJsonObject());
			return 0;
		}, "jsonObject.get").call();
	}

	@Test
	public void testJsonArray() {
		requireNonNull(App.getInstance().Zeze).newProcedure(() -> {
			var bValue = requireNonNull(App.getInstance().demo_Module1).getTable1().getOrAdd(7678L);
			bValue.getJsonArray_JSON_ARRAY().clear();
			return 0;
		}, "jsonArray.clear").call();
		requireNonNull(App.getInstance().Zeze).newProcedure(() -> {
			var bValue = requireNonNull(App.getInstance().demo_Module1).getTable1().getOrAdd(7678L);
			var json = new JSONObject();
			json.put("key", "value");
			bValue.getJsonArray_JSON_ARRAY().add(json);
			return 0;
		}, "jsonArray.put").call();
		requireNonNull(App.getInstance().Zeze).newProcedure(() -> {
			var bValue = requireNonNull(App.getInstance().demo_Module1).getTable1().getOrAdd(7678L);
			System.out.println(bValue.getJsonArray());
			Assertions.assertEquals("[{\"key\":\"value\"}]", bValue.getJsonArray());
			return 0;
		}, "jsonArray.get").call();
	}
}
