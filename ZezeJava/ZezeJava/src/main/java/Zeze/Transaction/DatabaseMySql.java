package Zeze.Transaction;

import java.util.Objects;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.StringJoiner;
import java.util.Map;
import java.util.regex.Pattern;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.SQLStatement;
import Zeze.Util.Action1;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import com.alibaba.druid.pool.DruidDataSource;
import org.jspecify.annotations.Nullable;
import static Zeze.Services.GlobalCacheManagerConst.StateModify;
import static Zeze.Services.GlobalCacheManagerConst.StateShare;

/**
 * MySQL 数据库后端：提供 KV 表与关系映射表的 JDBC 实现，
 * 以及存储过程版 Operates（setInUse/clearInUse/同版本保存）与带租期的全局启动锁。
 */
public final class DatabaseMySql extends DatabaseJdbc implements DatabaseRelationalMapping {
	public static final byte[] keyOfLock =
			("Zeze.AtomicOpenDatabase.Flag." + 5284111301429717881L).getBytes(StandardCharsets.UTF_8);

	// 三个 Operates 存储过程 SQL 文本的 SHA-256 版本戳行 key，戳一致时跳过重建。
	public static final byte[] keyOfOperatesProceduresStamp =
			("Zeze.OperatesMySql.Procedures.Stamp." + 7362541089214536742L).getBytes(StandardCharsets.UTF_8);

	// 全表流式 walk 期间临时调大会话 net_write_timeout，防止慢回调期间服务端断连，结束后恢复默认。
	private static final int WALK_NET_WRITE_TIMEOUT_SECONDS = 600;

