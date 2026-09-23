package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.Task;
import Zeze.Util.ZezeCounter;
import com.alibaba.druid.pool.DruidDataSource;
import org.jetbrains.annotations.NotNull;

public final class DatabaseSqlServer extends DatabaseJdbc {
	// 与 DatabaseMySql.keyOfLock 相同的固定 flag id（各后端库独立存储，仅保持字节一致）。
	public static final byte[] keyOfLock =
			("Zeze.AtomicOpenDatabase.Flag." + 5284111301429717881L).getBytes(StandardCharsets.UTF_8);

	// 存储过程 _ZezeSaveDataWithSameVersion_ 有 4 个参数（@id,@data,@version,@ReturnValue），
	// 调用串占位符必须与之相同：registerOutParameter(4)/getInt(4) 操作第 4 个参数（@ReturnValue），
	// 少一个占位符会导致参数索引越界（mssql-jdbc: "The index 4 is out of range"），带 schemas 启动即失败。
	private static final String saveDataWithSameVersionCall = "{CALL _ZezeSaveDataWithSameVersion_(?, ?, ?, ?)}";

	public DatabaseSqlServer(Application zeze, DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesSqlServer());
	}

	@Override
	public Database.@NotNull Table openTable(@NotNull String name, int id) {
		return new TableSqlServer(name);
	}

	@Override
	public void renameTable(String oldName, String newName) throws Exception {
		// 幂等重跑：Schemas.checkCompatible的rename序列非原子——部分成功后中断（或renames全部
		// 成功但saveDataWithSameVersion未落库）时重启会对同一对名字再次rename，此时源已不存在、
		// 目标已存在，视为上次已完成，no-op跳过（sp_rename对两者都不报错，必须显式判定）。
		try (var conn = dataSource.getConnection()) {
			if (!tableExists(conn, oldName)) {
				if (tableExists(conn, newName))
					return;
				throw new IllegalStateException("renameTable: source table not found: " + oldName);
			}
			String sql = "EXEC sp_rename '" + oldName + "', '" + newName + "'";
			try (var stmt = conn.prepareStatement(sql)) {
				stmt.executeUpdate();
			}
		}
	}

	// renameTable幂等重跑的判定：OBJECT_ID按当前库解析对象名。
	private boolean tableExists(Connection conn, String name) throws SQLException {
		try (var ps = conn.prepareStatement("SELECT OBJECT_ID(?)")) {
			ps.setString(1, name);
			try (var rs = ps.executeQuery()) {
				return rs.next() && rs.getString(1) != null;
			}
		}
	}

	private static final ZezeCounter.LabeledObserverCreator sqlserverObserverCreator
			= ZezeCounter.instance.allocRunTimeObserverCreator("sqlserver_operation", "operation");
	private static final ZezeCounter.LongObserver sqlserverSelectCounter
			= sqlserverObserverCreator.labelValues("select");
	private static final ZezeCounter.LongObserver sqlserverDeleteCounter
			= sqlserverObserverCreator.labelValues("delete");
	private static final ZezeCounter.LongObserver sqlserverReplaceCounter
			= sqlserverObserverCreator.labelValues("replace");

	private final class OperatesSqlServer implements Operates {
		// 全局启动锁的租期（T3-F1），语义与取值对齐 DatabaseRedis.LOCK_LEASE_SECONDS：
		// 持锁进程崩溃后残留的锁最多存活一个租期，之后轮询的实例自动接管。
		private static final int LOCK_LEASE_SECONDS = 600;

		@Override
		public boolean tryLock() {
			// 条件写式互斥：锁行version复用为租期到期时间戳，0=空闲，>0=持锁至该时刻
			// （DB服务器时钟UTC，单一时间来源），到期即可接管。与MySql/PG版同款，
			// 误过期取舍同review-2026-09/l4/T3-4。
			var createRecordSql = """
					BEGIN TRY
					    insert into _ZezeDataWithVersion_ values(?, ?, 0)
					END TRY
					BEGIN CATCH
					    if ERROR_NUMBER() not in (2627, 2601)
					    begin
					        ; THROW
					    end
					END CATCH""";
			var lockSql = "UPDATE _ZezeDataWithVersion_ SET version=DATEDIFF_BIG(second, '1970-01-01', SYSUTCDATETIME())+"
					+ LOCK_LEASE_SECONDS
					+ " WHERE id=? AND version<=DATEDIFF_BIG(second, '1970-01-01', SYSUTCDATETIME())";
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareStatement(createRecordSql)) {
					cmd.setBytes(1, keyOfLock);
					cmd.setBytes(2, ByteBuffer.Empty);
					cmd.executeUpdate();
				}
				try (var cmd = connection.prepareStatement(lockSql)) {
					cmd.setBytes(1, keyOfLock);
					return cmd.executeUpdate() == 1;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void unlock() {
			var unlockSql = "UPDATE _ZezeDataWithVersion_ SET version=0 WHERE id=?";
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareStatement(unlockSql)) {
					cmd.setBytes(1, keyOfLock);
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void setInUse(int localId, @NotNull String global) {
			// 【FND11 txn-01】_ZezeSetInUse_与MySQL/PG版同构：并发双实例首启时insert全局行与
			// count(*)扫描互等成环（1205死锁）。对齐姊妹实现重试64次（SP整体事务，重试安全）。
			for (int i = 0; i < 64; ++i) {
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
							throw new IllegalStateException("UnknownReturnValue");
						}
					}
				} catch (SQLException e) {
					// mssql-jdbc死锁消息形如"Transaction (Process Id ...) was deadlocked ... deadlock victim"
					if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("deadlock"))
						throw Task.forceThrow(e);
				}
			}
			throw new IllegalStateException("setInUse Deadlock");
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
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
				throw Task.forceThrow(e);
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.CopyIf());
					try (var reader = cmd.executeQuery()) {
						if (reader.next()) {
							var result = new DataWithVersion();
							result.data = ByteBuffer.Wrap(reader.getBytes(1));
							result.version = reader.getLong(2);
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
		public KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data, long version) {
			if (key.isEmpty())
				throw new IllegalArgumentException("key is empty.");

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall(saveDataWithSameVersionCall)) {
					cmd.setBytes(1, key.CopyIf()); // @id
					cmd.setBytes(2, data.CopyIf()); // @data
					cmd.registerOutParameter(3, Types.BIGINT); // @version
					cmd.setLong(3, version);
					cmd.registerOutParameter(4, Types.INTEGER); // @ReturnValue return code
					cmd.executeUpdate();
					return switch (cmd.getInt(4)) {
						case 0 -> KV.create(cmd.getLong(3), true);
						case 2 -> KV.create(0L, false);
						default -> throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error.");
					};
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		public OperatesSqlServer() {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(false);

				String TableDataWithVersion = "if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')"
						+ " CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY("
						+ eMaxKeyLength
						+ ") NOT NULL PRIMARY KEY, data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
				try (var cmd = connection.prepareStatement(TableDataWithVersion)) {
					cmd.executeUpdate();
				}

				// 插入路径：并发插入撞重复键（2627主键/2601唯一索引）按MySQL版INSERT IGNORE的
				// 失败语义映射return 4，其他错误重抛。
				String ProcSaveDataWithSameVersion = """
						Create or Alter procedure _ZezeSaveDataWithSameVersion_
						                        @id VARBINARY(Max),
						                        @data VARBINARY(MAX),
						                        @version bigint output,
						                        @ReturnValue int output
						                    as
						                    begin
						                        BEGIN TRANSACTION
						                        set @ReturnValue=1
						                        DECLARE @currentversion bigint
						                        select @currentversion=version from _ZezeDataWithVersion_ where id = @id
						                        if @@ROWCOUNT > 0
						                        begin
						                            if @currentversion <> @version
						                            begin
						                                set @ReturnValue=2
						                                ROLLBACK TRANSACTION
						                                return 2
						                            end
						                            set @currentversion = @currentversion + 1
						                            update _ZezeDataWithVersion_ set data = @data, version = @currentversion where id = @id
						                            if @@rowcount = 1
						                            begin
						                                set @version = @currentversion
						                                set @ReturnValue=0
						                                COMMIT TRANSACTION
						                                return 0
						                            end
						                            set @ReturnValue=3
						                            ROLLBACK TRANSACTION
						                            return 3
						                        end
						
						                        BEGIN TRY
						                            insert into _ZezeDataWithVersion_ values(@id,@data,@version)
						                        END TRY
						                        BEGIN CATCH
						                            if ERROR_NUMBER() in (2627, 2601)
						                            begin
						                                set @ReturnValue=4
						                                ROLLBACK TRANSACTION
						                                return 4
						                            end
						                            ; THROW
						                        END CATCH
						                        set @ReturnValue=0
						                        COMMIT TRANSACTION
						                        return 0
						                    end""";
				try (var cmd = connection.prepareStatement(ProcSaveDataWithSameVersion)) {
					cmd.executeUpdate();
				}

				//noinspection SpellCheckingInspection
				String TableInstances = "if not exists (select * from sysobjects where name='_ZezeInstances_' and xtype='U')"
						+ " CREATE TABLE _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				try (var cmd = connection.prepareStatement(TableInstances)) {
					cmd.executeUpdate();
				}
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。

				// localid插入并发撞重复键（2627/2601）映射return 3。
				// global记录不存在则插入（最后一个实例退出时由_ZezeClearInUse_删除）；并发首启后到者
				// 撞重复键（2627/2601）重读比较，相同继续（对齐MySQL版INSERT IGNORE），不同return 4；
				// 对方必已提交（未提交的插入持键锁会先阻塞本方），重读必见已提交值。
				String ProcSetInUse = """
						Create or Alter procedure _ZezeSetInUse_
						                        @localid int,
						                        @global VARBINARY(MAX),
						                        @ReturnValue int output
						                    as
						                    begin
						                        BEGIN TRANSACTION
						                        set @ReturnValue=1
						                        if exists (select localid from _ZezeInstances_ where localid = @localid)
						                        begin
						                            set @ReturnValue=2
						                            ROLLBACK TRANSACTION
						                            return 2
						                        end
						                        BEGIN TRY
						                            insert into _ZezeInstances_ values(@localid)
						                        END TRY
						                        BEGIN CATCH
						                            if ERROR_NUMBER() in (2627, 2601)
						                            begin
						                                set @ReturnValue=3
						                                ROLLBACK TRANSACTION
						                                return 3
						                            end
						                            ; THROW
						                        END CATCH
						                        DECLARE @currentglobal VARBINARY(MAX)
						                        declare @emptybinary varbinary(max)
						                        set @emptybinary = convert(varbinary(max), '')
						                        select @currentglobal=data from _ZezeDataWithVersion_ where id=@emptybinary
						                        if @@rowcount > 0
						                        begin
						                            if @currentglobal <> @global
						                            begin
						                                set @ReturnValue=4
						                                ROLLBACK TRANSACTION
						                                return 4
						                            end
						                        end
						                        else
						                        begin
						                            BEGIN TRY
						                                insert into _ZezeDataWithVersion_ values(@emptybinary, @global, 0)
						                            END TRY
						                            BEGIN CATCH
						                                if ERROR_NUMBER() not in (2627, 2601)
						                                begin
						                                    ; THROW
						                                end
						                                select @currentglobal=data from _ZezeDataWithVersion_ where id=@emptybinary
						                                if @@rowcount > 0 and @currentglobal <> @global
						                                begin
						                                    set @ReturnValue=4
						                                    ROLLBACK TRANSACTION
						                                    return 4
						                                end
						                            END CATCH
						                        end
						                        DECLARE @InstanceCount int
						                        set @InstanceCount=0
						                        select @InstanceCount=count(*) from _ZezeInstances_
						                        if @InstanceCount = 1
						                        begin
						                            set @ReturnValue=0
						                            COMMIT TRANSACTION
						                            return 0
						                        end
						                        if DATALENGTH(@global)=0
						                        begin
						                            set @ReturnValue=6
						                            ROLLBACK TRANSACTION
						                            return 6
						                        end
						                        set @ReturnValue=0
						                        COMMIT TRANSACTION
						                        return 0
						                    end""";
				try (var cmd = connection.prepareStatement(ProcSetInUse)) {
					cmd.executeUpdate();
				}

				// localid不存在时不报错，总是继续后面的清除判断（实例可能已先退出）。
				String ProcClearInUse = """
						Create or Alter procedure _ZezeClearInUse_
							@localid int,
							@global VARBINARY(MAX),
							@ReturnValue int output
						as
						begin
							BEGIN TRANSACTION
							set @ReturnValue=1
							delete from _ZezeInstances_ where localid=@localid
							DECLARE @InstanceCount int
							set @InstanceCount=0
							select @InstanceCount=count(*) from _ZezeInstances_
							if @InstanceCount = 0
							begin
								declare @emptybinary varbinary(max)
								set @emptybinary = convert(varbinary(max), '')
								delete from _ZezeDataWithVersion_ where id=@emptybinary
							end
							set @ReturnValue=0
							COMMIT TRANSACTION
							return 0
						end""";
				try (var cmd = connection.prepareStatement(ProcClearInUse)) {
					cmd.executeUpdate();
				}
				connection.commit();
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}
	}

	private final class TableSqlServer extends Database.AbstractKVTable {
		private final String name;
		private final boolean isNew;
		private boolean dropped;

		@Override
		public @NotNull DatabaseSqlServer getDatabase() {
			return DatabaseSqlServer.this;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		public TableSqlServer(String name) {
			this.name = name;

			// isNew 仅用来在Schemas比较的时候可选的忽略被删除的表，这里没有跟Create原子化。
			try (var connection = dataSource.getConnection()) {
				DatabaseMetaData meta = connection.getMetaData();
				try (ResultSet resultSet = meta.getTables(null, null, this.name, new String[]{"TABLE"})) {
					isNew = !resultSet.next();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "if not exists (select * from sysobjects where name='"
						+ getName() + "' and xtype='U') CREATE TABLE "
						+ getName() + "(id VARBINARY("
						+ eMaxKeyLength
						+ ") NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(@NotNull ByteBuffer key) {
			if (dropped)
				return null;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			checkKvKeyLength(name, key);
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT value FROM " + getName() + " WHERE id = ?";
				// 是否可以重用 SqlCommand
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.CopyIf());
					try (var reader = cmd.executeQuery()) {
						if (reader.next()) {
							byte[] value = reader.getBytes(1);
							if (timeBegin != 0) // 统计禁用时零开销
								sqlserverSelectCounter.observe(System.nanoTime() - timeBegin);
							return ByteBuffer.Wrap(value);
						}
						if (timeBegin != 0) // 统计禁用时零开销
							sqlserverSelectCounter.observe(System.nanoTime() - timeBegin);
						return null;
					}
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void remove(@NotNull Transaction t, @NotNull ByteBuffer key) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			checkKvKeyLength(name, key);
			var my = (JdbcTrans)t;
			String sql = "DELETE FROM " + getName() + " WHERE id=?";
			try (var cmd = my.conn.prepareStatement(sql)) {
				cmd.setBytes(1, key.CopyIf());
				cmd.executeUpdate();
				if (timeBegin != 0) // 统计禁用时零开销
					sqlserverDeleteCounter.observe(System.nanoTime() - timeBegin);
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void replace(@NotNull Transaction t, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			if (dropped)
				return;

			var timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
			checkKvKeyLength(name, key);
			var my = (JdbcTrans)t;
			String sql = "update " + getName() + " set value=? where id=?"
					+ " if @@rowcount = 0 and @@error = 0 insert into " + getName() + " values(?,?)";
			try (var cmd = my.conn.prepareStatement(sql)) {
				var keyCopy = key.CopyIf();
				var valueCopy = value.CopyIf();
				cmd.setBytes(1, valueCopy);
				cmd.setBytes(2, keyCopy); // 传两次，使用存储过程优化？
				cmd.setBytes(3, keyCopy);
				cmd.setBytes(4, valueCopy);
				cmd.executeUpdate();
				if (timeBegin != 0) // 统计禁用时零开销
					sqlserverReplaceCounter.observe(System.nanoTime() - timeBegin);
			} catch (SQLException e) {
				throw Task.forceThrow(e);
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

		private long walk(TableWalkHandleRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + getName();
				if (asc)
					sql += " ORDER BY id";
				else
					sql += " ORDER BY id DESC";

				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							byte[] key = reader.getBytes(1);
							byte[] value = reader.getBytes(2);
							++count;
							if (!callback.handle(key, value)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		private long walkKey(TableWalkKeyRaw callback, boolean asc) throws Exception {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id FROM " + getName();
				if (asc)
					sql += " ORDER BY id";
				else
					sql += " ORDER BY id DESC";
				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							byte[] key = reader.getBytes(1);
							++count;
							if (!callback.handle(key)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top (?) id,value FROM " + getName()
						+ (exclusiveStartKey != null ? " WHERE id > ?" : "")
						+ " ORDER BY id";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setInt(1, proposeLimit);
					if (exclusiveStartKey != null)
						cmd.setBytes(2, exclusiveStartKey.CopyIf());

					byte[] lastKey = null;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							lastKey = reader.getBytes(1);
							if (!callback.handle(lastKey, reader.getBytes(2)))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top (?) id FROM " + getName()
						+ (exclusiveStartKey != null ? " WHERE id > ?" : "")
						+ " ORDER BY id";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setInt(1, proposeLimit);
					if (exclusiveStartKey != null)
						cmd.setBytes(2, exclusiveStartKey.CopyIf());

					byte[] lastKey = null;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							lastKey = reader.getBytes(1);
							if (!callback.handle(lastKey))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkHandleRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top (?) id,value FROM " + getName()
						+ (exclusiveStartKey != null ? " WHERE id < ?" : "")
						+ " ORDER BY id DESC";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setInt(1, proposeLimit);
					if (exclusiveStartKey != null)
						cmd.setBytes(2, exclusiveStartKey.CopyIf());

					byte[] lastKey = null;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							lastKey = reader.getBytes(1);
							if (!callback.handle(lastKey, reader.getBytes(2)))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, @NotNull TableWalkKeyRaw callback) throws Exception {
			if (dropped || proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top (?) id FROM " + getName()
						+ (exclusiveStartKey != null ? " WHERE id < ?" : "")
						+ " ORDER BY id DESC";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setInt(1, proposeLimit);
					if (exclusiveStartKey != null)
						cmd.setBytes(2, exclusiveStartKey.CopyIf());

					byte[] lastKey = null;
					try (var reader = cmd.executeQuery()) {
						while (reader.next()) {
							lastKey = reader.getBytes(1);
							if (!callback.handle(lastKey))
								break;
						}
					}
					return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
				}
			} catch (SQLException e) {
				throw Task.forceThrow(e);
			}
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
		public long getSize() {
			return dropped ? -1 : queryLong1(dataSource, "SELECT count(*) FROM " + name);
		}

		@Override
		public long getSizeApproximation() {
			return dropped ? -1 :
					queryLong1(dataSource, "SELECT p.rows FROM sys.partitions p WHERE p.object_id = OBJECT_ID('"
							+ name + "') AND p.index_id IN (0, 1);"); // -- 0=堆表, 1=聚集索引
		}
	}

	public static long queryLong1(@NotNull DruidDataSource dataSource, @NotNull String sql) {
		try (var conn = dataSource.getConnection(); var ps = conn.prepareStatement(sql); var rs = ps.executeQuery()) {
			return rs.next() ? rs.getLong(1) : -1;
		} catch (SQLException e) {
			throw Task.forceThrow(e);
		}
	}
}
