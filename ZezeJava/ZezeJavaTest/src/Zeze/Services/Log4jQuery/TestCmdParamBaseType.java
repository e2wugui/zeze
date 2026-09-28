package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import Zeze.Services.Log4jQuery.handler.QueryHandler;
import Zeze.Services.Log4jQuery.handler.QueryHandlerManager;
import Zeze.Services.Log4jQuery.handler.entity.ClassInfo;
import Zeze.Services.Log4jQuery.handler.entity.JsonTestObj;
import Zeze.Services.Log4jQuery.handler.impl.SelectCmdParamHandler;

import harness.Fast;

/**
 * GD-C10回归：SelectCmdParamHandler的isAssignableFrom参数方向写反
 * （paramClass.isAssignableFrom(Number.class)恒false），数值型参数命令的
 * cmd_param元数据baseType错误。当前注册命令无数值参数（潜伏缺陷），通过反射
 * 注入容器直接驱动真实invoke路径。
 */
@Fast
public class TestCmdParamBaseType {
	@Test
	public void testNumberParamMarkedBaseType() throws Exception {
		var handler = new SelectCmdParamHandler();
		var handlerMap = handlerMap();

		var cmds = new String[] {"fnd19_test_int", "fnd19_test_long", "fnd19_test_string", "fnd19_test_obj"};
		handlerMap.put(cmds[0], new QueryHandlerManager.QueryHandleContainer(intHandler()));
		handlerMap.put(cmds[1], new QueryHandlerManager.QueryHandleContainer(longHandler()));
		handlerMap.put(cmds[2], new QueryHandlerManager.QueryHandleContainer(stringHandler()));
		handlerMap.put(cmds[3], new QueryHandlerManager.QueryHandleContainer(objHandler()));
		try {
			var intInfo = handler.invoke(cmds[0]);
			assertEquals("java.lang.Integer", intInfo.getClassName());
			assertTrue(intInfo.isBaseType(), "数值参数应标记baseType（修复前恒false）");

			assertTrue(handler.invoke(cmds[1]).isBaseType());

			var strInfo = handler.invoke(cmds[2]);
			assertEquals("java.lang.String", strInfo.getClassName());
			assertTrue(strInfo.isBaseType());

			var objInfo = handler.invoke(cmds[3]);
			assertEquals(JsonTestObj.class.getName(), objInfo.getClassName());
			assertFalse(objInfo.isBaseType(), "非基本类型走字段表分支");
			assertNotNull(objInfo.getFields());
		} finally {
			for (var cmd : cmds)
				handlerMap.remove(cmd);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, QueryHandlerManager.QueryHandleContainer> handlerMap() throws Exception {
		Field field = QueryHandlerManager.class.getDeclaredField("handlerMap");
		field.setAccessible(true);
		return (Map<String, QueryHandlerManager.QueryHandleContainer>)field.get(null);
	}

	// 匿名类：QueryHandleContainer扫描getDeclaredMethods取invoke首个参数类型（桥方法invoke(Object)被跳过）。
	private static QueryHandler<Integer, ClassInfo> intHandler() {
		return new QueryHandler<>() {
			@Override
			public ClassInfo invoke(Integer param) {
				return null;
			}
		};
	}

	private static QueryHandler<Long, ClassInfo> longHandler() {
		return new QueryHandler<>() {
			@Override
			public ClassInfo invoke(Long param) {
				return null;
			}
		};
	}

	private static QueryHandler<String, ClassInfo> stringHandler() {
		return new QueryHandler<>() {
			@Override
			public ClassInfo invoke(String param) {
				return null;
			}
		};
	}

	private static QueryHandler<JsonTestObj, ClassInfo> objHandler() {
		return new QueryHandler<>() {
			@Override
			public ClassInfo invoke(JsonTestObj param) {
				return null;
			}
		};
	}
}
