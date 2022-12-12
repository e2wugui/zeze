package Zeze;

public class Tool {
	public static void main(String[] args) {
		var zezeXml = "zeze.xml";
		var dropMysqlOperatesProcedures = false;
		for (String arg : args) {
			if (arg.equals("-dropMysqlOperatesProcedures")) {
				dropMysqlOperatesProcedures = true;
			} else {
				zezeXml = arg;
			}
		}

		var config = Config.load(zezeXml);
		if (dropMysqlOperatesProcedures)
			config.dropMysqlOperatesProcedures();
	}
}
