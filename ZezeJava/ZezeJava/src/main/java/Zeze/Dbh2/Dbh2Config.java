package Zeze.Dbh2;

import Zeze.Config;
import org.w3c.dom.Element;

public class Dbh2Config implements Config.ICustomize {
	public int serverFastErrorPeriod = 5000;
	public int loginTimeout = 3000;
	public long prepareMaxTime = 12_000; // 12s 必须大于Agent所在的数据库配置。DatabaseConf.PrepareMaxTime。

	@Override
	public String getName() {
		return "Dbh2";
	}

	public long getPrepareMaxTime() {
		return prepareMaxTime;
	}

	@Override
	public void parse(Element self) {

	}
}
