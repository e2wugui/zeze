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

	// OperatesMySql三个过程体SQL文本的版本戳行key：data列存SHA-256 hex（见calcOperatesProceduresStamp），
	// 戳一致时启动跳过过程DDL（见OperatesMySql构造器）。
	public static final byte[] keyOfOperatesProceduresStamp =
			("Zeze.OperatesMySql.Procedures.Stamp." + 7362541089214536742L).getBytes(StandardCharsets.UTF_8);

	// 全表walk流式抓取：MySQL驱动默认把整个结果集缓冲进内存，大表walk有OOM风险。
	// setFetchSize(Integer.MIN_VALUE)启用逐行流式（驱动约定，无需url参数）。
	// 四个walk族入口（typed/raw × walk/walkKey，分属两个内部表类）共用。
	// 流式带来两个必须善后的问题（非流式下不存在）：
	// 1. 未读完就rs.close()时驱动会同步读完剩余所有行才能归还连接——中断/异常路径
	//    需先KILL QUERY让服务端停止发送。Statement.cancel()不可用：executeQuery返回后
	//    流式行消费期间statementExecuting已置false，cancel直接no-op（8.4.0验证）；
	//    驱动自身的超时中止（CancelQueryTask）也是另开连接发KILL QUERY，此处同法。
	// 2. 慢回调期间客户端停止读socket超过服务端net_write_timeout（默认60s）会被
	//    服务端断连——会话级临时调大，结束恢复为全局值（Druid回收不重置会话变量）。
	//    取值与驱动排空流式残余用的netTimeoutForStreamingResults默认值一致。
	private static final int WALK_NET_WRITE_TIMEOUT_SECONDS = 600;

	private long walkStreamed(String sql, JdbcWalkRow row) throws Exception {
		try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql)) {
			var connectionId = queryConnectionId(conn); // 连接号须在开始流式前取：流式读取期间该连接不能再执行语句
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
							abandoned = true; // walk契约：回调返回false中断
							break;
						}
					}
				} catch (Throwable t) {
					abandoned = true;
					throw t;
				} finally {
					if (abandoned && !killQuery(connectionId))
						// 控制通路失败（借取超时/KILL异常）时废弃物理连接：服务端看到TCP断开
						// 即停止发送并中止查询，避免close同步排空剩余整表。后续rs/close的异常
						// 属于连接已死，各处已有忽略路径；废弃不掩盖主流程异常。
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
							throw e; // abandoned时KILL会让close收到1317中断包，异常不掩盖主流程
						// kill成功但close仍抛=驱动可能未清除流式状态（MySQL探针批soak-20261023-1712
						// 实锤：中断排空的close不保证置关闭标志，带毒连接归池后永久
						// "streaming still active"风暴）——升级废弃物理连接，绝不归池。
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
					// 恢复失败（连接已死）可忽略：服务端会话随之消亡，Druid校验会逐出坏连接
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

	// KILL QUERY只中止查询、连接仍可用；账号无需特权即可kill自己的线程。尽力而为：
	// 失败时退回默认行为（close排空剩余行），不阻塞主流程；返回false表示控制通路失败，
	// 调用方可据此升级到废弃物理连接（见walkStreamed）。
	// 取消通路必须独立于业务池：业务池被并发walk占满时（maxWait默认-1无限等待），从同一池
	// 借连接清理会形成互等——每个清理者等第二条连接，而被清理的walk连接必须等清理完成才
	// 归还，池饱和即永久卡死（maxActive=1的合法配置一次walk即触发）。控制池与业务池容量
	// 隔离、借取带期限；懒初始化，从未中断walk的库不建控制池。
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
			    SELECT count(*) INTO instance_count FROM _ZezeInstances_;
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

	// SQLException.getMessage()无契约保证非null（驱动包装异常、本地化场景可为null），
	// catch块内直接contains会NPE：本应幂等继续/死锁重试的路径变成启动失败且掩盖原始异常。
	// 收口为null安全判定：null消息按不匹配处理，走默认抛出路径。
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

	private static final String PROC_CLEAR_IN_USE_SQL = """
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
			    DELETE FROM _ZezeInstances_ WHERE localid=in_local_id;
			    SET instance_count=0;
			    SELECT count(*) INTO instance_count FROM _ZezeInstances_;
			    IF instance_count = 0 THEN
			        SET empty_bin = BINARY '';
			        DELETE FROM _ZezeDataWithVersion_ WHERE id=empty_bin;
			    END IF;
			    SET ret_value=0;
			    IF 1=1 THEN
			        COMMIT;
			    END IF;
			    LEAVE return_label;
			END;""";

	// 版本戳=三个过程体SQL文本的SHA-256（hex）。过程体任何修改都改变戳，从而触发一次DROP+CREATE。
	private static String calcOperatesProceduresStamp() {
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
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesMySql());
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
		// MySQL 出厂默认排序规则 utf8mb4_0900_ai_ci 大小写不敏感，string key 列做
		// PRIMARY KEY 时大小写变体 key 被合并：REPLACE 冲突先删后插静默丢行、find/remove
		// 命中错误行。显式二进制排序规则使等值匹配与 Zeze 缓存的字节比较语义一致。
		// 必须同时显式 CHARACTER SET：COLLATE 不带字符集时继承库默认字符集，
		// 默认字符集非 utf8mb4 的库上 CREATE TABLE 直接报错。
		// 存量表的列排序规则不会被新配置改变，需按 docs（database/relational.md）手工 ALTER。
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
		// 幂等重跑：Schemas.checkCompatible的rename序列非原子——部分成功后中断（或renames全部
		// 成功但saveDataWithSameVersion未落库）时重启会对同一对名字再次rename，此时源已不存在、
		// 目标已存在，视为上次已完成，no-op跳过；源目标都不存在是真正的异常，明确报错。
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

	// 在给定连接上drop三个过程并清版本戳。OperatesMySql构造器持连接调用——不得再向池借
	// 第二条连接（maxActive=1的合法配置下两处互等即死锁）；运维入口经无参包装走独立事务。
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
		// 版本戳一并清除：drop入口语义是"强制下次启动重建"（运维排障用），
		// 留戳会让下次启动按戳跳过重建、过程持续缺失。表不存在（disableOperates库）时无需清。
		if (tableExists(conn, "_ZezeDataWithVersion_")) {
			try (var ps = conn.prepareStatement("DELETE FROM _ZezeDataWithVersion_ WHERE id=?")) {
				ps.setBytes(1, keyOfOperatesProceduresStamp);
				ps.executeUpdate();
			}
		}
	}

	private final class OperatesMySql implements Operates {
		// 全局启动锁的租期，语义与取值对齐 DatabaseRedis.LOCK_LEASE_SECONDS：
		// 持锁进程崩溃（kill -9/OOM/断电）后残留的锁最多存活一个租期，之后轮询的
		// 实例自动接管，无需人工恢复。
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
						// 3=版本条件的UPDATE未命中行：并发写者已抢先，映射false走CAS重读重试
						case 3 -> KV.create(0L, false);
						// 4=首插路径INSERT IGNORE撞并发重复键（他进程在SELECT与INSERT间抢先认领，
						// 见过程体SET ret_value=4）：败者须回读真正胜者走CAS重试（对齐Tikv/MongoDb
						// 同场景契约），此前落入default抛ISE会中断OwnerCheck/publishSchemas启动
						case 4 -> KV.create(0L, false);
						default -> throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error");
					};
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		public OperatesMySql() {
			// MySQL无CREATE OR REPLACE PROCEDURE：过程体修复（重读校验、错误传播等）要下发
			// 存量库只能DROP后CREATE。但无条件每次重建不可取：本DDL在构造器执行，先于启动锁
			// （atomicOpenDatabase），多实例共享库时DROP与CREATE之间（含崩溃残留）的窗口
			// 每次启动都出现，干扰正在调用过程的在跑实例。改为版本戳门控：三个过程体SQL文本
			// 的SHA-256存_ZezeDataWithVersion_专用行，戳一致即跳过全部过程DDL——常态启动零
			// 过程DDL，仅过程体随版本变更时重建一次（此时仍有短暂缺失窗口，属一次性升级代价）。
			// 戳在三个CREATE全部成功后写入：中途崩溃留下旧戳/无戳，下次启动重建自愈。
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
					return; // 过程体未变，跳过重建（常态启动路径）
				// 同一连接上完成drop+create+写戳：不再嵌套借连接（maxActive=1的合法配置下互等死锁）
				dropOperatesProcedures(conn);
				// 并发首启动的间隙保护：CREATE撞"already exist"视为对方已建同版本，吞掉继续。
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
			// 锁行version复用为租期到期时间戳（对齐DatabaseRedis.LOCK_LEASE_SECONDS的取舍）：
			// 0=空闲，>0=持锁至该时刻（DB服务器时钟，单一时间来源避免各实例本地时钟漂移），
			// 到期即可接管——version不得只在0/1间翻转：持锁进程硬崩溃后残留的1永不恢复，
			// 所有后续实例启动永久挂死。租期须显著大于持锁窗口最坏耗时（atomicOpenDatabase全程，
			// 含renameTable与大表tryAlter，分钟级）；不做续期与持有者校验（unlock无条件置0），
			// 慢启动超过租期的误过期窗口是既定取舍（与Redis版一致）。
			// 升级窗口注意：旧版本持有的version=1会被新代码立即视为已过期而接管，一次性
			// 退化为无锁并发（schemasCompatible本就保留并发安全）。
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
		// 复合key必须按元组字典序比较：游标(1,"zzz")之后是(2,"aaa")而非两者都大于。
		// AND形式(col1>? AND col2>?)整块漏掉跨段数据；行值比较(col1,col2)>(v1,v2)与ORDER BY语义一致。
		// 列值对为 col=?（参数）或 col=字面量（数值内联），按首个'='拆分，params占位符相对顺序不变。
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
				// 同PG（DatabasePostgreSQL.tryAlter）：元数据差分为空≠物理已迁移，
				// meta领先物理的desync以物理对账自愈——current列物理缺失即补列。
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
			// 幂等重放：元数据发布延迟到DDL成功之后（Application.publishSchemas），
			// DDL成功、发布前崩溃的重启会重算出同一差分——必须按物理列过滤已应用的部分，
			// 否则ADD COLUMN撞已存在列失败，升级重启卡死。ALTER TABLE单语句原子，
			// 按表全有/全无，过滤后为空即整表已迁移。
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
			// key列序未变不重建主键：DROP+ADD PRIMARY KEY在InnoDB下是整表拷贝，
			// 纯value列变更不应付出该代价。key变化必然表现为列diff（varIds排序决定列序）。
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
			// 超长 string key 不在写入期拒绝的话，flush 落库才触发 MySQL 1406 "Data too long"（不含表名），
			// 毒化整个 flush 批次且难定位——与 KV 表超长 key 前置检查同型（见 TableMysql.replace）。
			for (var p : stKey.getParams())
				if (p instanceof String s && s.length() > eMaxKeyStringLength)
					throw new IllegalArgumentException("key string too long for mysql relational table '" + name
							+ "': " + s.length() + " > " + eMaxKeyStringLength);
			// 空value段（value bean零变量）拼出"REPLACE t SET k=v, "尾逗号语法错误，毒化整个
			// flush批次且不含表名；生成的关系表value bean至少有列（appendXXX逐变量无条件追加，
			// 全默认值不产生空段），本守卫防御未来出现零变量bean的配置，fail-fast并指向表名。
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
				throw Task.forceThrow(e);
			}
			return callback.endWalk(count);
		}

		private <K extends Comparable<K>, V extends Bean>
		long walkDatabaseKey(TableX<K, V> table, TableWalkKey<K> callback,
							 String orderBy) throws Exception {
			if (dropped)
				return callback.endWalk(0);

			//noinspection DataFlowIssue
			var s = "SELECT " + table.getRelationalTable().currentKeyColumns + " FROM " + getName() + orderBy;
			var count = 0L;
			try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(s); var rs = ps.executeQuery()) {
				while (rs.next()) {
					count++;
					if (!callback.handle(table.decodeKeyResultSet(rs)))
						break;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
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

			checkKvKeyLengthForLegacyAccess(name, key);
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

			checkKvKeyLengthForLegacyAccess(name, key);
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
