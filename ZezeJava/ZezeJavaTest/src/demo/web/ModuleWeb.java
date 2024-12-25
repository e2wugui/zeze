package demo.web;

import Zeze.Netty.HttpExchange;

public class ModuleWeb extends AbstractModule {
	public void Start(demo.App app) throws Exception {
	}

	public void Stop(demo.App app) throws Exception {
	}

	public demo.web.tMap2Bean1 getMap2Bean1() {
		return _tMap2Bean1;
	}

	@Override
	protected void OnServlethellocount(HttpExchange x) throws Exception {
		var counter = _tHelloCount.getOrAdd(1L);
		counter.setCount(counter.getCount() + 1);
		x.sendFreeMarker(counter);
	}

	// ZEZE_FILE_CHUNK {{{ GEN MODULE @formatter:off
    public ModuleWeb(demo.App app) {
        super(app);
    }
    // ZEZE_FILE_CHUNK }}} GEN MODULE @formatter:on
}
