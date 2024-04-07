package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.Task;

public final class DatabaseSqlServer extends DatabaseJdbc {
	public DatabaseSqlServer(Application zeze, DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesSqlServer());
	}

	@Override
	public Database.Table openTable(String name, int id) {
		return new TableSqlServer(name);
	}

	private final class OperatesSqlServer implements Operates {
		@Override
		public void setInUse(int localId, String global) {
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
						throw new IllegalStateException("Unknown ReturnValue");
					}
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
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
				//noinspection UnreachableCode
				return -1; // never run here
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
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
				try (var cmd = connection.prepareCall("{CALL _ZezeSaveDataWithSameVersion_(?, ?, ?)}")) {
					cmd.setBytes(1, key.CopyIf());
					cmd.setBytes(2, data.CopyIf());
					cmd.registerOutParameter(3, Types.BIGINT);
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

		public OperatesSqlServer() {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(false);

				//noinspection SpellCheckingInspection
				String TableDataWithVersion = "if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')"
						+ " CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY("
						+ eMaxKeyLength
						+ ") NOT NULL PRIMARY KEY, data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
				try (var cmd = connection.prepareStatement(TableDataWithVersion)) {
					cmd.executeUpdate();
				}

				//noinspection SpellCheckingInspection
				String ProcSaveDataWithSameVersion = "Create or Alter procedure _ZezeSaveDataWithSameVersion_" + "\r\n" +
						"                        @id VARBINARY(Max)," + "\r\n" +
						"                        @data VARBINARY(MAX)," + "\r\n" +
						"                        @version bigint output," + "\r\n" +
						"                        @ReturnValue int output" + "\r\n" +
						"                    as" + "\r\n" +
						"                    begin" + "\r\n" +
						"                        BEGIN TRANSACTION" + "\r\n" +
						"                        set @ReturnValue=1" + "\r\n" +
						"                        DECLARE @currentversion bigint" + "\r\n" +
						"                        select @currentversion=version from _ZezeDataWithVersion_ where id = @id" + "\r\n" +
						"                        if @@ROWCOUNT > 0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            if @currentversion <> @version" + "\r\n" +
						"                            begin" + "\r\n" +
						"                                set @ReturnValue=2" + "\r\n" +
						"                                ROLLBACK TRANSACTION" + "\r\n" +
						"                                return 2" + "\r\n" +
						"                            end" + "\r\n" +
						"                            set @currentversion = @currentversion + 1" + "\r\n" +
						"                            update _ZezeDataWithVersion_ set data = @data, version = @currentversion where id = @id" + "\r\n" +
						"                            if @@rowcount = 1" + "\r\n" +
						"                            begin" + "\r\n" +
						"                                set @version = @currentversion" + "\r\n" +
						"                                set @ReturnValue=0" + "\r\n" +
						"                                COMMIT TRANSACTION" + "\r\n" +
						"                                return 0" + "\r\n" +
						"                            end" + "\r\n" +
						"                            set @ReturnValue=3" + "\r\n" +
						"                            ROLLBACK TRANSACTION" + "\r\n" +
						"                            return 3" + "\r\n" +
						"                        end" + "\r\n" +
						"\r\n" +
						"insert into _ZezeDataWithVersion_ values(@id,@data,@version) " +
						" select id,data,version from _ZezeDataWithVersion_ where NOT EXISTS (select id where id=@id)" + "\r\n" +
						"                        if @@rowcount = 1" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=0" + "\r\n" +
						"                            COMMIT TRANSACTION" + "\r\n" +
						"                            return 0" + "\r\n" +
						"                        end" + "\r\n" +
						"                        set @ReturnValue=4" + "\r\n" +
						"                        ROLLBACK TRANSACTION" + "\r\n" +
						"                        return 4" + "\r\n" +
						"                    end";
				try (var cmd = connection.prepareStatement(ProcSaveDataWithSameVersion)) {
					cmd.executeUpdate();
				}

				//noinspection SpellCheckingInspection
				String TableInstances = "if not exists (select * from sysobjects where name='_ZezeInstances_' and xtype='U')" + " CREATE TABLE _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				try (var cmd = connection.prepareStatement(TableInstances)) {
					cmd.executeUpdate();
				}
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。

				//noinspection SpellCheckingInspection
				String ProcSetInUse = "Create or Alter procedure _ZezeSetInUse_" + "\r\n" +
						"                        @localid int," + "\r\n" +
						"                        @global VARBINARY(MAX)," + "\r\n" +
						"                        @ReturnValue int output" + "\r\n" +
						"                    as" + "\r\n" +
						"                    begin" + "\r\n" +
						"                        BEGIN TRANSACTION" + "\r\n" +
						"                        set @ReturnValue=1" + "\r\n" +
						"                        if exists (select localid from _ZezeInstances_ where localid = @localid)" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=2" + "\r\n" +
						"                            ROLLBACK TRANSACTION" + "\r\n" +
						"                            return 2" + "\r\n" +
						"                        end" + "\r\n" +
						"                        insert into _ZezeInstances_ values(@localid) select localid from _ZezeInstances_ where NOT EXISTS (select localid from _ZezeInstances_ where localid=@localid)" + "\r\n" +
						"                        if @@rowcount = 0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=3" + "\r\n" +
						"                            ROLLBACK TRANSACTION" + "\r\n" +
						"                            return 3" + "\r\n" +
						"                        end" + "\r\n" +
						"                        DECLARE @currentglobal VARBINARY(MAX)" + "\r\n" +
						"                        declare @emptybinary varbinary(max)" + "\r\n" +
						"                        set @emptybinary = convert(varbinary(max), '')" + "\r\n" +
						"                        select @currentglobal=data from _ZezeDataWithVersion_ where id=@emptybinary" + "\r\n" +
						"                        if @@rowcount > 0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            if @currentglobal <> @global" + "\r\n" +
						"                            begin" + "\r\n" +
						"                                set @ReturnValue=4" + "\r\n" +
						"                                ROLLBACK TRANSACTION" + "\r\n" +
						"                                return 4" + "\r\n" +
						"                            end" + "\r\n" +
						"                        end" + "\r\n" +
						"                        else" + "\r\n" +
						"                        begin" + "\r\n" +
						"insert into _ZezeInstances_ values(@localid) select localid from _ZezeInstances_ where NOT EXISTS (select localid from _ZezeInstances_ where localid=@localid)" + "\r\n" +
						"                        end" + "\r\n" +
						"                        DECLARE @InstanceCount int" + "\r\n" +
						"                        set @InstanceCount=0" + "\r\n" +
						"                        select @InstanceCount=count(*) from _ZezeInstances_" + "\r\n" +
						"                        if @InstanceCount = 1" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=0" + "\r\n" +
						"                            COMMIT TRANSACTION" + "\r\n" +
						"                            return 0" + "\r\n" +
						"                        end" + "\r\n" +
						"                        if DATALENGTH(@global)=0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=6" + "\r\n" +
						"                            ROLLBACK TRANSACTION" + "\r\n" +
						"                            return 6" + "\r\n" +
						"                        end" + "\r\n" +
						"                        set @ReturnValue=0" + "\r\n" +
						"                        COMMIT TRANSACTION" + "\r\n" +
						"                        return 0" + "\r\n" +
						"                    end";
				try (var cmd = connection.prepareStatement(ProcSetInUse)) {
					cmd.executeUpdate();
				}

				//noinspection SpellCheckingInspection
				String ProcClearInUse = "Create or Alter procedure _ZezeClearInUse_" + "\r\n" +
						"                        @localid int," + "\r\n" +
						"                        @global VARBINARY(MAX)," + "\r\n" +
						"                        @ReturnValue int output" + "\r\n" +
						"                    as" + "\r\n" +
						"                    begin" + "\r\n" +
						"                        BEGIN TRANSACTION" + "\r\n" +
						"                        set @ReturnValue=1" + "\r\n" +
						"                        delete from _ZezeInstances_ where localid=@localid" + "\r\n" +
						"                        if @@rowcount = 0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            set @ReturnValue=2" + "\r\n" +
						"                            ROLLBACK TRANSACTION" + "\r\n" +
						"                            return 2" + "\r\n" +
						"                        end" + "\r\n" +
						"                        DECLARE @InstanceCount int" + "\r\n" +
						"                        set @InstanceCount=0" + "\r\n" +
						"                        select @InstanceCount=count(*) from _ZezeInstances_" + "\r\n" +
						"                        if @InstanceCount = 0" + "\r\n" +
						"                        begin" + "\r\n" +
						"                            declare @emptybinary varbinary(max)" + "\r\n" +
						"                            set @emptybinary = convert(varbinary(max), '')" + "\r\n" +
						"                            delete from _ZezeDataWithVersion_ where id=@emptybinary" + "\r\n" +
						"                        end" + "\r\n" +
						"                        set @ReturnValue=0" + "\r\n" +
						"                        COMMIT TRANSACTION" + "\r\n" +
						"                        return 0" + "\r\n" +
						"                    end";
				try (var cmd = connection.prepareStatement(ProcClearInUse)) {
					cmd.executeUpdate();
				}
				connection.commit();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}
	}

	private final class TableSqlServer extends Database.AbstractKVTable {
		private final String name;
		private final boolean isNew;

		@Override
		public DatabaseSqlServer getDatabase() {
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
				ResultSet resultSet = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = resultSet.next();
			} catch (SQLException e) {
				Task.forceThrow(e);
				throw new AssertionError(); // never run here
			}

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				//noinspection SpellCheckingInspection
				String sql = "if not exists (select * from sysobjects where name='"
						+ getName() + "' and xtype='U') CREATE TABLE "
						+ getName() + "(id VARBINARY("
						+ eMaxKeyLength
						+ ") NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				Task.forceThrow(e);
				throw new AssertionError(); // never run here
			}
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT value FROM " + getName() + " WHERE id = ?";
				// 是否可以重用 SqlCommand
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.CopyIf());
					try (var reader = cmd.executeQuery()) {
						if (reader.next()) {
							byte[] value = reader.getBytes(1);
							return ByteBuffer.Wrap(value);
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
		public void remove(Transaction t, ByteBuffer key) {
			var my = (JdbcTrans)t;
			String sql = "DELETE FROM " + getName() + " WHERE id=?";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				cmd.setBytes(1, key.CopyIf());
				cmd.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var my = (JdbcTrans)t;
			String sql = "update " + getName() + " set value=? where id=?" + " if @@rowcount = 0 and @@error = 0 insert into " + getName() + " values(?,?)";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				var keyCopy = key.CopyIf();
				var valueCopy = value.CopyIf();
				cmd.setBytes(1, valueCopy);
				cmd.setBytes(2, keyCopy); // 传两次，使用存储过程优化？
				cmd.setBytes(3, keyCopy);
				cmd.setBytes(4, valueCopy);
				cmd.executeUpdate();
			} catch (SQLException e) {
				Task.forceThrow(e);
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
				Task.forceThrow(e);
				//noinspection UnreachableCode
				return -1; // never run here
			}
		}

		private long walkKey(TableWalkKeyRaw callback, boolean asc) {
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
				Task.forceThrow(e);
				//noinspection UnreachableCode
				return -1; // never run here
			}
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top ? id,value FROM " + getName()
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
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top ? id FROM " + getName()
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
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top ? id,value FROM " + getName()
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
				Task.forceThrow(e);
				return null; // never run here
			}
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT top ? id FROM " + getName()
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
				Task.forceThrow(e);
				return null; // never run here
			}
		}
	}
}
