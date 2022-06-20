
package Zege;

import Zeze.Util.Counters;

public class Program {
	public static Counters counters = new Counters();

	public synchronized static void main(String[] args) throws Throwable {
		var conf = "linkd.xml";
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-perf":
				counters.Enable = true;
				break;
			case "-zezeconf":
				conf = args[++i];
				break;
			}
		}

		Zege.App.Instance.Start(conf);
		if (counters.Enable)
			counters.start();
		try {
			Program.class.wait();
		} finally {
			Zege.App.Instance.Stop();
		}
	}
}
