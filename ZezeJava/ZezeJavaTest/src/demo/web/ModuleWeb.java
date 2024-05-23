package demo.web;

import Zeze.Netty.HttpExchange;

public class ModuleWeb extends AbstractModule {
	public void Start(demo.App app) throws Exception {
	}

	public void Stop(demo.App app) throws Exception {
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
