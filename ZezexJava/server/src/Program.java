
public class Program {
	public static void main(String[] args) throws InterruptedException {
		String srcDirWhenPostBuild = null;
		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
				case "-srcDirWhenPostBuild":
					srcDirWhenPostBuild = args[++i];
					break;
			}
		}
		if (!srcDirWhenPostBuild.equals(null)) {
			Zezex.ModuleRedirect.Instance.setSrcDirWhenPostBuild(srcDirWhenPostBuild);
			Game.App.getInstance().Create();
			if (Zezex.ModuleRedirect.Instance.getHasNewGen()) {
				System.out.println("ModuleRedirect HasNewGen. Please Rebuild Now.");
			}
			Game.App.getInstance().Destroy();
			return;
		}

		Game.App.getInstance().Start(args);
		try {
			while (true) {
				Thread.sleep(1000);
			}
		}
		finally {
			Game.App.getInstance().Stop();
		}
	}
}