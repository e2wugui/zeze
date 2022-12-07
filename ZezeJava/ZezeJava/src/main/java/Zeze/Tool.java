package Zeze;

public class Tool {
	public static void main(String[] args) {
		var zezeXml = "zeze.xml";
		var dropMysqlOperatesProcedures = false;
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-dropMysqlOperatesProcedures")) {
				dropMysqlOperatesProcedures = true;
			} else {
				zezeXml = args[i];
			}
		}

		var config = Config.load(zezeXml);
		if (dropMysqlOperatesProcedures)
			config.dropMysqlOperatesProcedures();
	}
}
