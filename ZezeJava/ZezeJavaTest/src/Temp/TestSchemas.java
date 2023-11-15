package Temp;

import Zeze.Services.Daemon;
import demo.App;

public class TestSchemas {
	public static void main(String [] args) throws Exception {
		System.setProperty(Daemon.propertyNameClearInUse, "true");
		App.Instance.Start();
		App.Instance.Stop();
	}
}