	private long walkStreamed(String sql, JdbcWalkRow row) throws Exception {
		try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
			var connectionId = queryConnectionId(conn); // 须在流式读取开始前获取连接号
			execSql(conn, "SET SESSION net_write_timeout=" + WALK_NET_WRITE_TIMEOUT_SECONDS);
			try {
				ps.setFetchSize(Integer.MIN_VALUE);
				var rs = ps.executeQuery();
				var count = 0L;
				var abandoned = false;
				try {
					while (rs.next()) {
						count++;
						if (!row.handle(rs)) {
							abandoned = true;
							break;
						}
					}
				} catch (Throwable t) {
					abandoned = true;
					throw t;
				} finally {
					if (abandoned && !killQuery(connectionId))
						// 控制通路失败时直接废弃物理连接，避免 rs.close() 同步排空剩余结果集。
						try {
							conn.abort(Runnable::run);
						} catch (Throwable e) { // logger.warn
							logger.warn("walkStreamed abort after kill fail {} exception:", connectionId, e);
						}
					try {
						rs.close();
					} catch (SQLException e) {
						if (!abandoned)
							//noinspection ThrowFromFinallyBlock
							throw e;
						// 中断路径下 rs.close() 抛错时直接废弃物理连接，防止残留流式状态污染连接池。
						try {
							conn.abort(Runnable::run);
						} catch (Throwable e2) {
							logger.warn("walkStreamed abort after close fail {} exception:", connectionId, e2);
						}
					}
				}
				return count;
			} finally {
				try {
					execSql(conn, "SET SESSION net_write_timeout=DEFAULT");
				} catch (SQLException e) {
					// 连接已失效时忽略，Druid 校验会逐出坏连接。
				}
			}
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		}
	}

	private static long queryConnectionId(Connection conn) throws SQLException {
		try (var st = conn.createStatement(); var rs = st.executeQuery("SELECT CONNECTION_ID()")) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static void execSql(Connection conn, String sql) throws SQLException {
		try (var st = conn.createStatement()) {
			st.execute(sql);
		}
	}

	// 中断流式查询使用独立的懒初始化控制连接池发送 KILL QUERY，避免业务池被并发 walk 占满时互等死锁。
	private static final int KILL_CONTROL_MAX_ACTIVE = 2;
	private static final long KILL_CONTROL_MAX_WAIT_MILLIS = 10_000;
	private volatile @Nullable DruidDataSource killControlDataSource;

	private DruidDataSource getKillControlDataSource() {
		var ds = killControlDataSource;
		if (ds != null)
			return ds;
		synchronized (this) {
			ds = killControlDataSource;
			if (ds == null) {
				var dc = Objects.requireNonNull(getConf().getDruidConf());
				var control = new DruidDataSource();
				control.setUrl(getConf().getDatabaseUrl());
				control.setDriverClassName(dc.driverClassName);
				control.setUsername(dc.userName);
				control.setPassword(dc.password);
				control.setInitialSize(0);
				control.setMinIdle(0);
				control.setMaxActive(KILL_CONTROL_MAX_ACTIVE);
				control.setMaxWait(KILL_CONTROL_MAX_WAIT_MILLIS);
				killControlDataSource = ds = control;
			}
			return ds;
		}
	}

	private boolean killQuery(long connectionId) {
		try (var conn = getKillControlDataSource().getConnection(); var st = conn.createStatement()) {
			st.execute("KILL QUERY " + connectionId);
			return true;
		} catch (SQLException e) {
			logger.warn("walkStreamed killQuery {} failed, fallback to drain-on-close", connectionId, e);
			return false;
		}
	}

	@Override
	public void close() {
		super.close();
		var control = killControlDataSource;
		if (control != null)
			control.close();
	}

	// 过程体修复（重读校验、错误传播等）通过版本戳变化触发DROP+CREATE下发到存量库（见OperatesMySql构造器）。
	static final String PROC_SET_IN_USE_SQL = """
			CREATE PROCEDURE _ZezeSetInUse_(
			    IN  in_local_id INT,
			    IN  in_global LONGBLOB,
			    OUT ret_value INT
			)
			return_label:BEGIN
			    DECLARE cur_global LONGBLOB;
			    DECLARE empty_bin LONGBLOB;
			    DECLARE instance_count INT;
			    DECLARE row_count INT;

			    START TRANSACTION;
			    SET ret_value=1;
			    IF exists (SELECT localid FROM _ZezeInstances_ WHERE localid=in_local_id) THEN
			        SET ret_value=2;
			        ROLLBACK;
			        LEAVE return_label;
			    END IF;
			    INSERT IGNORE INTO _ZezeInstances_ VALUES(in_local_id);
			    SELECT ROW_COUNT() INTO row_count;
			    IF row_count = 0 THEN
			        SET ret_value=3;
			        ROLLBACK;
			        LEAVE return_label;
			    END IF;
			    SET empty_bin = BINARY '';
			    SELECT data INTO cur_global FROM _ZezeDataWithVersion_ WHERE id=empty_bin;
			    SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=empty_bin;
			    IF row_count > 0 THEN
			        IF cur_global <> in_global THEN
			            SET ret_value=4;
			            ROLLBACK;
			            LEAVE return_label;
			        END IF;
			    ELSE
			        INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(empty_bin, in_global, 0);
			        SELECT ROW_COUNT() INTO row_count;
			        IF row_count = 0 THEN
			            -- 并发首启动：事务快照看不到对方未提交的行，INSERT IGNORE被静默忽略。
			            -- 锁定读取最新已提交版本比较（REPEATABLE READ下普通读仍是旧快照，
			            -- 检不出对方提交的global，不同global的两实例会双双启动成功）。
			            SELECT data INTO cur_global FROM _ZezeDataWithVersion_ WHERE id=empty_bin FOR UPDATE;
			            SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=empty_bin FOR UPDATE;
			            IF row_count > 0 THEN
			                IF cur_global <> in_global THEN
			                    SET ret_value=4;
			                    ROLLBACK;
			                    LEAVE return_label;
			                END IF;
			            ELSE
			                SET ret_value=5;
			                ROLLBACK;
			                LEAVE return_label;
			            END IF;
			        END IF;
			    END IF;
			    SET instance_count=0;
			    -- 锁定读取最新已提交实例数：REPEATABLE READ下普通读沿用事务初建的旧快照，
			    -- 并发首启的无GCM两实例彼此不可见，会双双读到instance_count=1而通过校验。
			    SELECT count(*) INTO instance_count FROM _ZezeInstances_ FOR UPDATE;
			    IF instance_count = 1 THEN
			        SET ret_value=0;
			        COMMIT;
			        LEAVE return_label;
			    END IF;
			    IF LENGTH(in_global)=0 THEN
			        SET ret_value=6;
			        ROLLBACK;
			        LEAVE return_label;
			    END IF;
			    SET ret_value=0;
			    IF 1=1 THEN
			        COMMIT;
			    END IF;
			    LEAVE return_label;
			END;""";

	// 驱动异常消息可能为 null，统一做空安全子串匹配。
	private static boolean sqlMessageContains(SQLException e, String token) {
		var msg = e.getMessage();
		return msg != null && msg.contains(token);
	}

	private static final ZezeCounter.LabeledObserverCreator mysqlObserverCreator
			= ZezeCounter.instance.allocRunTimeObserverCreator("mysql_operation", "operation");

	private static final ZezeCounter.LongObserver mysqlSelectCounter = mysqlObserverCreator.labelValues("select");
	private static final ZezeCounter.LongObserver mysqlDeleteCounter = mysqlObserverCreator.labelValues("delete");
	private static final ZezeCounter.LongObserver mysqlReplaceCounter = mysqlObserverCreator.labelValues("replace");
	private static final Pattern SPLIT_PATTERN = Pattern.compile(", ");

	// 关系映射表 string key 列建表即固定为 VARCHAR(256)（见下方 getKeyStringType 的二进制排序规则覆写）。
	private static final int eMaxKeyStringLength = 256;

	// 另两个过程体SQL文本常量（_ZezeSetInUse_是PROC_SET_IN_USE_SQL），三者一起参与版本戳计算。
	private static final String PROC_SAVE_DATA_WITH_SAME_VERSION_SQL = """
			CREATE PROCEDURE _ZezeSaveDataWithSameVersion_(
			    IN    in_id VARBINARY(%d),
			    IN    in_data LONGBLOB,
			    INOUT inout_version BIGINT,
			    OUT   ret_value INT
			)
			return_label:BEGIN
			    DECLARE old_ver BIGINT;
			    DECLARE row_count INT;

			    START TRANSACTION;
			    SET ret_value=1;
			    SELECT version INTO old_ver FROM _ZezeDataWithVersion_ WHERE id=in_id;
			    SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=in_id;
			    IF row_count > 0 THEN
			        IF old_ver <> inout_version THEN
			            SET ret_value=2;
			            ROLLBACK;
			            LEAVE return_label;
			        END IF;
			        SET old_ver = old_ver + 1;
			        -- 更新带版本条件：快照读到更新的窗口内并发写者可双双通过前置校验，
			        -- 无条件UPDATE让后者覆盖前者且双双报成功（丢失更新）。
			        UPDATE _ZezeDataWithVersion_ SET data=in_data, version=old_ver
			            WHERE id=in_id AND version=inout_version;
			        SELECT ROW_COUNT() INTO row_count;
			        IF row_count = 1 THEN
			            SET inout_version = old_ver;
			            SET ret_value=0;
			            COMMIT;
			            LEAVE return_label;
			        END IF;
			        SET ret_value=3;
			        ROLLBACK;
			        LEAVE return_label;
			    END IF;

			    INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(in_id,in_data,inout_version);
			    SELECT ROW_COUNT() INTO row_count;
			    IF row_count = 1 THEN
			        SET ret_value=0;
			        COMMIT;
			        LEAVE return_label;
			    END IF;
			    SET ret_value=4;
			    IF 1=1 THEN
			        ROLLBACK;
			    END IF;
			    LEAVE return_label;
			END;""".formatted(eMaxKeyLength);

	static final String PROC_CLEAR_IN_USE_SQL = """
			CREATE PROCEDURE _ZezeClearInUse_(
			    IN  in_local_id int,
			    IN  in_global LONGBLOB,
			    OUT ret_value int
			)
			return_label:BEGIN
			    DECLARE instance_count INT;
			    DECLARE empty_bin LONGBLOB;
			    DECLARE row_count INT;

			    START TRANSACTION;
			    SET ret_value=1;
			    SET empty_bin = BINARY '';
			    -- 先锁定 empty_bin 行串行化并发 clearInUse，并锁定计数 _ZezeInstances_，
			    -- 避免最后两个实例在 REPEATABLE READ 快照读下各看到 instance_count=1 而残留 empty_bin。
			    SELECT COUNT(*) INTO row_count FROM _ZezeDataWithVersion_ WHERE id=empty_bin FOR UPDATE;
			    DELETE FROM _ZezeInstances_ WHERE localid=in_local_id;
			    SET instance_count=0;
			    SELECT count(*) INTO instance_count FROM _ZezeInstances_ FOR UPDATE;
			    IF instance_count = 0 THEN
			        DELETE FROM _ZezeDataWithVersion_ WHERE id=empty_bin;
			    END IF;
			    SET ret_value=0;
			    IF 1=1 THEN
			        COMMIT;
			    END IF;
			    LEAVE return_label;
			END;""";

	// 版本戳=三个过程体SQL文本的SHA-256（hex）。过程体任何修改都改变戳，从而触发一次DROP+CREATE。
	static String calcOperatesProceduresStamp() {
		var text = PROC_SAVE_DATA_WITH_SAME_VERSION_SQL + "\0" + PROC_SET_IN_USE_SQL + "\0" + PROC_CLEAR_IN_USE_SQL;
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 unavailable", e); // JVM规范保证存在，不可达
		}
	}

	// 戳的存取走直连SQL而非存储过程：首启/升级时过程可能尚不存在（鸡生蛋）。
	private static @Nullable String getOperatesProceduresStamp(Connection conn) throws SQLException {
		try (var ps = conn.prepareStatement("SELECT data FROM _ZezeDataWithVersion_ WHERE id=?")) {
			ps.setBytes(1, keyOfOperatesProceduresStamp);
			try (var rs = ps.executeQuery()) {
				return rs.next() ? new String(rs.getBytes(1), StandardCharsets.UTF_8) : null;
			}
		}
	}

	private static void writeOperatesProceduresStamp(Connection conn, String stamp) throws SQLException {
		try (var ps = conn.prepareStatement("REPLACE INTO _ZezeDataWithVersion_ VALUES(?,?,?)")) {
			ps.setBytes(1, keyOfOperatesProceduresStamp);
			ps.setBytes(2, stamp.getBytes(StandardCharsets.UTF_8));
			ps.setLong(3, 0);
			ps.executeUpdate();
		}
	}

	public DatabaseMySql(@Nullable Application zeze, DatabaseConf conf) {
		super(zeze, conf);
		initializeDirectOperates(() -> conf.isDisableOperates() ? new NullOperates() : new OperatesMySql());
	}

	private static final Map<String, String> sqlTypeTable = new HashMap<>();

	static {
		sqlTypeTable.put("bool", "BOOL");
		sqlTypeTable.put("byte", "TINYINT");
		sqlTypeTable.put("short", "SMALLINT");
		sqlTypeTable.put("int", "INT");
		sqlTypeTable.put("long", "BIGINT");
		sqlTypeTable.put("float", "FLOAT");
		sqlTypeTable.put("double", "DOUBLE");
		sqlTypeTable.put("binary", "BLOB");
		sqlTypeTable.put("string", "TEXT");
		// json
		sqlTypeTable.put("dynamic", "TEXT");
		sqlTypeTable.put("list", "TEXT");
		sqlTypeTable.put("array", "TEXT");
		sqlTypeTable.put("set", "TEXT");
		sqlTypeTable.put("map", "TEXT");
		sqlTypeTable.put("sortedmap", "TEXT");
		sqlTypeTable.put("gtable", "TEXT");
		// 下面的类型会被展开，这里的类型展开后的实际类型。
		sqlTypeTable.put("vector2", "FLOAT");
		sqlTypeTable.put("vector2int", "INT");
		sqlTypeTable.put("vector3", "FLOAT");
		sqlTypeTable.put("vector3int", "INT");
		sqlTypeTable.put("vector4", "FLOAT");
		sqlTypeTable.put("quaternion", "FLOAT");
	}

	@Override
	public Map<String, String> getSqlTypeMap() {
		return sqlTypeTable;
	}

	@Override
	public String getKeyStringType() {
		// 显式指定 utf8mb4 字符集与二进制排序规则，避免默认大小写不敏感合并不同大小写 key。
		return "VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";
	}

	@Override
	public Database.Table openTable(String name, int id) {
		return new TableMysql(name);
	}

	@Override
	public Database.Table openRelationalTable(String name) {
		return new TableMysqlRelational(name);
	}

	@Override
	public void dropTable(String name) {
		var table = getTable(name);
		if (table != null) {
			var storage = table.getStorage();
			if (storage != null)
				storage.getDatabaseTable().drop();
		}
	}

	@Override
	public void renameTable(String tableOldName, String tableNewName) throws Exception {
		// 支持幂等重跑：源不存在而目标已存在视为已完成，均不存在则报错。
		try (var conn = dataSource.getConnection()) {
			conn.setAutoCommit(true);
			if (!tableExists(conn, tableOldName)) {
				if (tableExists(conn, tableNewName))
					return;
				throw new IllegalStateException("renameTable: source table not found: " + tableOldName);
			}
			String sql = "RENAME TABLE " + tableOldName + " TO " + tableNewName;
			try (var ps = conn.prepareStatement(sql)) {
				ps.executeUpdate();
			}
		}
	}

	// renameTable幂等重跑的判定：查当前schema下表是否存在。
	private static boolean tableExists(Connection conn, String name) throws SQLException {
		try (var ps = conn.prepareStatement(
				"SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
			ps.setString(1, name);
			try (var rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	@Override
	public void relationalSql(String sql, Action1<ResultSet> handle) throws Exception {
		try (var conn = dataSource.getConnection()) {
			conn.setAutoCommit(true);
			try (var ps = conn.prepareStatement(sql)) {
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						handle.run(rs);
					}
				}
			}
		}
	}

	public void dropOperatesProcedures() {
		try (var conn = dataSource.getConnection()) {
			conn.setAutoCommit(false);
			dropOperatesProcedures(conn);
			conn.commit();
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		}
	}

	// 复用调用方连接删除三个过程并清版本戳，避免在 maxActive=1 时嵌套借连接死锁。
	private static void dropOperatesProcedures(Connection conn) throws SQLException {
		try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_")) {
			ps.executeUpdate();
		}
		try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSetInUse_")) {
			ps.executeUpdate();
		}
		try (var ps = conn.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeClearInUse_")) {
			ps.executeUpdate();
		}
		// 同步清除版本戳，确保下次启动重新创建过程。
		if (tableExists(conn, "_ZezeDataWithVersion_")) {
			try (var ps = conn.prepareStatement("DELETE FROM _ZezeDataWithVersion_ WHERE id=?")) {
				ps.setBytes(1, keyOfOperatesProceduresStamp);
				ps.executeUpdate();
			}
		}
	}

	private final class OperatesMySql implements Operates {
		// 全局启动锁租期（秒），到期自动可接管。
		private static final int LOCK_LEASE_SECONDS = 600;

		@Override
		public void setInUse(int localId, String global) {
			for (int i = 0; i < 64; ++i) {
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
					if (!sqlMessageContains(e, "Deadlock"))
						throw Task.forceThrow(e);
				}
			}
			throw new IllegalStateException("setInUse Deadlock");
		}

		@Override
		public int clearInUse(int localId, String global) {
			for (int i = 0; i < 64; ++i) {
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
					if (!sqlMessageContains(e, "Deadlock"))
						throw Task.forceThrow(e);
				}
			}
			throw new IllegalStateException("clearInUse Deadlock");
		}

		@Override
		public @Nullable DataWithVersion getDataWithVersion(ByteBuffer key) {
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
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key,
																  ByteBuffer data, long version) {
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
					return switch (ps.getInt(4)) {
						case 0 -> KV.create(ps.getLong(3), true);
						case 2 -> KV.create(0L, false);
						// 3=并发更新版本未命中，4=并发首插重复键，均返回 false 供上层 CAS 重试
						case 3 -> KV.create(0L, false);
						case 4 -> KV.create(0L, false);
						default -> throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error");
					};
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		public OperatesMySql() {
			// MySQL 无 CREATE OR REPLACE PROCEDURE，以三个过程体 SHA-256 版本戳门控，仅变化时重建。
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				var tableDataWithVersionSql = "CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_(" +
						"id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY, " +
						"data LONGBLOB NOT NULL, version BIGINT NOT NULL)";
				try (var ps = conn.prepareStatement(tableDataWithVersionSql)) {
					ps.executeUpdate();
				}
				var tableInstancesSql = "CREATE TABLE IF NOT EXISTS _ZezeInstances_(localid int NOT NULL PRIMARY KEY)";
				try (var ps = conn.prepareStatement(tableInstancesSql)) {
					ps.executeUpdate();
				}
				var stamp = calcOperatesProceduresStamp();
				if (stamp.equals(getOperatesProceduresStamp(conn)))
					return;
				// 复用当前连接完成重建与写戳，避免 maxActive=1 时嵌套借连接死锁。
				dropOperatesProcedures(conn);
				// 并发首启撞 "already exist" 视为对方已建同版本，忽略继续。
				try (var ps = conn.prepareStatement(PROC_SAVE_DATA_WITH_SAME_VERSION_SQL)) {
					ps.executeUpdate();
				} catch (SQLException ex) {
					if (!sqlMessageContains(ex, "already exist"))
						throw ex;
				}
				try (var ps = conn.prepareStatement(PROC_SET_IN_USE_SQL)) {
					ps.executeUpdate();
				} catch (SQLException ex) {
					if (!sqlMessageContains(ex, "already exist"))
						throw ex;
				}
				try (var ps = conn.prepareStatement(PROC_CLEAR_IN_USE_SQL)) {
					ps.executeUpdate();
				} catch (SQLException ex) {
					if (!sqlMessageContains(ex, "already exist"))
						throw ex;
				}
				writeOperatesProceduresStamp(conn, stamp);
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public boolean tryLock() {
			// 锁行 version 存租期到期 Unix 秒数（DB 服务器时钟：0=空闲，>0=持锁至该时刻），到期即可接管。
			var createRecordSql = "INSERT IGNORE INTO _ZezeDataWithVersion_ VALUES(?,?,?)";
			var lockSql = "UPDATE _ZezeDataWithVersion_ SET version=UNIX_TIMESTAMP()+" + LOCK_LEASE_SECONDS
					+ " WHERE id=? AND version<=UNIX_TIMESTAMP()";
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
				throw Task.forceThrow(e);
			}
		}
	}

	private static void setParams(PreparedStatement pre, int start,
								  ArrayList<Object> params) throws SQLException {
		for (int i = 0; i < params.size(); ++i) {
			var p = params.get(i);
			if (p instanceof String)
				pre.setString(i + start, (String)p);
			else
				pre.setBytes(i + start, ((Binary)p).toBytes());
		}
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeCallback(TableX<K, V> table, ResultSet rs, TableWalkHandle<K, V> callback,
						   @Nullable OutObject<K> outKey) throws Exception {
		K k = table.decodeKeyResultSet(rs);
		if (outKey != null)
			outKey.value = k;
		var r = table.requireCache().get(k);
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
	String buildOrder(TableX<K, V> table) {
		// 目前考虑keyColumns让Schemas来构造，注意生成顺序最好和encodeKeySQLStatement,decodeKeyResultSet【最好一致】。
		//noinspection DataFlowIssue
		return " ORDER BY " + table.getRelationalTable().currentKeyColumns;
	}

	private static <K extends Comparable<K>, V extends Bean>
	String buildOrderByDesc(TableX<K, V> table) {
		//noinspection DataFlowIssue
		return " ORDER BY " + table.getRelationalTable().currentKeyColumns.replace(",", " DESC,") + " DESC";
	}

	private static <K extends Comparable<K>, V extends Bean>
	String buildKeyWhere(TableX<K, V> table, SQLStatement st, @Nullable K exclusiveStartKey,
								  boolean asc) {
		if (exclusiveStartKey == null)
			return "";

		table.encodeKeySQLStatement(st, exclusiveStartKey);
		// 复合 key 使用行值比较 (col1,col2) > (v1,v2)，与 ORDER BY 元组字典序一致。
		var sql = st.getSql().toString();
		if (!sql.contains(", "))
			return " WHERE " + sql.replace('=', asc ? '>' : '<'); // 单列保持原样
		var columns = new StringJoiner(", ");
		var values = new StringJoiner(", ");
		for (var pair : SPLIT_PATTERN.split(sql)) {
			var eq = pair.indexOf('=');
			columns.add(pair.substring(0, eq));
			values.add(pair.substring(eq + 1));
		}
		return " WHERE (" + columns + ") " + (asc ? '>' : '<') + " (" + values + ")";
	}

	private static String buildKeyWhere(SQLStatement st) {
		return st.getSql().toString().replace(",", " AND ");
	}

	private static <K extends Comparable<K>, V extends Bean>
	boolean invokeKeyCallback(TableX<K, V> table, ResultSet rs, TableWalkKey<K> callback,
							  @Nullable OutObject<K> outKey) throws Exception {
		K k = table.decodeKeyResultSet(rs);
		if (outKey != null)
			outKey.value = k;
		var r = table.requireCache().get(k);
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
		private final String name;
		private boolean isNew;
		// volatile：flush线程与drop调用线程可能不同，可见性不得依赖锁外的巧合调度
		private volatile boolean dropped;

		public TableMysqlRelational(String name) {
			this.name = name;
			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			// create table if not exists 在表已存在时返回warning，由此检测isNew（原子性由数据库保证）。
			isNew = true;
			var table = getDatabase().getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			//noinspection DataFlowIssue
			var sql = table.getRelationalTable().createTableSql();
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
					isNew = !tableAlreadyExistsWarning(ps.getWarnings());
				}
			} catch (SQLException e) {
				if (!sqlMessageContains(e, "already exist"))
					throw Task.forceThrow(e);
				isNew = false;
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

		@Override
		public long getSize() {
			return dropped ? -1 : queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			return dropped ? -1 :
					queryLong1(dataSource, "SELECT TABLE_ROWS FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"
							+ name + "'");
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
				throw Task.forceThrow(e);
			}
		}

		// 实际存在的物理列（幂等重放的判定目录）。
		private java.util.HashSet<String> getPhysicalColumns() throws SQLException {
			var columns = new java.util.HashSet<String>();
			try (var conn = dataSource.getConnection()) {
				try (var ps = conn.prepareStatement(
						"SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?")) {
					ps.setString(1, name);
					try (var rs = ps.executeQuery()) {
						while (rs.next())
							columns.add(rs.getString(1));
					}
				}
			}
			return columns;
		}

		@Override
		public void tryAlter() {
			if (isNew) {
				logger.info("tryAlter isNew {}", name);
				return; // 已经是最新的表。不需要alter。
			}
			var table = getDatabase().getTable(name);
			if (table == null)
				throw new IllegalStateException("not found table: " + name);
			var r = Objects.requireNonNull(table.getRelationalTable());
			if (r.add.isEmpty() && r.remove.isEmpty() && r.change.isEmpty()) {
				// 元数据无差分时仍与物理列对账，补齐物理缺失列。
				try {
					var physical = getPhysicalColumns();
					for (var c : r.current)
						if (!physical.contains(c.name))
							r.add.add(c);
				} catch (SQLException e) {
					throw Task.forceThrow(e);
				}
				if (r.add.isEmpty()) {
					logger.info("tryAlter no change {}", name);
					return; // do nothing
				}
				logger.info("tryAlter heal {} : {} column(s) missing physically", name, r.add.size());
			}
			// 按物理列过滤已应用的 DDL 变更，保证崩溃重启后幂等重放。
			try {
				var physical = getPhysicalColumns();
				r.add.removeIf(c -> physical.contains(c.name));
				r.remove.removeIf(c -> !physical.contains(c.name));
				//noinspection DataFlowIssue
				r.change.removeIf(c -> !physical.contains(c.change.name));
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
			if (r.add.isEmpty() && r.remove.isEmpty() && r.change.isEmpty()) {
				logger.info("tryAlter no change {} (physical already migrated)", name);
				return;
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
				//noinspection DataFlowIssue
				sb.append(" CHANGE COLUMN ").append(c.change.name).append(' ')
						.append(c.name).append(' ').append(c.sqlType);
			}
			// 主键列序未变时不重建主键，避免 InnoDB 整表拷贝。
			//noinspection DataFlowIssue
			if (!r.currentKeyColumns.equals(r.previousKeyColumns))
				sb.append(", DROP PRIMARY KEY, ADD PRIMARY KEY (").append(r.currentKeyColumns).append(')');
			var sql = sb.toString();
			logger.info("tryAlter {} {}", table.getName(), sql);

			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> @Nullable V find(TableX<K, V> table,
																		  Object key) {
			if (dropped)
				return null;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT * FROM " + name + " WHERE " + buildKeyWhere(st);
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
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
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlSelectCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		@Override
		public <K extends Comparable<K>, V extends Bean> boolean containsKey(TableX<K, V> table,
																			 Object key) {
			if (dropped)
				return false;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var st = new SQLStatement();
			table.encodeKeySQLStatement(st, key);
			var sql = "SELECT 1 FROM " + name + " WHERE " + buildKeyWhere(st) + " LIMIT 1";
			// 存在性判定不取整行：SELECT 1+LIMIT 1避免大value列的无效传输
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
				try (var rs = ps.executeQuery()) {
					return rs.next();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlSelectCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(Transaction t, Object key, Object value) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var stValue = (SQLStatement)value;
			// 写入期前置校验 string key 长度与非空 value 段，提前暴露带表名的错误。
			for (var p : stKey.getParams())
				if (p instanceof String s && s.length() > eMaxKeyStringLength)
					throw new IllegalArgumentException("key string too long for mysql relational table '" + name
							+ "': " + s.length() + " > " + eMaxKeyStringLength);
			if (stValue.getSql().isEmpty())
				throw new IllegalStateException("mysql relational table '" + name + "' replace with empty value statement");
			var sql = "REPLACE " + name + " SET " + stKey.getSql() + ", " + stValue.getSql();
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sql)) {
				setParams(ps, 1, stKey.getParams());
				setParams(ps, stKey.getParams().size() + 1, stValue.getParams());
				ps.executeUpdate();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlReplaceCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void remove(Transaction t, Object key) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var stKey = (SQLStatement)key;
			var sql = "DELETE FROM " + name + " WHERE " + buildKeyWhere(stKey);
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sql)) {
				setParams(ps, 1, stKey.getParams());
				ps.executeUpdate();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlDeleteCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		private <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback,
				  String orderBy) throws Exception {
			if (dropped)
				return callback.endWalk(0);

			var s = "SELECT * FROM " + name + orderBy;
			var count = walkStreamed(s, rs -> invokeCallback(table, rs, callback, null));
			return callback.endWalk(count);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback,
					 String orderBy) throws Exception {
			if (dropped)
				return callback.endWalk(0);

			//noinspection DataFlowIssue
			var s = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + name + orderBy;
			var count = walkStreamed(s, rs -> invokeKeyCallback(table, rs, callback, null));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walk(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, callback, buildOrderByDesc(table)); // 反序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			return walkKey(table, callback, buildOrder(table)); // 正序
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			return walkKey(table, callback, buildOrderByDesc(table)); // 反序
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walk(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
						 TableWalkHandle<K, V> callback,
						 String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0) {
				callback.endWalk(0);
				return null;
			}

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
				ps.setInt(st.getParams().size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!invokeCallback(table, rs, callback, lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
			callback.endWalk(proposeLimit);
			return lastKey.value;
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
							TableWalkKey<K> callback, String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0) {
				callback.endWalk(0);
				return null;
			}

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			//noinspection DataFlowIssue
			var sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
					+ keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
				ps.setInt(st.getParams().size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						if (!invokeKeyCallback(table, rs, callback, lastKey))
							break;
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
			callback.endWalk(proposeLimit);
			return lastKey.value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walk(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
		   TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
		   TableWalkHandle<K, V> callback) throws Exception {
			return walk(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
			  TableWalkKey<K> callback) throws Exception {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
			  TableWalkKey<K> callback) throws Exception {
			return walkKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback,
						  String orderBy) throws Exception {
			if (dropped)
				return callback.endWalk(0);

			var s = "SELECT * FROM " + name + orderBy;
			var parents = new ArrayList<String>();
			var count = walkStreamed(s, rs -> {
				var key = table.decodeKeyResultSet(rs);
				var value = table.newValue();
				value.decodeResultSet(parents, rs);
				parents.clear();
				return callback.handle(key, value);
			});
			return callback.endWalk(count);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback,
							 String orderBy) throws Exception {
			if (dropped)
				return callback.endWalk(0);

			//noinspection DataFlowIssue
			var s = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName() + orderBy;
			var count = walkStreamed(s, rs -> callback.handle(table.decodeKeyResultSet(rs)));
			return callback.endWalk(count);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabase(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseDesc(TableX<K, V> table, TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, callback, buildOrderByDesc(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, callback, buildOrder(table));
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKeyDesc(TableX<K, V> table, TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, callback, buildOrderByDesc(table));
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 TableWalkHandle<K, V> callback,
								 String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0) {
				callback.endWalk(0);
				return null;
			}

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			var sql = "SELECT * FROM " + getName() + keyWhere + orderBy + " LIMIT ?";
			var parents = new ArrayList<String>();
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
				ps.setInt(st.getParams().size() + 1, proposeLimit);
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
				throw Task.forceThrow(e);
			}
			callback.endWalk(proposeLimit);
			return lastKey.value;
		}

		private <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									TableWalkKey<K> callback,
									String orderBy, boolean asc) throws Exception {
			if (dropped || proposeLimit <= 0) {
				callback.endWalk(0);
				return null;
			}

			var st = new SQLStatement();
			var keyWhere = buildKeyWhere(table, st, exclusiveStartKey, asc);
			//noinspection DataFlowIssue
			var sql = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName()
					+ keyWhere + orderBy + " LIMIT ?";
			var lastKey = new OutObject<K>();
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
				setParams(ps, 1, st.getParams());
				ps.setInt(st.getParams().size() + 1, proposeLimit);
				try (var rs = ps.executeQuery()) {
					while (rs.next()) {
						var key = table.decodeKeyResultSet(rs);
						lastKey.value = key;
						if (!callback.handle(key))
							break;
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
			callback.endWalk(proposeLimit);
			return lastKey.value;
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabase(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
								 TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									 TableWalkHandle<K, V> callback) throws Exception {
			return walkDatabase(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKey(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
									TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrder(table), true);
		}

		@Override
		public <K extends Comparable<K>, V extends Bean>
		@Nullable K walkDatabaseKeyDesc(TableX<K, V> table, @Nullable K exclusiveStartKey, int proposeLimit,
										TableWalkKey<K> callback) throws Exception {
			return walkDatabaseKey(table, exclusiveStartKey, proposeLimit, callback, buildOrderByDesc(table), false);
		}
	}

	public final class TableMysql extends Database.AbstractKVTable {
		private final String name;
		private final String sqlFind, sqlRemove, sqlReplace;
		private final boolean isNew;
		// volatile：flush线程与drop调用线程可能不同，可见性不得依赖锁外的巧合调度
		private volatile boolean dropped;

		public TableMysql(String name) {
			this.name = name;
			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			// create table if not exists 在表已存在时返回warning，由此检测isNew（原子性由数据库保证）。
			var isNew = true;
			try (var conn = dataSource.getConnection()) {
				conn.setAutoCommit(true);
				var sql = "CREATE TABLE IF NOT EXISTS " + name
						+ "(id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)";
				try (var ps = conn.prepareStatement(sql)) {
					ps.executeUpdate();
					isNew = !tableAlreadyExistsWarning(ps.getWarnings());
				}
			} catch (SQLException e) {
				if (!sqlMessageContains(e, "already exist"))
					throw Task.forceThrow(e);
				isNew = false;
			}

			sqlFind = "SELECT value FROM " + name + " WHERE id=?";
			sqlRemove = "DELETE FROM " + name + " WHERE id=?";
			sqlReplace = "REPLACE " + name + " VALUE(?,?)";
			this.isNew = isNew;
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

		@Override
		public long getSize() {
			return dropped ? -1 : queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			return dropped ? -1 :
					queryLong1(dataSource, "SELECT TABLE_ROWS FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"
							+ name + "'");
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
				throw Task.forceThrow(e);
			}
		}

		@Override
		public @Nullable ByteBuffer find(ByteBuffer key) {
			if (dropped)
				return null;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var k = key.CopyIf();
			byte[] v = null;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sqlFind)) {
				ps.setBytes(1, k);
				try (var rs = ps.executeQuery()) {
					if (rs.next())
						v = rs.getBytes(1);
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlSelectCounter.observe(System.nanoTime() - timeBegin);
			}
			return v != null ? ByteBuffer.Wrap(v) : null;
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var k = key.CopyIf();
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sqlRemove)) {
				ps.setBytes(1, k);
				ps.executeUpdate();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlDeleteCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			if (dropped)
				return;

			checkKvKeyLength(name, key);
			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			var k = key.CopyIf();
			var v = value.CopyIf();
			try (var ps = ((JdbcTrans)t).conn.prepareStatement(sqlReplace)) {
				ps.setBytes(1, k);
				ps.setBytes(2, v);
				ps.executeUpdate();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			} finally {
				if (timeBegin != 0) // 统计禁用时零开销
					mysqlReplaceCounter.observe(System.nanoTime() - timeBegin);
			}
		}

		@Override
		public long walk(TableWalkHandleRaw callback) throws Exception {
			return walk(callback, true);
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) throws Exception {
			return walkKey(callback, true);
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) throws Exception {
			return walk(callback, false);
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) throws Exception {
			return walkKey(callback, false);
		}

		private long walk(TableWalkHandleRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT * FROM " + name + (asc ? " ORDER BY id" : " ORDER BY id DESC");
			return walkStreamed(s, rs -> callback.handle(rs.getBytes(1), rs.getBytes(2)));
		}

		private long walkKey(TableWalkKeyRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			var s = "SELECT id FROM " + name + (asc ? " ORDER BY id" : " ORDER BY id DESC");
			return walkStreamed(s, rs -> callback.handle(rs.getBytes(1)));
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 TableWalkHandleRaw callback) throws Exception {
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
				throw Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											TableWalkKeyRaw callback) throws Exception {
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
				throw Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 TableWalkHandleRaw callback) throws Exception {
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
				throw Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												TableWalkKeyRaw callback) throws Exception {
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
				throw Task.forceThrow(e);
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}
	}

	public static long queryLong1(DruidDataSource dataSource, String sql) {
		var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
		try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql); var rs = ps.executeQuery()) {
			return rs.next() ? rs.getLong(1) : -1;
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		} finally {
			if (timeBegin != 0) // 统计禁用时零开销
				mysqlSelectCounter.observe(System.nanoTime() - timeBegin);
		}
	}
}
