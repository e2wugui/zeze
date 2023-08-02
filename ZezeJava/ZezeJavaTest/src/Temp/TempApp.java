package Temp;

import java.nio.file.Path;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Hot.HotManager;

public class TempApp extends AppBase {
	public final HotManager manager;

	public TempApp() throws Exception {
		var workingDir = "ZezeJavaTest\\hot";
		var distributeDir = "ZezeJavaTest\\hot\\distributes";
		manager = new HotManager(this, workingDir, distributeDir);
		manager.startModules(java.util.List.of());
	}

	@Override
	public Application getZeze() {
		return null;
	}
}
