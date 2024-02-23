package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.ArrayList;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import com.alibaba.druid.pool.DruidDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

public final class DatabaseMySql extends DatabaseJdbc {
	public DatabaseMySql(Application zeze, DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesMySql());
	}

	@Override
	public Database.Table openTable(String name) {
		return new TableMysql(name);
	}

	public Database.Table openRelationalTable(String name) {
		return new TableMysqlRelational(name);
	}

	public void dropTable(String name) {
		var tTable = getTable(name);
		if (null == tTable)
			return;

		var storage = tTable.getStorage();
		if (null == storage)
			return;

		if (storage.getDatabaseTable() instanceof TableMysql) {
			var dTable = (TableMysql)storage.getDatabaseTable();
			dTable.drop();
		} else if (storage.getDatabaseTable() instanceof TableMysqlRelational) {
			var dTable = (TableMysqlRelational)storage.getDatabaseTable();
			dTable.drop();
		}
	}

	public void dropOperatesProcedures() {
		try (var connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_")) {
				cmd.executeUpdate();
			}
			try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSetInUse_")) {
				cmd.executeUpdate();
			}
			try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeClearInUse_")) {
				cmd.executeUpdate();
			}
			connection.commit();
		} catch (SQLException e) {
			Task.forceThrow(e);
		}
	}

	public static final byte[] keyOfLock = ("Zeze.AtomicOpenDatabase.Flag." + 5284111301429717881L).getBytes(StandardCharsets.UTF_8);

	private final class OperatesMySql implements Operates {
		@Override
		public void setInUse(int localId, String global) {
			while (true) {
				try (var connection = dataSource.getConnection()) {
					connection.setAutoCommit(true);
					try (var cmd = connection.prepareCall("{CALL _ZezeSetInUse_(?, ?, ?)}")) {
						cmd.setInt(1, localId);
						cmd.setBytes(2, global.getBytes(StandardCharsets.UTF_8));
						cmd.registerOutParameter(3, Types.INTEGER);
						cmd.executeUpdate();
						switch (cmd.getInt(3)) {
						case 0:
							return;
						case 1:
							throw new IllegalStateException("Unknown Error");
						case 2:
							throw new IllegalStateException("Instance Exist.");
						case 3:
							throw new IllegalStateException("Insert LocalId Failed");
						case 4:
							throw new IllegalStateException("Global Not Equals");
						case 5:
							throw new IllegalStateException("Insert Global Failed");
						case 6:
							throw new IllegalStateException("Instance Greater Than One But No Global");
						default:
							throw new IllegalStateException("Unknown ReturnValue");
						}
					}
				} catch (SQLException e) {
					if (!e.getMessage().contains("Deadlock"))
						Task.forceThrow(e);
				}
			}
		}

		@Override
		public int clearInUse(int localId, String global) {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeClearInUse_(?, ?, ?)}")) {
					cmd.setInt(1, localId);
					cmd.setBytes(2, global.getBytes(StandardCharsets.UTF_8));
					cmd.registerOutParameter(3, Types.INTEGER);
					cmd.executeUpdate();
					// Clear 不报告错误，直接返回。
					return cmd.getInt(3);
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareStatement("SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?")) {
					cmd.setBytes(1, key.CopyIf());
					try (ResultSet rs = cmd.executeQuery()) {
						if (rs.next()) {
							byte[] value = rs.getBytes(1);
							long version = rs.getLong(2);
							var result = new DataWithVersion();
							result.data = ByteBuffer.Wrap(value);
							result.version = version;
							return result;
						}
						return null;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			if (key.isEmpty())
				throw new IllegalArgumentException("key is empty.");

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeSaveDataWithSameVersion_(?, ?, ?, ?)}")) {
					cmd.setBytes(1, key.CopyIf()); // key
					cmd.setBytes(2, data.CopyIf()); // data
					cmd.registerOutParameter(3, Types.BIGINT); // version (in | out)
					cmd.setLong(3, version);
					cmd.registerOutParameter(4, Types.INTEGER); // return code
					cmd.executeUpdate();
					switch (cmd.getInt(4)) {
					case 0:
						return KV.create(cmd.getLong(3), true);
					case 2:
						return KV.create(0L, false);
					default:
						throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error.");
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		public OperatesMySql() {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String TableDataWithVersion = "CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (" + "\r\n" +
						"                        id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY," + "\r\n" +
						"                        data LONGBLOB NOT NULL," + "\r\n" +
						"                        version bigint NOT NULL" + "\r\n" +
						"                    )";
				try (var cmd = connection.prepareStatement(TableDataWithVersion)) {
					cmd.executeUpdate();
				}
				//noinspection SpellCheckingInspection
				String ProcSaveDataWithSameVersion = "Create procedure _ZezeSaveDataWithSameVersion_ (" + "\r\n" +
						"                        IN    in_id VARBINARY(" + eMaxKeyLength + ")," + "\r\n" +
						"                        IN    in_data LONGBLOB," + "\r\n" +
						"                        INOUT inout_version bigint," + "\r\n" +
						"                        OUT   ReturnValue int" + "\r\n" +
						"                    )" + "\r\n" +
						"                    return_label:begin" + "\r\n" +
						"                        DECLARE oldversionexsit BIGINT;" + "\r\n" +
						"                        DECLARE ROWCOUNT int;" + "\r\n" +
						"\r\n" +
						"                        START TRANSACTION;" + "\r\n" +
						"                        set ReturnValue=1;" + "\r\n" +
						"                        select version INTO oldversionexsit from _ZezeDataWithVersion_ where id=in_id;" + "\r\n" +
						"                        select COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=in_id;" + "\r\n" +
						"                        if ROWCOUNT > 0 then" + "\r\n" +
						"                            if oldversionexsit <> inout_version then" + "\r\n" +
						"                                set ReturnValue=2;" + "\r\n" +
						"                                ROLLBACK;" + "\r\n" +
						"                                LEAVE return_label;" + "\r\n" +
						"                            end if;" + "\r\n" +
						"                            set oldversionexsit = oldversionexsit + 1;" + "\r\n" +
						"                            update _ZezeDataWithVersion_ set data=in_data, version=oldversionexsit where id=in_id;" + "\r\n" +
						"                            select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                            if ROWCOUNT = 1 then" + "\r\n" +
						"                                set inout_version = oldversionexsit;" + "\r\n" +
						"                                set ReturnValue=0;" + "\r\n" +
						"                                COMMIT;" + "\r\n" +
						"                                LEAVE return_label;" + "\r\n" +
						"                            end if;" + "\r\n" +
						"                            set ReturnValue=3;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"\r\n" +
						"                        insert IGNORE into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);" + "\r\n" +
						"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT = 1 then" + "\r\n" +
						"                            set ReturnValue=0;" + "\r\n" +
						"                            COMMIT;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set ReturnValue=4;" + "\r\n" +
						"                        if 1=1 then" + "\r\n" +
						"							 ROLLBACK;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"						 LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcSaveDataWithSameVersion)) {
					cmd.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}
				//noinspection SpellCheckingInspection
				String TableInstances = "CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				try (var cmd = connection.prepareStatement(TableInstances)) {
					cmd.executeUpdate();
				}
				//noinspection SpellCheckingInspection
				String ProcSetInUse = "Create procedure _ZezeSetInUse_ (" + "\r\n" +
						"                        in in_localid int," + "\r\n" +
						"                        in in_global LONGBLOB," + "\r\n" +
						"                        out ReturnValue int" + "\r\n" +
						"                    )" + "\r\n" +
						"                    return_label:begin" + "\r\n" +
						"                        DECLARE currentglobal LONGBLOB;" + "\r\n" +
						"                        declare emptybinary LONGBLOB;" + "\r\n" +
						"                        DECLARE InstanceCount int;" + "\r\n" +
						"                        DECLARE ROWCOUNT int;" + "\r\n" +
						"\r\n" +
						"                        START TRANSACTION;" + "\r\n" +
						"                        set ReturnValue=1;" + "\r\n" +
						"                        if exists (select localid from _ZezeInstances_ where localid=in_localid) then" + "\r\n" +
						"                            set ReturnValue=2;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        insert IGNORE into _ZezeInstances_ values(in_localid);" + "\r\n" +
						"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT = 0 then" + "\r\n" +
						"                            set ReturnValue=3;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set emptybinary = BINARY '';" + "\r\n" +
						"                        select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" +
						"                        select COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" +
						"                        if ROWCOUNT > 0 then" + "\r\n" +
						"                            if currentglobal <> in_global then" + "\r\n" +
						"                                set ReturnValue=4;" + "\r\n" +
						"                                ROLLBACK;" + "\r\n" +
						"                                LEAVE return_label;" + "\r\n" +
						"                            end if;" + "\r\n" +
						"                        else" + "\r\n" +
						// 忽略这一行的操作结果，当最后一个实例退出的时候，这条记录会被删除。不考虑退出和启动的并发了？
						"                            insert IGNORE into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set InstanceCount=0;" + "\r\n" +
						"                        select count(*) INTO InstanceCount from _ZezeInstances_;" + "\r\n" +
						"                        if InstanceCount = 1 then" + "\r\n" +
						"                            set ReturnValue=0;" + "\r\n" +
						"                            COMMIT;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                       end if;" + "\r\n" +
						"                       if LENGTH(in_global)=0 then" + "\r\n" +
						"                            set ReturnValue=6;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set ReturnValue=0;" + "\r\n" +
						"                        if 1=1 then" + "\r\n" +
						"							 COMMIT;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"						 LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcSetInUse)) {
					cmd.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}
				//noinspection SpellCheckingInspection
				String ProcClearInUse = "Create procedure _ZezeClearInUse_ (" + "\r\n" +
						"                        in in_localid int," + "\r\n" +
						"                        in in_global LONGBLOB," + "\r\n" +
						"                        out ReturnValue int" + "\r\n" +
						"                    )" + "\r\n" +
						"                    return_label:begin" + "\r\n" +
						"                        DECLARE InstanceCount int;" + "\r\n" +
						"                        declare emptybinary LONGBLOB;" + "\r\n" +
						"                        DECLARE ROWCOUNT INT;" + "\r\n" +
						"\r\n" +
						"                        START TRANSACTION;" + "\r\n" +
						"                        set ReturnValue=1;" + "\r\n" +
						"                        delete from _ZezeInstances_ where localid=in_localid;" + "\r\n" +
						"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT = 0 then" + "\r\n" +
						"                            set ReturnValue=2;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set InstanceCount=0;" + "\r\n" +
						"                        select count(*) INTO InstanceCount from _ZezeInstances_;" + "\r\n" +
						"                        if InstanceCount = 0 then" + "\r\n" +
						"                            set emptybinary = BINARY '';" + "\r\n" +
						"                            delete from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set ReturnValue=0;" + "\r\n" +
						"                        if 1=1 then" + "\r\n" +
						"							 COMMIT;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"						 LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcClearInUse)) {
					cmd.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public boolean tryLock() {
			// 总是尝试创建记录，忽略已经存在。
			var createRecord = "insert IGNORE into _ZezeDataWithVersion_ values(?, ?, ?)";
			var lockSql = "update _ZezeDataWithVersion_ set version=1 where id=? and version=0";
			try (var conn = dataSource.getConnection()) {
				try (var pre = conn.prepareStatement(createRecord)) {
					pre.setBytes(1, keyOfLock);
					pre.setBytes(2, ByteBuffer.Empty);
					pre.setLong(3, 0);
					pre.executeUpdate();
				}
				try (var pre = conn.prepareStatement(lockSql)) {
					pre.setBytes(1, keyOfLock);
					return 1 == pre.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return false; // never run here
			}
		}

		@Override
		public void unlock() {
			var unlockSql = "update _ZezeDataWithVersion_ set version=0 where id=?";
			try (var conn = dataSource.getConnection()) {
				try (var pre = conn.prepareStatement(unlockSql)) {
					pre.setBytes(1, keyOfLock);
					pre.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}
	}

	private static void setParams(PreparedStatement pre, int start, ArrayList<Object> params) throws SQLException {
		for (int i = 0; i < params.size(); ++i) {
			var p = params.get(i);
			if (p instanceof String)
				pre.setString(i + start, (String)p);
			else
				pre.setBytes(i + start, ((Zeze.Net.Binary)p).toBytes());
		}
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeCallback(TableX<K, V> table, ResultSet rs, TableWalkHandle<K, V> callback, OutObject<K> outKey)
			throws SQLException {
		K k = table.decodeKeyResultSet(rs);
		if (null != outKey)
			outKey.value = k;
		var r = table.getCache().get(k);
		if (r != null) {
			V v = null;
			r.enterFairLock();
			try {
				if (r.getState() == StateShare || r.getState() == StateModify) {
					// 拥有正确的状态：
					v = r.copyValue();
					if (v == null)
						return true; // 已经被删除，但是还没有checkpoint的记录看不到。返回true，继续循环。
				}
			} finally {
				r.exitFairLock();
			}
			// 从cache中copy成功
			if (v != null)
				return callback.handle(k, v);
			// else GlobalCacheManager.StateInvalid
			// 继续后面的处理：使用数据库中的数据。
		}
		// 缓存中不存在或者正在被删除，使用数据库中的数据。
		var value = table.newValue();
		var parents = new ArrayList<String>();
		value.decodeResultSet(parents, rs);
		return callback.handle(k, value);
	}

	private static <K extends Comparable<K>, V extends Bean>
	String buildOrder(TableX<K, V> table) {
		// 目前考虑keyColumns让Schemas来构造，注意生成顺序最好和encodeKeySQLStatement,decodeKeyResultSet【最好一致】。
		return " ORDER BY " + table.getRelationalTable().currentKeyColumns;
	}

	private static <K extends Comparable<K>, V extends Bean>
	String buildOrderByDesc(TableX<K, V> table) {
		// 目前考虑keyColumns让Schemas来构造，注意生成顺序最好和encodeKeySQLStatement,decodeKeyResultSet【最好一致】。
		var orderBy = table.getRelationalTable().currentKeyColumns;
		orderBy = orderBy.replace(",", " DESC,");
		return " ORDER BY " + orderBy + " DESC"; // last desc
	}

	private static <K extends Comparable<K>, V extends Bean>
	String buildKeyWhere(TableX<K, V> table, SQLStatement st, K exclusiveStartKey, boolean asc) {
		if (null == exclusiveStartKey)
			return "";

		table.encodeKeySQLStatement(st, exclusiveStartKey);
		var where = st.sql.toString();
		where = where.replace(",", " AND ");
		where = where.replace('=', asc ? '>' : '<');
		return " WHERE " + where;
	}

	private static String buildKeyWhere(SQLStatement st) {
		var where = st.sql.toString();
		where = where.replace(",", " AND ");
		return where;
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeKeyCallback(TableX<K, V> table, ResultSet rs, TableWalkKey<K> callback, OutObject<K> outKey)
			throws SQLException {
		K k = table.decodeKeyResultSet(rs);
		if (outKey != null)
			outKey.value = k;
		var r = table.getCache().get(k);
		if (r != null) {
			r.enterFairLock();
			try {
				if (r.getState() == StateShare || r.getState() == StateModify) {
					// 拥有正确的状态：
					if (!r.containsValue())
						return true; // 已经被删除，但是还没有checkpoint的记录看不到。返回true，继续循环。
				}
				// else GlobalCacheManager.StateInvalid
				// 继续后面的处理：使用数据库中的数据。
			} finally {
				r.exitFairLock();
			}
		}
		// 缓存中不存在或者正在被删除，使用数据库中的数据。
		return callback.handle(k);
	}

	public static boolean tableAlreadyExistsWarning(SQLWarning warning) {
		while (warning != null) {
			var msg = warning.getMessage();
			if (msg.startsWith("Table") && msg.contains("already exists"))
				return true;
			warning = warning.getNextWarning();
		}
		return false;
	}

	public final class TableMysqlRelational implements Database.Table {
		private final String name;
		private boolean isNew;
		private boolean dropped = false;

		@Override
		public void waitReady() {
		}

		public void drop() {
			if (dropped)
				return;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "DROP TABLE IF EXISTS " + name;
				try (var cmd = connection.prepareStatement(sql)) {
					dropped = true; // set flag before real drop.
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				dropped = false; // rollback
				Task.forceThrow(e);
			}
		}

		@Override
		public DatabaseMySql getDatabase() {
			return DatabaseMySql.this;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		public TableMysqlRelational(String name) {
			this.name = name;

			/*
			if (name.equals("demo_Module1_Table1") || name.equals("demo_Module1_Table2")) {
				System.out.println("new " + name);
			}
			*/
			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			// 下面的create table if not exists 在存在的时候会返回warning，isNew是否可以通过这个方法得到？
			// warning的方案的原子性由数据库保证，比较好，但warning本身可能不是很标准，先保留MetaData方案了。
			isNew = true;
			/*
			try (var connection = dataSource.getConnection()) {
				DatabaseMetaData meta = connection.getMetaData();
				ResultSet resultSet = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = !resultSet.next();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			*/

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				var table = getDatabase().getTable(name);
				if (table == null)
					throw new IllegalStateException("not found table: " + name);
				String sql = table.getRelationalTable().createTableSql();
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
					isNew = !tableAlreadyExistsWarning(cmd.getWarnings());
				}
			} catch (SQLException e) {
				if (!e.getMessage().contains("already exist"))
					Task.forceThrow(e);
				isNew = false;
			}
		}

		public void tryAlter() {
			/*
			if (name.equals("demo_Module1_Table1") || name.equals("demo_Module1_Table2")) {
				System.out.println(name);
			}
			*/
			if (isNew)
				return; // 已经是最新的表。不需要alter。

			var table = getDatabase().getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			var r = table.getRelationalTable();
			if (r.add.isEmpty() && r.remove.isEmpty() && r.change.isEmpty())
				return; // do nothing

			var sb = new StringBuilder();
			sb.append("ALTER TABLE ").append(name);
			var first = true;
			for (var c : r.add) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(" ADD COLUMN ").append(c.name).append(" ").append(c.sqlType);
			}
			for (var c : r.remove) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(" DROP COLUMN ").append(c.name);
			}
			for (var c : r.change) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(" CHANGE COLUMN ").append(c.change.name).append(" ")
						.append(c.name).append(" ").append(c.sqlType);
			}
			sb.append(", DROP PRIMARY KEY, ADD PRIMARY KEY (").append(r.currentKeyColumns).append(")");

			//System.out.println(sb);
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var pre = conn.prepareStatement(sb.toString())) {
					pre.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> V find(TableX<K, V> table, Object key) {
			if (dropped)
				return null;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT * FROM " + name + " WHERE " + buildKeyWhere(st);
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var pre = conn.prepareStatement(sql)) {
					setParams(pre, 1, st.params);
					try (var rs = pre.executeQuery()) {
						if (!rs.next())
							return null;
						var value = table.newValue();
						var parents = new ArrayList<String>();
						value.decodeResultSet(parents, rs);
						return value;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.select", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> boolean containsKey(TableX<K, V> table, Object key) {
			if (dropped)
				return false;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT * FROM " + name + " WHERE " + buildKeyWhere(st);
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var pre = conn.prepareStatement(sql)) {
					setParams(pre, 1, st.params);
					try (var rs = pre.executeQuery()) {
						return rs.next();
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return false; // never run here
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.select", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(Transaction t, Object key, Object value) {
			if (dropped)
				return;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var stValue = (SQLStatement)value;
			var sql = "REPLACE " + name + " SET " + stKey.sql + ", " + stValue.sql;
			var my = (JdbcTrans)t;
			try (var pre = my.Connection.prepareStatement(sql)) {
				setParams(pre, 1, stKey.params);
				setParams(pre, stKey.params.size() + 1, stValue.params);
				pre.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.replace", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void remove(Transaction t, Object key) {
			if (dropped)
				return;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var sql = "DELETE FROM " + name + " WHERE " + buildKeyWhere(stKey);
			var my = (JdbcTrans)t;
			try (var pre = my.Connection.prepareStatement(sql)) {
				setParams(pre, 1, stKey.params);
				pre.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.delete", System.nanoTime() - timeBegin);
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback, String orderBy) {
			if (dropped)
				return 0;

			var sql = "SELECT * FROM " + name + orderBy;
			try (var conn = dataSource.getConnection();
				 var pre = conn.prepareStatement(sql);
				 var rs = pre.executeQuery()) {
				var count = 0L;
				while (rs.next()) {
					count++;
					if (!invokeCallback(table, rs, callback, null))
						break;
				}
				return count;
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback, String orderBy) {
			if (dropped)
				return 0;

			String sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + name + orderBy;
			try (var conn = dataSource.getConnection();
				 var pre = conn.prepareStatement(sql);
				 var rs = pre.executeQuery()) {
				var count = 0L;
				while (rs.next()) {
					count++;
					if (!invokeKeyCallback(table, rs, callback, null))
						break;
				}
				return count;
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walk(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walk(table, callback, buildOrderByDesc(table)); // 反序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) {
			return walkKey(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) {
			return walkKey(table, callback, buildOrderByDesc(table)); // 反序
		}

		private <K extends Comparable<K>, V extends Bean>
		K walk(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
			   TableWalkHandle<K, V> callback, String orderBy, boolean asc) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				var st = new SQLStatement();
				var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
				String sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					setParams(cmd, 1, st.params);
					cmd.setInt(st.params.size() + 1, proposeLimit);
					var lastKey = new OutObject<K>();
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							if (!invokeCallback(table, rs, callback, lastKey))
								break;
						}
					}
					return lastKey.value;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		K walkKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
				  TableWalkKey<K> callback, String orderBy, boolean asc) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				var st = new SQLStatement();
				var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
				String sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
						+ keyWhere + orderBy + " LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					setParams(cmd, 1, st.params);
					cmd.setInt(st.params.size() + 1, proposeLimit);
					var lastKey = new OutObject<K>();
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							if (!invokeKeyCallback(table, rs, callback, lastKey))
								break;
						}
					}
					return lastKey.value;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walk(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkHandle<K, V> callback) {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
				   TableWalkHandle<K, V> callback) {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(TableX<K, V> table, K exclusiveStartKey, int proposeLimit, TableWalkKey<K> callback) {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback, String orderBy) {
			if (dropped)
				return 0;

			var sql = "SELECT * FROM " + name + orderBy;
			try (var conn = dataSource.getConnection()) {
				try (var pre = conn.prepareStatement(sql)) {
					try (var rs = pre.executeQuery()) {
						var count = 0L;
						while (rs.next()) {
							count++;
							var key = table.decodeKeyResultSet(rs);
							var value = table.newValue();
							var parents = new ArrayList<String>();
							value.decodeResultSet(parents, rs);
							if (!callback.handle(key, value))
								break;
						}
						return count;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback, String orderBy) {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName() + orderBy;
				try (var cmd = connection.prepareStatement(sql)) {
					var count = 0L;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							count++;
							var key = table.decodeKeyResultSet(rs);
							if (!callback.handle(key))
								break;
						}
					}
					return count;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walkDatabase(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) {
			return walkDatabase(table, callback, buildOrderByDesc(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkDatabaseKey(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) {
			return walkDatabaseKey(table, callback, buildOrderByDesc(table));
		}

		private <K extends Comparable<K>, V extends Bean>
		K walkDatabase(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
					   TableWalkHandle<K, V> callback, String orderBy, boolean asc) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				var st = new SQLStatement();
				var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
				String sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					setParams(cmd, 1, st.params);
					cmd.setInt(st.params.size() + 1, proposeLimit);
					var lastKey = new OutObject<K>();
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							var key = table.decodeKeyResultSet(rs);
							var value = table.newValue();
							var parents = new ArrayList<String>();
							value.decodeResultSet(parents, rs);
							lastKey.value = key;
							if (!callback.handle(key, value))
								break;
						}
					}
					return lastKey.value;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		K walkDatabaseKey(TableX<K, V> table, K exclusiveStartKey, int proposeLimit,
						  TableWalkKey<K> callback, String orderBy, boolean asc) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				var st = new SQLStatement();
				var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
				String sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
						+ keyWhere + orderBy + " LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					setParams(cmd, 1, st.params);
					cmd.setInt(st.params.size() + 1, proposeLimit);
					var lastKey = new OutObject<K>();
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							var key = table.decodeKeyResultSet(rs);
							lastKey.value = key;
							if (!callback.handle(key))
								break;
						}
					}
					return lastKey.value;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabase(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
					   @NotNull TableWalkHandle<K, V> callback) {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						   @NotNull TableWalkHandle<K, V> callback) {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						  @NotNull TableWalkKey<K> callback) {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDatabaseKeyDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							  @NotNull TableWalkKey<K> callback) {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public void close() {
		}

		@Override
		public long getSize() {
			if (dropped)
				return -1;
			return queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			if (dropped)
				return -1;
			return queryLong1(dataSource,
					"SELECT TABLE_ROWS FROM information_schema.tables where TABLE_SCHEMA="
							+ name + " AND TABLE_NAME=" + name);
		}
	}

	public final class TableMysql extends Database.AbstractKVTable {
		private final String name;
		private final String sqlFind, sqlRemove, sqlReplace;
		private boolean isNew;
		private boolean dropped = false;

		public void drop() {
			if (dropped)
				return;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "DROP TABLE IF EXISTS " + name;
				try (var cmd = connection.prepareStatement(sql)) {
					dropped = true; // set flag before real drop.
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				dropped = false; // rollback
				Task.forceThrow(e);
			}
		}

		@Override
		public DatabaseMySql getDatabase() {
			return DatabaseMySql.this;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		public TableMysql(String name) {
			this.name = name;

			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			// 下面的create table if not exists 在存在的时候会返回warning，isNew是否可以通过这个方法得到？
			// warning的方案的原子性由数据库保证，比较好，但warning本身可能不是很标准，先保留MetaData方案了。
			isNew = true;
			/*
			try (var connection = dataSource.getConnection()) {
				DatabaseMetaData meta = connection.getMetaData();
				ResultSet resultSet = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = !resultSet.next();
			} catch (SQLException e) {
				Task.forceThrow(e);
				throw new AssertionError(); // never run here
			}
			*/

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "CREATE TABLE IF NOT EXISTS " + name
						+ "(id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
					isNew = !tableAlreadyExistsWarning(cmd.getWarnings());
				}
			} catch (SQLException e) {
				if (!e.getMessage().contains("already exist"))
					Task.forceThrow(e);
				isNew = false;
			}

			sqlFind = "SELECT value FROM " + name + " WHERE id=?";
			sqlRemove = "DELETE FROM " + name + " WHERE id=?";
			sqlReplace = "REPLACE INTO " + name + " values(?,?)";
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			if (dropped)
				return null;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				// 是否可以重用 SqlCommand
				try (var cmd = connection.prepareStatement(sqlFind)) {
					cmd.setBytes(1, key.CopyIf());
					try (var rs = cmd.executeQuery()) {
						return rs.next() ? ByteBuffer.Wrap(rs.getBytes(1)) : null;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.select", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			if (dropped)
				return;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var cmd = ((JdbcTrans)t).Connection.prepareStatement(sqlRemove)) {
				cmd.setBytes(1, key.CopyIf());
				cmd.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.delete", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			if (dropped)
				return;

			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			try (var cmd = ((JdbcTrans)t).Connection.prepareStatement(sqlReplace)) {
				cmd.setBytes(1, key.CopyIf());
				cmd.setBytes(2, value.CopyIf());
				cmd.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (PerfCounter.ENABLE_PERF)
					PerfCounter.instance.addRunInfo("MySQL.replace", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			return walk(callback, true);
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			return walkKey(callback, true);
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			return walk(callback, false);
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			return walkKey(callback, false);
		}

		private long walk(TableWalkHandleRaw callback, boolean asc) {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + name;
				if (asc)
					sql += " ORDER BY id";
				else
					sql += " ORDER BY id DESC";
				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							count++;
							byte[] key = rs.getBytes(1);
							byte[] value = rs.getBytes(2);
							if (!callback.handle(key, value)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		private long walkKey(TableWalkKeyRaw callback, boolean asc) {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id FROM " + name;
				if (asc)
					sql += " ORDER BY id";
				else
					sql += " ORDER BY id DESC";
				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							count++;
							byte[] key = rs.getBytes(1);
							if (!callback.handle(key)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return -1; // never run here
			}
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + name
						+ (exclusiveStartKey != null ? " WHERE id > ?" : "") + " ORDER BY id LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					var index = 1;
					if (exclusiveStartKey != null)
						cmd.setBytes(index++, copyIf(exclusiveStartKey));
					cmd.setInt(index, proposeLimit);
					byte[] lastKey = null;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							lastKey = rs.getBytes(1);
							if (!callback.handle(lastKey, rs.getBytes(2)))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id FROM " + name
						+ (exclusiveStartKey != null ? " WHERE id > ?" : "") + " ORDER BY id LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					var index = 1;
					if (exclusiveStartKey != null)
						cmd.setBytes(index++, copyIf(exclusiveStartKey));
					cmd.setInt(index, proposeLimit);
					byte[] lastKey = null;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							lastKey = rs.getBytes(1);
							if (!callback.handle(lastKey))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + name
						+ (exclusiveStartKey != null ? " WHERE id < ?" : "")
						+ " ORDER BY id DESC LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					var index = 1;
					if (exclusiveStartKey != null)
						cmd.setBytes(index++, copyIf(exclusiveStartKey));
					cmd.setInt(index, proposeLimit);
					byte[] lastKey = null;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							lastKey = rs.getBytes(1);
							if (!callback.handle(lastKey, rs.getBytes(2)))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id FROM " + name
						+ (exclusiveStartKey != null ? " WHERE id < ?" : "")
						+ " ORDER BY id DESC LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					var index = 1;
					if (exclusiveStartKey != null)
						cmd.setBytes(index++, copyIf(exclusiveStartKey));
					cmd.setInt(index, proposeLimit);
					byte[] lastKey = null;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							lastKey = rs.getBytes(1);
							if (!callback.handle(lastKey))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public long getSize() {
			if (dropped)
				return -1;
			return queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			if (dropped)
				return -1;
			return queryLong1(dataSource,
					"SELECT TABLE_ROWS FROM information_schema.tables where TABLE_SCHEMA="
							+ name + " AND TABLE_NAME=" + name);
		}
	}

	public static long queryLong1(DruidDataSource dataSource, String sql) {
		var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
		try (var conn = dataSource.getConnection()) {
			conn.setAutoCommit(true);
			try (var pre = conn.prepareStatement(sql)) {
				try (var rs = pre.executeQuery()) {
					if (!rs.next())
						return -1;
					return rs.getLong(1);
				}
			}
		} catch (SQLException e) {
			Task.forceThrow(e);
			return -1; // never run here
		} finally {
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("MySQL.select", System.nanoTime() - timeBegin);
		}
	}
}
