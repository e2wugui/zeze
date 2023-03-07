package Zeze.Dbh2;

import Zeze.Config;
import org.w3c.dom.Element;

public class Dbh2Config implements Config.ICustomize {
	public int serverFastErrorPeriod = 5000;
	public int loginTimeout = 3000;

	@Override
	public String getName() {
		return "Dbh2";
	}

	@Override
	public void parse(Element self) {

	}
}
