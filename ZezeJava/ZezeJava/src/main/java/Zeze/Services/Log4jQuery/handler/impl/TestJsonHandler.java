package Zeze.Services.Log4jQuery.handler.impl;

import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.entity.JsonTestObj;
import Zeze.Services.Log4jQuery.handler.QueryHandler;

/**
 * test_json 命令：JSON 序列化连通性测试，原样返回参数对象。
 */
@HandlerCmd("test_json")
public class TestJsonHandler implements QueryHandler<JsonTestObj, JsonTestObj> {
		@Override
		public JsonTestObj invoke(JsonTestObj param) {

			return param;
		}

	}
