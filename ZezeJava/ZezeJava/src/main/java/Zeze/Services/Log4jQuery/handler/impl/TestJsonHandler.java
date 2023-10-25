package Zeze.Services.Log4jQuery.handler.impl;

import Zeze.Services.Log4jQuery.handler.HandlerCmd;
import Zeze.Services.Log4jQuery.handler.entity.JsonTestObj;
import Zeze.Services.Log4jQuery.handler.QueryHandler;

@HandlerCmd("test_json")
public class TestJsonHandler implements QueryHandler<JsonTestObj, JsonTestObj> {
		@Override
		public JsonTestObj invoke(JsonTestObj param) {

			return param;
		}

	}
