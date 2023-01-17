
package Zege;

import Zeze.Util.Counters;

public class Program {
	public static final Counters counters = new Counters();

	public synchronized static void main(String[] args) throws Exception {
		var conf = "linkd.xml";
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-perf":
				Counters.enable = true;
				break;
			case "-zezeconf":
				conf = args[++i];
				break;
			}
		}

		Zege.App.Instance.Start(conf);
		if (Counters.enable)
			counters.start();
		try {
			Program.class.wait();
		} finally {
			Zege.App.Instance.Stop();
		}
	}
}
