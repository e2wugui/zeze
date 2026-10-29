package Zeze.Transaction;

import Zeze.Config;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TestDatabaseMySql extends JdbcDatabaseContract {
	@Override
	protected Config.DbType backend() {
		return Config.DbType.MySql;
	}
}
