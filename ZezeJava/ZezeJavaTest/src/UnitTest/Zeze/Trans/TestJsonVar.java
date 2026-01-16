package UnitTest.Zeze.Trans;

import com.alibaba.fastjson2.JSONObject;
import demo.App;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestJsonVar {
	@Before
	public void before() throws Exception {
		App.Instance.Start();
	}

	@Test
	public void testJsonObject() {
		App.getInstance().Zeze.newProcedure(() -> {
			var bValue = App.getInstance().demo_Module1.getTable1().getOrAdd(7678L);
			bValue.getJsonObject_JSON_OBJECT().put("key", "value");
			return 0;
		}, "jsonObject.put").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var bValue = App.getInstance().demo_Module1.getTable1().getOrAdd(7678L);
			System.out.println(bValue.getJsonObject());
			Assert.assertEquals("{\"key\":\"value\"}", bValue.getJsonObject());
			return 0;
		}, "jsonObject.get").call();
	}

	@Test
	public void testJsonArray() {
		App.getInstance().Zeze.newProcedure(() -> {
			var bValue = App.getInstance().demo_Module1.getTable1().getOrAdd(7678L);
			bValue.getJsonArray_JSON_ARRAY().clear();
			return 0;
		}, "jsonArray.clear").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var bValue = App.getInstance().demo_Module1.getTable1().getOrAdd(7678L);
			var json = new JSONObject();
			json.put("key", "value");
			bValue.getJsonArray_JSON_ARRAY().add(json);
			return 0;
		}, "jsonArray.put").call();
		App.getInstance().Zeze.newProcedure(() -> {
			var bValue = App.getInstance().demo_Module1.getTable1().getOrAdd(7678L);
			System.out.println(bValue.getJsonArray());
			Assert.assertEquals("[{\"key\":\"value\"}]", bValue.getJsonArray());
			return 0;
		}, "jsonArray.get").call();
	}
}
