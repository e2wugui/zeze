package Zeze;

import org.jetbrains.annotations.NotNull;

/**
 * 运维小工具入口：按参数清理 MySQL 存储过程或清除数据库打开标志。
 */
public class Tool {
	public static void main(String @NotNull [] args) {
		var zezeXml = "zeze.xml";
		var dropMysqlOperatesProcedures = false;
		var clearOpenDatabaseFlag = false;
		for (String arg : args) {
			if (arg.equals("-dropMysqlOperatesProcedures"))
				dropMysqlOperatesProcedures = true;
			else if (arg.equals("-clearOpenDatabaseFlag"))
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
