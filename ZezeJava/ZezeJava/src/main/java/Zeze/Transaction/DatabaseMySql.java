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
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import com.alibaba.druid.pool.DruidDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

public final class DatabaseMySql extends DatabaseJdbc {
	public static final byte[] keyOfLock =
			("Zeze.AtomicOpenDatabase.Flag." + 5284111301429717881L).getBytes(StandardCharsets.UTF_8);

	public DatabaseMySql(@Nullable Application zeze, @NotNull DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesMySql());
	}

	@Override
	public @NotNull Database.Table openTable(@NotNull String name, int id) {
		return new TableMysql(name);
	}

	public @NotNull Database.Table openRelationalTable(@NotNull String name) {
		return new TableMysqlRelational(name);
	}

	public void dropTable(@NotNull String name) {
		var table = getTable(name);
		if (table != null) {
			var storage = table.getStorage();
			if (storage != null)
				storage.getDatabaseTable().drop();
		}
	}

	public void dropOperatesProcedures() {
		try (var conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_")) {
				ps.executeUpdate();
			}
			try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSetInUse_")) {
				ps.executeUpdate();
			}
			try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeClearInUse_")) {
				ps.executeUpdate();
			}
			conn.commit();
		} catch (SQLException e) {
			Task.forceThrow(e);
		}
	}

	private final class OperatesMySql implements Operates {
		@Override
		public void setInUse(int localId, @NotNull String global) {
			while (true) {
				try (var conn = dataSource.getConnection()) {
					conn.setAutoCommit(true);
					try (var ps = conn.prepareCall("{CALL _ZezeSetInUse_(?,?,?)}")) {
						ps.setInt(1, localId); // in_local_id
						ps.setBytes(2, global.getBytes(StandardCharsets.UTF_8)); // in_global
						ps.registerOutParameter(3, Types.INTEGER); // ret_value
						ps.executeUpdate();
						switch (ps.getInt(3)) {
						case 0:
							return;
						case 1:
							throw new IllegalStateException("Unknown Error");
						case 2:
							throw new IllegalStateException("Instance Exist");
						case 3:
							throw new IllegalStateException("Insert LocalId Failed");
						case 4:
							throw new IllegalStateException("Global Not Equals");
						case 5:
							throw new IllegalStateException("Insert Global Failed");
						case 6:
							throw new IllegalStateException("Instance Greater Than One But No Global");
						default:
							throw new IllegalStateException("Unknown Return Value");
						}
					}
				} catch (SQLException e) {
					if (!e.getMessage().contains("Deadlock"))
						Task.forceThrow(e);
				}
			}
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareCall("{CALL _ZezeClearInUse_(?,?,?)}")) {
					ps.setInt(1, localId); // in_local_id
					ps.setBytes(2, global.getBytes(StandardCharsets.UTF_8)); // in_global
					ps.registerOutParameter(3, Types.INTEGER); // ret_value
					ps.executeUpdate();
					return ps.getInt(3); // Clear 不报告错误，直接返回。
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement("SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?")) {
					ps.setBytes(1, key.CopyIf());
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							var result = new DataWithVersion();
							result.data = ByteBuffer.Wrap(rs.getBytes(1));
							result.version = rs.getLong(2);
							return result;
						}
						return null;
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public @NotNull KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key,
																  @NotNull ByteBuffer data, long version) {
			if (key.isEmpty())
				throw new IllegalArgumentException("key is empty");

			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareCall("{CALL _ZezeSaveDataWithSameVersion_(?,?,?,?)}")) {
					ps.setBytes(1, key.CopyIf()); // in_id
					ps.setBytes(2, data.CopyIf()); // in_data
					ps.registerOutParameter(3, Types.BIGINT); // inout_version
					ps.setLong(3, version);
					ps.registerOutParameter(4, Types.INTEGER); // ret_value
					ps.executeUpdate();
					switch (ps.getInt(4)) {
					case 0:
						return KV.create(ps.getLong(3), true);
					case 2:
						return KV.create(0L, false);
					default:
						throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error");
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		public OperatesMySql() {
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				var tableDataWithVersionSql = "CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_(" +
						"id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY, " +
						"data LONGBLOB NOT NULL, version BIGINT NOT NULL)";
				try (var ps = conn.prepareStatement(tableDataWithVersionSql)) {
					ps.executeUpdate();
				}
				var procSaveDataWithSameVersionSql = "CREATE PROCEDURE _ZezeSaveDataWithSameVersion_(\n" +
						"    IN    in_id VARBINARY(" + eMaxKeyLength + "),\n" +
						"    IN    in_data LONGBLOB,\n" +
						"    INOUT inout_version BIGINT,\n" +
						"    OUT   ret_value INT\n" +
						")\n" +
						"return_label:BEGIN\n" +
						"    DECLARE old_ver BIGINT;\n" +
						"    DECLARE row_count INT;\n" +
						"\n" +
						"    START TRANSACTION;\n" +
						"    SET ret_value=1;\n" +
						"    SELECT version INTO old_ver FROM _ZezeDataWithVersion_ WHERE id=in_id;\n" +
						"    SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=in_id;\n" +
						"    IF row_count > 0 THEN\n" +
						"        IF old_ver <> inout_version THEN\n" +
						"            SET ret_value=2;\n" +
						"            ROLLBACK;\n" +
						"            LEAVE return_label;\n" +
						"        END IF;\n" +
						"        SET old_ver = old_ver + 1;\n" +
						"        UPDATE _ZezeDataWithVersion_ SET data=in_data, version=old_ver WHERE id=in_id;\n" +
						"        SELECT ROW_COUNT() INTO row_count;\n" +
						"        IF row_count = 1 THEN\n" +
						"            SET inout_version = old_ver;\n" +
						"            SET ret_value=0;\n" +
						"            COMMIT;\n" +
						"            LEAVE return_label;\n" +
						"        END IF;\n" +
						"        SET ret_value=3;\n" +
						"        ROLLBACK;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"\n" +
						"    INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(in_id,in_data,inout_version);\n" +
						"    SELECT ROW_COUNT() INTO row_count;\n" +
						"    IF row_count = 1 THEN\n" +
						"        SET ret_value=0;\n" +
						"        COMMIT;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    SET ret_value=4;\n" +
						"    IF 1=1 THEN\n" +
						"        ROLLBACK;\n" +
						"    END IF;\n" +
						"    LEAVE return_label;\n" +
						"END;";
				try (var ps = conn.prepareStatement(procSaveDataWithSameVersionSql)) {
					ps.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}
				//noinspection SpellCheckingInspection
				var tableInstancesSql = "CREATE TABLE IF NOT EXISTS _ZezeInstances_(localid int NOT NULL PRIMARY KEY)";
				try (var ps = conn.prepareStatement(tableInstancesSql)) {
					ps.executeUpdate();
				}
				//noinspection SpellCheckingInspection
				var procSetInUseSql = "CREATE PROCEDURE _ZezeSetInUse_(\n" +
						"    IN  in_local_id INT,\n" +
						"    IN  in_global LONGBLOB,\n" +
						"    OUT ret_value INT\n" +
						")\n" +
						"return_label:BEGIN\n" +
						"    DECLARE cur_global LONGBLOB;\n" +
						"    DECLARE empty_bin LONGBLOB;\n" +
						"    DECLARE instance_count INT;\n" +
						"    DECLARE row_count INT;\n" +
						"\n" +
						"    START TRANSACTION;\n" +
						"    SET ret_value=1;\n" +
						"    IF exists (SELECT localid FROM _ZezeInstances_ WHERE localid=in_local_id) THEN\n" +
						"        SET ret_value=2;\n" +
						"        ROLLBACK;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    INSERT IGNORE INTO _ZezeInstances_ VALUES(in_local_id);\n" +
						"    SELECT ROW_COUNT() INTO row_count;\n" +
						"    IF row_count = 0 THEN\n" +
						"        SET ret_value=3;\n" +
						"        ROLLBACK;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    SET empty_bin = BINARY '';\n" +
						"    SELECT data INTO cur_global FROM _ZezeDataWithVersion_ WHERE id=empty_bin;\n" +
						"    SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=empty_bin;\n" +
						"    IF row_count > 0 THEN\n" +
						"        IF cur_global <> in_global THEN\n" +
						"            SET ret_value=4;\n" +
						"            ROLLBACK;\n" +
						"            LEAVE return_label;\n" +
						"        END IF;\n" +
						"    ELSE\n" +
						// 忽略这一行的操作结果，当最后一个实例退出的时候，这条记录会被删除。不考虑退出和启动的并发了？
						"        INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(empty_bin, in_global, 0);\n" +
						"    END IF;\n" +
						"    SET instance_count=0;\n" +
						"    SELECT count(*) INTO instance_count FROM _ZezeInstances_;\n" +
						"    IF instance_count = 1 THEN\n" +
						"        SET ret_value=0;\n" +
						"        COMMIT;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    IF LENGTH(in_global)=0 THEN\n" +
						"        SET ret_value=6;\n" +
						"        ROLLBACK;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    SET ret_value=0;\n" +
						"    IF 1=1 THEN\n" +
						"        COMMIT;\n" +
						"    END IF;\n" +
						"    LEAVE return_label;\n" +
						"END;";
				try (var ps = conn.prepareStatement(procSetInUseSql)) {
					ps.executeUpdate();
				} catch (SQLException ex) {
					if (!ex.getMessage().contains("already exist"))
						throw ex;
				}
				//noinspection SpellCheckingInspection
				var procClearInUseSql = "CREATE PROCEDURE _ZezeClearInUse_(\n" +
						"    IN  in_local_id int,\n" +
						"    IN  in_global LONGBLOB,\n" +
						"    OUT ret_value int\n" +
						")\n" +
						"return_label:BEGIN\n" +
						"    DECLARE instance_count INT;\n" +
						"    DECLARE empty_bin LONGBLOB;\n" +
						"    DECLARE row_count INT;\n" +
						"\n" +
						"    START TRANSACTION;\n" +
						"    SET ret_value=1;\n" +
						"    DELETE FROM _ZezeInstances_ WHERE localid=in_local_id;\n" +
						"    SELECT ROW_COUNT() INTO row_count;\n" +
						"    IF row_count = 0 THEN\n" +
						"        SET ret_value=2;\n" +
						"        ROLLBACK;\n" +
						"        LEAVE return_label;\n" +
						"    END IF;\n" +
						"    SET instance_count=0;\n" +
						"    SELECT count(*) INTO instance_count FROM _ZezeInstances_;\n" +
						"    IF instance_count = 0 THEN\n" +
						"        SET empty_bin = BINARY '';\n" +
						"        DELETE FROM _ZezeDataWithVersion_ WHERE id=empty_bin;\n" +
						"    END IF;\n" +
						"    SET ret_value=0;\n" +
						"    IF 1=1 THEN\n" +
						"        COMMIT;\n" +
						"    END IF;\n" +
						"    LEAVE return_label;\n" +
						"END;";
				try (var ps = conn.prepareStatement(procClearInUseSql)) {
					ps.executeUpdate();
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
			var createRecordSql = "INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(?,?,?)";
			var lockSql = "UPDATE _ZezeDataWithVersion_ SET version=1 WHERE id=? AND version=0";
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(createRecordSql)) {
					ps.setBytes(1, keyOfLock);
					ps.setBytes(2, ByteBuffer.Empty);
					ps.setLong(3, 0);
					ps.executeUpdate();
				}
				try (var ps = conn.prepareStatement(lockSql)) {
					ps.setBytes(1, keyOfLock);
					return ps.executeUpdate() == 1;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void unlock() {
			var unlockSql = "UPDATE _ZezeDataWithVersion_ SET version=0 WHERE id=?";
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(unlockSql)) {
					ps.setBytes(1, keyOfLock);
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}
	}

	private static void setParams(@NotNull PreparedStatement pre, int start,
								  @NotNull ArrayList<Object> params) throws SQLException {
		for (int i = 0; i < params.size(); ++i) {
			var p = params.get(i);
			if (p instanceof String)
				pre.setString(i + start, (String)p);
			else
				pre.setBytes(i + start, ((Binary)p).toBytes());
		}
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeCallback(@NotNull TableX<K, V> table, @NotNull ResultSet rs, @NotNull TableWalkHandle<K, V> callback,
						   @Nullable OutObject<K> outKey) throws Exception {
		K k = table.decodeKeyResultSet(rs);
		if (outKey != null)
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
		value.decodeResultSet(new ArrayList<>(), rs);
		return callback.handle(k, value);
	}

	private static <K extends Comparable<K>, V extends Bean>
	@NotNull String buildOrder(@NotNull TableX<K, V> table) {
		// 目前考虑keyColumns让Schemas来构造，注意生成顺序最好和encodeKeySQLStatement,decodeKeyResultSet【最好一致】。
		return " ORDER BY " + table.getRelationalTable().currentKeyColumns;
	}

	private static <K extends Comparable<K>, V extends Bean>
	@NotNull String buildOrderByDesc(@NotNull TableX<K, V> table) {
		// 目前考虑keyColumns让Schemas来构造，注意生成顺序最好和encodeKeySQLStatement,decodeKeyResultSet【最好一致】。
		return " ORDER BY " + table.getRelationalTable().currentKeyColumns.replace(",", " DESC,") + " DESC";
	}

	private static <K extends Comparable<K>, V extends Bean>
	@NotNull String buildKeyWhere(@NotNull TableX<K, V> table, @NotNull SQLStatement st, @Nullable K exclusiveStartKey,
								  boolean asc) {
		if (exclusiveStartKey == null)
			return "";

		table.encodeKeySQLStatement(st, exclusiveStartKey);
		return " WHERE " + st.sql.toString().replace(",", " AND ").replace('=', asc ? '>' : '<');
	}

	private static @NotNull String buildKeyWhere(@NotNull SQLStatement st) {
		return st.sql.toString().replace(",", " AND ");
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeKeyCallback(@NotNull TableX<K, V> table, @NotNull ResultSet rs, @NotNull TableWalkKey<K> callback,
							  @Nullable OutObject<K> outKey) throws Exception {
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

	public static boolean tableAlreadyExistsWarning(@Nullable SQLWarning warning) {
		for (; warning != null; warning = warning.getNextWarning()) {
			var msg = warning.getMessage();
			if (msg.startsWith("Table") && msg.contains("already exists"))
				return true;
		}
		return false;
	}

	public final class TableMysqlRelational implements Database.Table {
		private final @NotNull String name;
		private boolean isNew;
		private boolean dropped;

		public TableMysqlRelational(@NotNull String name) {
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
			try (var conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet rs = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = !rs.next();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			*/
			var table = getDatabase().getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			var sql = table.getRelationalTable().createTableSql();
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
					isNew = !tableAlreadyExistsWarning(ps.getWarnings());
				}
			} catch (SQLException e) {
				if (!e.getMessage().contains("already exist"))
					Task.forceThrow(e);
				isNew = false;
			}
		}

		@Override
		public @NotNull DatabaseMySql getDatabase() {
			return DatabaseMySql.this;
		}

		public @NotNull String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public long getSize() {
			return dropped ? -1 : queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			return dropped ? -1 :
					queryLong1(dataSource, "SELECT TABLE_ROWS FROM information_schema.tables WHERE TABLE_SCHEMA="
							+ name + " AND TABLE_NAME=" + name);
		}

		@Override
		public void close() {
		}

		@Override
		public void drop() {
			if (dropped)
				return;

			var sql = "DROP TABLE IF EXISTS " + name;
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					dropped = true; // set flag before real drop.
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				dropped = false; // rollback
				Task.forceThrow(e);
			}
		}

		public void tryAlter() {
			if (isNew) {
				logger.info("tryAlter isNew {}", name);
				return; // 已经是最新的表。不需要alter。
			}
			var table = getDatabase().getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			var r = table.getRelationalTable();
			if (r.add.isEmpty() && r.remove.isEmpty() && r.change.isEmpty()) {
				logger.info("tryAlter no change {}", name);
				return; // do nothing
			}

			var sb = new StringBuilder();
			sb.append("ALTER TABLE ").append(name);
			var first = true;
			for (var c : r.add) {
				if (first)
					first = false;
				else
					sb.append(", ");
				sb.append(" ADD COLUMN ").append(c.name).append(' ').append(c.sqlType);
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
				sb.append(" CHANGE COLUMN ").append(c.change.name).append(' ')
						.append(c.name).append(' ').append(c.sqlType);
			}
			sb.append(", DROP PRIMARY KEY, ADD PRIMARY KEY (").append(r.currentKeyColumns).append(')');
			var sql = sb.toString();
			logger.info("tryAlter {} {}", table.getName(), sql);

			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> @Nullable V find(@NotNull TableX<K, V> table,
																		  @NotNull Object key) {
			if (dropped)
				return null;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT * FROM " + name + " WHERE " + buildKeyWhere(st);
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				try (var rs = ps.executeQuery()) {
					if (!rs.next())
						return null;
					var value = table.newValue();
					value.decodeResultSet(new ArrayList<>(), rs);
					return value;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.SELECT", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> boolean containsKey(@NotNull TableX<K, V> table,
																			 @NotNull Object key) {
			if (dropped)
				return false;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT * FROM " + name + " WHERE " + buildKeyWhere(st);
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				try (var rs = ps.executeQuery()) {
					return rs.next();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.SELECT", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull Object key, @NotNull Object value) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var stValue = (SQLStatement)value;
			var sql = "REPLACE " + name + " SET " + stKey.sql + ", " + stValue.sql;
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sql)) {
				setParams(ps, 1, stKey.params);
				setParams(ps, stKey.params.size() + 1, stValue.params);
				ps.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.REPLACE", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull Object key) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var sql = "DELETE FROM " + name + " WHERE " + buildKeyWhere(stKey);
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sql)) {
				setParams(ps, 1, stKey.params);
				ps.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.DELETE", System.nanoTime() - timeBegin);
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		long walk(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback,
				  @NotNull String orderBy) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT * FROM " + name + orderBy;
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!invokeCallback(table, rs, callback, null))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback,
					 @NotNull String orderBy) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + name + orderBy;
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!invokeKeyCallback(table, rs, callback, null))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, callback, buildOrderByDesc(table)); // 反序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) throws Exception {
			return walkKey(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) throws Exception {
			return walkKey(table, callback, buildOrderByDesc(table)); // 反序
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walk(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						 @NotNull TableWalkHandle<K, V> callback,
						 @NotNull String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				ps.setInt(st.params.size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!invokeCallback(table, rs, callback, lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey.value;
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							@NotNull TableWalkKey<K> callback, @NotNull String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
					+ keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				ps.setInt(st.params.size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!invokeKeyCallback(table, rs, callback, lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey.value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walk(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
			   @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
				   @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
				  @NotNull TableWalkKey<K> callback) throws Exception {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		K walkKeyDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
					  @NotNull TableWalkKey<K> callback) throws Exception {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabase(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback,
						  @NotNull String orderBy) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT * FROM " + name + orderBy;
			var parents = new ArrayList<String>();
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					var key = table.decodeKeyResultSet(rs);
					var value = table.newValue();
					value.decodeResultSet(parents, rs);
					parents.clear();
					if (!callback.handle(key, value))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback,
							 @NotNull String orderBy) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName() + orderBy;
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!callback.handle(table.decodeKeyResultSet(rs)))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(@NotNull TableX<K, V> table, @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, callback, buildOrderByDesc(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(@NotNull TableX<K, V> table, @NotNull TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, callback, buildOrderByDesc(table));
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 @NotNull TableWalkHandle<K, V> callback,
								 @NotNull String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
			var parents = new ArrayList<String>();
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				ps.setInt(st.params.size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						var key = table.decodeKeyResultSet(rs);
						var value = table.newValue();
						value.decodeResultSet(parents, rs);
						parents.clear();
						lastKey.value = key;
						if (!callback.handle(key, value))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey.value;
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									@NotNull TableWalkKey<K> callback,
									@NotNull String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
					+ keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.params);
				ps.setInt(st.params.size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						var key = table.decodeKeyResultSet(rs);
						lastKey.value = key;
						if (!callback.handle(key))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey.value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									 @NotNull TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									@NotNull TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKeyDesc(@NotNull TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
										@NotNull TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}
	}

	public final class TableMysql extends Database.AbstractKVTable {
		private final @NotNull String name;
		private final @NotNull String sqlFind, sqlRemove, sqlReplace;
		private final boolean isNew;
		private boolean dropped;

		public TableMysql(@NotNull String name) {
			this.name = name;
			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			// 下面的create table if not exists 在存在的时候会返回warning，isNew是否可以通过这个方法得到？
			// warning的方案的原子性由数据库保证，比较好，但warning本身可能不是很标准，先保留MetaData方案了。
			var isNew = true;
			/*
			try (var conn = dataSource.getConnection()) {
				DatabaseMetaData meta = conn.getMetaData();
				ResultSet rs = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = !rs.next();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
			*/
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				var sql = "CREATE TABLE IF NOT EXISTS " + name
						+ "(id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)";
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
					isNew = !tableAlreadyExistsWarning(ps.getWarnings());
				}
			} catch (SQLException e) {
				if (!e.getMessage().contains("already exist"))
					Task.forceThrow(e);
				isNew = false;
			}

			sqlFind = "SELECT value FROM " + name + " WHERE id=?";
			sqlRemove = "DELETE FROM " + name + " WHERE id=?";
			sqlReplace = "REPLACE " + name + " VALUE(?,?)";
			this.isNew = isNew;
		}

		@Override
		public @NotNull DatabaseMySql getDatabase() {
			return DatabaseMySql.this;
		}

		public @NotNull String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public long getSize() {
			return dropped ? -1 : queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			return dropped ? -1 :
					queryLong1(dataSource, "SELECT TABLE_ROWS FROM information_schema.tables WHERE TABLE_SCHEMA="
							+ name + " AND TABLE_NAME=" + name);
		}

		@Override
		public void close() {
		}

		@Override
		public void drop() {
			if (dropped)
				return;

			var sql = "DROP TABLE IF EXISTS " + name;
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					dropped = true; // set flag before real drop.
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				dropped = false; // rollback
				Task.forceThrow(e);
			}
		}

		@Override
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			if (dropped)
				return null;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var k = key.CopyIf();
			byte[] v = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sqlFind)) {
				ps.setBytes(1, k);
				try (var rs = ps.executeQuery()) {
					if (rs.next())
						v = rs.getBytes(1);
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.SELECT", System.nanoTime() - timeBegin);
			}
			return v != null ? ByteBuffer.Wrap(v) : null;
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var k = key.CopyIf();
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sqlRemove)) {
				ps.setBytes(1, k);
				ps.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.DELETE", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var k = key.CopyIf();
			var v = value.CopyIf();
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sqlReplace)) {
				ps.setBytes(1, k);
				ps.setBytes(2, v);
				ps.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			} finally {
				if (ZezeCounter.ENABLE_PERF)
					ZezeCounter.instance.addRunTime("MySQL.REPLACE", System.nanoTime() - timeBegin);
			}
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			return walk(callback, true);
		}

		@Override
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			return walkKey(callback, true);
		}

		@Override
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			return walk(callback, false);
		}

		@Override
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			return walkKey(callback, false);
		}

		private long walk(@NotNull TableWalkHandleRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT * FROM " + name + (asc ? " ORDER BY id" : " ORDER BY id DESC");
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!callback.handle(rs.getBytes(1), rs.getBytes(2)))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		private long walkKey(@NotNull TableWalkKeyRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT id FROM " + name + (asc ? " ORDER BY id" : " ORDER BY id DESC");
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!callback.handle(rs.getBytes(1)))
						break;
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return count;
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var sql = "SELECT * FROM " + name + (exclusiveStartKey != null ? " WHERE id>?" : "")
					+ " ORDER BY id LIMIT ?";
			byte[] lastKey = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				var index = 1;
				if (exclusiveStartKey != null)
					ps.setBytes(index++, exclusiveStartKey.CopyIf());
				ps.setInt(index, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						lastKey = rs.getBytes(1);
						if (!callback.handle(lastKey, rs.getBytes(2)))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											@NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var sql = "SELECT id FROM " + name + (exclusiveStartKey != null ? " WHERE id>?" : "")
					+ " ORDER BY id LIMIT ?";
			byte[] lastKey = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				var index = 1;
				if (exclusiveStartKey != null)
					ps.setBytes(index++, exclusiveStartKey.CopyIf());
				ps.setInt(index, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						lastKey = rs.getBytes(1);
						if (!callback.handle(lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var sql = "SELECT * FROM " + name + (exclusiveStartKey != null ? " WHERE id<?" : "")
					+ " ORDER BY id DESC LIMIT ?";
			byte[] lastKey = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				var index = 1;
				if (exclusiveStartKey != null)
					ps.setBytes(index++, exclusiveStartKey.CopyIf());
				ps.setInt(index, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						lastKey = rs.getBytes(1);
						if (!callback.handle(lastKey, rs.getBytes(2)))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			var sql = "SELECT id FROM " + name + (exclusiveStartKey != null ? " WHERE id<?" : "")
					+ " ORDER BY id DESC LIMIT ?";
			byte[] lastKey = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				var index = 1;
				if (exclusiveStartKey != null)
					ps.setBytes(index++, exclusiveStartKey.CopyIf());
				ps.setInt(index, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						lastKey = rs.getBytes(1);
						if (!callback.handle(lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}
	}

	public static long queryLong1(@NotNull DruidDataSource dataSource, @NotNull String sql) {
		var timeBegin = ZezeCounter.ENABLE_PERF ? System.nanoTime() : 0;
		try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql); var rs = ps.executeQuery()) {
			return rs.next() ? rs.getLong(1) : -1;
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		} finally {
			if (ZezeCounter.ENABLE_PERF)
				ZezeCounter.instance.addRunTime("MySQL.SELECT", System.nanoTime() - timeBegin);
		}
	}
}
