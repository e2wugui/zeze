package Zeze;

import org.jetbrains.annotations.NotNull;

public class Tool {
	public static void main(String @NotNull [] args) {
		var zezeXml = "zeze.xml";
		var dropMysqlOperatesProcedures = false;
		var clearOpenDatabaseFlag = false;
		for (String arg : args) {
			if (arg.equals("-dropMysqlOperatesProcedures"))
				dropMysqlOperatesProcedures = true;
			if (arg.equals("-clearOpenDatabaseFlag"))
				clearOpenDatabaseFlag = true;
			else
				zezeXml = arg;
		}

		var config = Config.load(zezeXml);
		if (dropMysqlOperatesProcedures)
			config.dropMysqlOperatesProcedures();
		if (clearOpenDatabaseFlag)
			config.clearOpenDatabaseFlag();
	}
}
