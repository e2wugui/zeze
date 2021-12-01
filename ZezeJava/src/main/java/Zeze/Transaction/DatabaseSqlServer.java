package Zeze.Transaction;

import Zeze.Serialize.*;
import Zeze.Config.DatabaseConf;

public final class DatabaseSqlServer extends DatabaseJdbc {
	public DatabaseSqlServer(DatabaseConf conf) {
		super(conf);
		setDirectOperates(new OperatesSqlServer(this));
	}

	@Override
	public Database.Table OpenTable(String name) {
		return new TableSqlServer(this, name);
	}

	public final static class OperatesSqlServer implements Operates {
		private final DatabaseSqlServer DatabaseReal;
		public DatabaseSqlServer getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}

		public void SetInUse(int localId, String global) {
			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeSetInUse_(?, ?, ?)}")) {
					cmd.setInt(1, localId);
					cmd.setBytes(2, global.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					cmd.registerOutParameter(3, java.sql.Types.INTEGER);
					cmd.executeUpdate();
					switch (cmd.getInt(3)) {
						case 0:
							return;
						case 1:
							throw new RuntimeException("Unknown Error");
						case 2:
							throw new RuntimeException("Instance Exist");
						case 3:
							throw new RuntimeException("Insert LocalId Faild");
						case 4:
							throw new RuntimeException("Global Not Equals");
						case 5:
							throw new RuntimeException("Insert Global Faild");
						case 6:
							throw new RuntimeException("Instance Greater Than One But No Global");
						default:
							throw new RuntimeException("Unknown ReturnValue");
					}
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public int ClearInUse(int localId, String global) {
			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeClearInUse_(?, ?, ?)}")) {
					cmd.setInt(1, localId);
					cmd.setBytes(2, global.getBytes(java.nio.charset.StandardCharsets.UTF_8));
					cmd.registerOutParameter(3, java.sql.Types.INTEGER);
					cmd.executeUpdate();
					// Clear 不报告错误，直接返回。
					return cmd.getInt(3);
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public DataWithVersion GetDataWithVersion(ByteBuffer key) {
			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.Copy());
					try (var reader = cmd.executeQuery()) {
						if (reader.next()) {
							var result = new DataWithVersion();
							result.Data = ByteBuffer.Wrap(reader.getBytes(1));
							result.Version = reader.getLong(2);
							return result;
						}
						return null;
					}
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public Zeze.Util.KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			if (key.Size() == 0) {
				throw new RuntimeException("key is empty.");
			}

			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeSaveDataWithSameVersion_(?, ?, ?)}")) {
					cmd.setBytes(1, key.Copy());
					cmd.setBytes(2, data.Copy());
					cmd.registerOutParameter(3, java.sql.Types.BIGINT);
					cmd.setLong(3, version);
					cmd.registerOutParameter(4, java.sql.Types.INTEGER); // return code
					cmd.executeUpdate();
					switch (cmd.getInt(4)) {
						case 0:
							return Zeze.Util.KV.Create(cmd.getLong(3), true);
						case 2:
							return Zeze.Util.KV.Create(0L, false);
						default:
							throw new RuntimeException("Procedure SaveDataWithSameVersion Exec Error.");
					}
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public OperatesSqlServer(DatabaseSqlServer database) {
			DatabaseReal = database;

			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(false);
    
				String TableDataWithVersion = "if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')" + " CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY(767) NOT NULL PRIMARY KEY, data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
				try (var cmd = connection.prepareStatement(TableDataWithVersion)) {
					cmd.executeUpdate();
				}
    
				String ProcSaveDataWithSameVersion = "Create or Alter procedure _ZezeSaveDataWithSameVersion_" + "\r\n" + 
"                        @id VARBINARY(767)," + "\r\n" + 
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
"                        insert into _ZezeDataWithVersion_ values(@id,@data,@version)" + "\r\n" + 
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
    
				String TableInstances = "if not exists (select * from sysobjects where name='_ZezeInstances_' and xtype='U')" + " CREATE TABLE _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				try (var cmd = connection.prepareStatement(TableInstances)) {
					cmd.executeUpdate();
				}
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。
    
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
"                        insert into _ZezeInstances_ values(@localid)" + "\r\n" + 
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
"                            insert into _ZezeDataWithVersion_ values(@emptybinary, @global, 0)" + "\r\n" + 
"                            if @@rowcount <> 1" + "\r\n" + 
"                            begin" + "\r\n" + 
"                                set @ReturnValue=5" + "\r\n" + 
"                                ROLLBACK TRANSACTION" + "\r\n" + 
"                                return 5" + "\r\n" + 
"                            end" + "\r\n" + 
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
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final static class TableSqlServer implements Database.Table {
		private final DatabaseSqlServer DatabaseReal;
		public DatabaseSqlServer getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private final String Name;
		public String getName() {
			return Name;
		}

		public TableSqlServer(DatabaseSqlServer database, String name) {
			DatabaseReal = database;
			Name = name;

			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
    
				String sql = "if not exists (select * from sysobjects where name='" + getName() + "' and xtype='U') CREATE TABLE " + getName() + "(id VARBINARY(767) NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public void Close() {
		}

		public ByteBuffer Find(ByteBuffer key) {
			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
    
				String sql = "SELECT value FROM " + getName() + " WHERE id = ?";    
				// 是否可以重用 SqlCommand
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.Copy());
					try (var reader = cmd.executeQuery()) {
						if (reader.next()) {
							byte[] value = reader.getBytes(1);
							return ByteBuffer.Wrap(value);
						}
						return null;
					}
				}
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public void Remove(Transaction t, ByteBuffer key) {
			var my = (JdbcTrans)t;
			String sql = "DELETE FROM " + getName() + " WHERE id=?";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				cmd.setBytes(1, key.Copy());
				cmd.executeUpdate();
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var my = (JdbcTrans)t;
			String sql = "update " + getName() + " set value=? where id=?" + " if @@rowcount = 0 and @@error = 0 insert into " + getName() + " values(?,?)";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				var keycopy = key.Copy();
				var valuecopy = value.Copy();
				cmd.setBytes(1, valuecopy);
				cmd.setBytes(2, keycopy); // 传两次，使用存储过程优化？
				cmd.setBytes(3, keycopy);
				cmd.setBytes(4, valuecopy);
				cmd.executeUpdate();
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public long Walk(TableWalkHandleRaw callback) {
			try (var connection = DatabaseReal.dataSource.getConnection()) {
				connection.setAutoCommit(true);
    
				String sql = "SELECT id,value FROM " + getName();
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
			} catch (java.sql.SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}