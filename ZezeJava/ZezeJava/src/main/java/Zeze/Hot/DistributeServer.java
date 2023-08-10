package Zeze.Hot;

import java.util.HashMap;
import Zeze.Transaction.Bean;
import Zeze.Util.Reflect;

public class DistributeServer {
	private final static HashMap<String, Bean> beans = new HashMap<>();
	public static void main(String [] args) throws Exception {
		String solution = null;
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("solution"))
				solution = args[++i];
		}
		if (null == solution)
			throw new RuntimeException("-solution must present.");

		for (var path : Reflect.collectClassPaths(ClassLoader.getSystemClassLoader())) {
			if (path.startsWith(solution)) {
				var cls = Class.forName(path);
				if (Bean.class.isAssignableFrom(cls)) // is bean
					beans.put(cls.getName(), (Bean)cls.getConstructor().newInstance());
			}
		}


	}
}
