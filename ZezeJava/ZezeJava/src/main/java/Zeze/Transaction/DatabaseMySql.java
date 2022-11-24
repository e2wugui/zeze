package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;

public final class DatabaseMySql extends DatabaseJdbc {
	public DatabaseMySql(DatabaseConf conf) {
		super(conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesMySql());
	}

	@Override
	public Database.Table openTable(String name) {
		return new TableMysql(name);
	}

	public void dropTable(String name) {
		var tTable = getTable(name);
		if (null == tTable)
			return;

		var storage = tTable.getStorage();
		if (null == storage)
			return;

		var dTable = (TableMysql)storage.getDatabaseTable();
		dTable.drop();
	}

	private final class OperatesMySql implements Operates {
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
				throw new RuntimeException(e);
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
				throw new RuntimeException(e);
			}
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareStatement("SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?")) {
					cmd.setBytes(1, key.Copy());
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
				throw new RuntimeException(e);
			}
		}

		@Override
		public Zeze.Util.KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			if (key.Size() == 0) {
				throw new IllegalArgumentException("key is empty.");
			}

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				try (var cmd = connection.prepareCall("{CALL _ZezeSaveDataWithSameVersion_(?, ?, ?, ?)}")) {
					cmd.setBytes(1, key.Copy()); // key
					cmd.setBytes(2, data.Copy()); // data
					cmd.registerOutParameter(3, Types.BIGINT); // version (in | out)
					cmd.setLong(3, version);
					cmd.registerOutParameter(4, Types.INTEGER); // return code
					cmd.executeUpdate();
					switch (cmd.getInt(4)) {
					case 0:
						return Zeze.Util.KV.create(cmd.getLong(3), true);
					case 2:
						return Zeze.Util.KV.create(0L, false);
					default:
						throw new IllegalStateException("Procedure SaveDataWithSameVersion Exec Error.");
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		public OperatesMySql() {
			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(false);
				String TableDataWithVersion = "CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (" + "\r\n" +
						"                        id VARBINARY(" + eMaxKeyLength + ") NOT NULL PRIMARY KEY," + "\r\n" +
						"                        data LONGBLOB NOT NULL," + "\r\n" +
						"                        version bigint NOT NULL" + "\r\n" +
						"                    )";
				try (var cmd = connection.prepareStatement(TableDataWithVersion)) {
					cmd.executeUpdate();
				}
				try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_")) {
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
						"                        select FOUND_ROWS() into ROWCOUNT;" + "\r\n" +
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
						"                        insert into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);" + "\r\n" +
						"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT = 1 then" + "\r\n" +
						"                            set ReturnValue=0;" + "\r\n" +
						"                            COMMIT;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set ReturnValue=4;" + "\r\n" +
						"                        ROLLBACK;" + "\r\n" +
						"                        LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcSaveDataWithSameVersion)) {
					cmd.executeUpdate();
				}
				//noinspection SpellCheckingInspection
				String TableInstances = "CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				try (var cmd = connection.prepareStatement(TableInstances)) {
					cmd.executeUpdate();
				}
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。
				try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeSetInUse_")) {
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
						"                        insert into _ZezeInstances_ values(in_localid);" + "\r\n" +
						"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT = 0 then" + "\r\n" +
						"                            set ReturnValue=3;" + "\r\n" +
						"                            ROLLBACK;" + "\r\n" +
						"                            LEAVE return_label;" + "\r\n" +
						"                        end if;" + "\r\n" +
						"                        set emptybinary = BINARY '';" + "\r\n" +
						"                        select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" +
						"                        select FOUND_ROWS() into ROWCOUNT;" + "\r\n" +
						"                        if ROWCOUNT > 0 then" + "\r\n" +
						"                            if currentglobal <> in_global then" + "\r\n" +
						"                                set ReturnValue=4;" + "\r\n" +
						"                                ROLLBACK;" + "\r\n" +
						"                                LEAVE return_label;" + "\r\n" +
						"                            end if;" + "\r\n" +
						"                        else" + "\r\n" +
						"                            insert into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);" + "\r\n" +
						"                            select ROW_COUNT() into ROWCOUNT;" + "\r\n" +
						"                            if ROWCOUNT <> 1 then" + "\r\n" +
						"                                set ReturnValue=5;" + "\r\n" +
						"                                ROLLBACK;" + "\r\n" +
						"                                LEAVE return_label;" + "\r\n" +
						"                            end if;" + "\r\n" +
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
						"                        COMMIT;" + "\r\n" +
						"                        LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcSetInUse)) {
					cmd.executeUpdate();
				}
				try (var cmd = connection.prepareStatement("DROP PROCEDURE IF EXISTS _ZezeClearInUse_")) {
					cmd.executeUpdate();
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
						"                        COMMIT;" + "\r\n" +
						"                        LEAVE return_label;" + "\r\n" +
						"                    end;";
				try (var cmd = connection.prepareStatement(ProcClearInUse)) {
					cmd.executeUpdate();
				}
				connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public final class TableMysql implements Database.Table {
		private final String name;
		private final boolean isNew;
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
				throw new RuntimeException(e);
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
			try (var connection = dataSource.getConnection()) {
				DatabaseMetaData meta = connection.getMetaData();
				ResultSet resultSet = meta.getTables(null, null, this.name, new String[]{"TABLE"});
				isNew = resultSet.next();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);
				String sql = "CREATE TABLE IF NOT EXISTS " + getName()
						+ "(id VARBINARY("
						+ eMaxKeyLength
						+ ") NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)";
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.executeUpdate();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			if (dropped)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT value FROM " + getName() + " WHERE id = ?";
				// 是否可以重用 SqlCommand
				try (var cmd = connection.prepareStatement(sql)) {
					cmd.setBytes(1, key.Copy());
					try (var rs = cmd.executeQuery()) {
						if (rs.next()) {
							byte[] value = rs.getBytes(1);
							return ByteBuffer.Wrap(value);
						}
						return null;
					}
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			if (dropped)
				return;

			var my = (JdbcTrans)t;
			String sql = "DELETE FROM " + getName() + " WHERE id=?";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				cmd.setBytes(1, key.Copy());
				cmd.executeUpdate();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			if (dropped)
				return;

			var my = (JdbcTrans)t;
			String sql = "REPLACE INTO " + getName() + " values(?, ?)";
			try (var cmd = my.Connection.prepareStatement(sql)) {
				cmd.setBytes(1, key.Copy());
				cmd.setBytes(2, value.Copy());
				cmd.executeUpdate();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + getName();
				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							byte[] key = rs.getBytes(1);
							byte[] value = rs.getBytes(2);
							++count;
							if (!callback.handle(key, value)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (dropped)
				return null;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id,value FROM " + getName() + (null != exclusiveStartKey ? " WHERE id > ?" : "") + " LIMIT ?";
				try (var cmd = connection.prepareStatement(sql)) {
					var index = 1;
					if (null != exclusiveStartKey)
						cmd.setBytes(index++, copyIf(exclusiveStartKey));
					cmd.setInt(index, proposeLimit);
					byte[] lastKey = null;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							byte[] key = rs.getBytes(1);
							lastKey = key;
							byte[] value = rs.getBytes(2);
							if (!callback.handle(key, value)) {
								break;
							}
						}
					}
					return null == lastKey ? null : ByteBuffer.Wrap(lastKey);
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			if (dropped)
				return 0;

			try (var connection = dataSource.getConnection()) {
				connection.setAutoCommit(true);

				String sql = "SELECT id FROM " + getName();
				try (var cmd = connection.prepareStatement(sql)) {
					long count = 0;
					try (var rs = cmd.executeQuery()) {
						while (rs.next()) {
							byte[] key = rs.getBytes(1);
							++count;
							if (!callback.handle(key)) {
								break;
							}
						}
					}
					return count;
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
