
package Zege;

import Zeze.Util.Counters;

public class LinkdProgram {
	public static final Counters counters = new Counters();

	public synchronized static void main(String[] args) throws Exception {
		var conf = "linkd.xml";
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-perf":
				Counters.setEnable(true);
				break;
			case "-zezeconf":
				conf = args[++i];
				break;
			}
		}

		Zege.App.Instance.Start(conf);
		if (Counters.isEnable())
			counters.start();
		try {
			LinkdProgram.class.wait();
		} finally {
			Zege.App.Instance.Stop();
		}
	}
}
