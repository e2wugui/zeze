package Zeze.Dbh2;

import java.util.Objects;

import java.util.Date;
import Zeze.Raft.IRaftRpc;
import Zeze.Raft.LogSequence;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine.AppliedWatermark;
import Zeze.Raft.UniqueRequestState;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;

/** 应用完成记录与桶数据同库、同批写入，水位和请求终态存根随桶快照恢复。 */
final class Dbh2AppliedState {
	private static final byte[] WatermarkKey = {0};
	private static final String UniqueAppliedPrefix = "Zeze.Dbh2.UniqueApplied";
	private final RocksDatabase database;
	private final RocksDatabase.Table watermarkTable;

	Dbh2AppliedState(RocksDatabase database) throws RocksDBException {
		this.database = database;
		watermarkTable = database.getOrAddTable("Zeze.Dbh2.AppliedWatermark");
	}

	@Nullable AppliedWatermark readWatermark() throws RocksDBException {
		var bytes = watermarkTable.get(WatermarkKey);
		if (bytes == null)
			return null;
		var bb = ByteBuffer.Wrap(bytes);
		return new AppliedWatermark(bb.ReadLong(), bb.ReadLong());
	}

	private static String uniqueAppliedTableName(long createTime) {
		return LogSequence.makeUniqueRequestTableName(UniqueAppliedPrefix,
				LogSequence.toUniqueRequestKey(new Date(createTime)));
	}

	@Nullable UniqueRequestState getUniqueAppliedStub(IRaftRpc rpc) throws RocksDBException {
		var table = database.getTableMap().get(uniqueAppliedTableName(rpc.getCreateTime()));
		if (table == null)
			return null; // 只读查询不创建列族
		var key = ByteBuffer.Allocate();
		rpc.getUnique().encode(key);
		var bytes = table.get(key.Bytes, 0, key.WriteIndex);
		if (bytes == null)
			return null;
		var state = new UniqueRequestState();
		state.decode(ByteBuffer.Wrap(bytes));
		return state;
	}

	void removeExpiredUniqueStubTables(long expired) throws Exception {
		for (var table : database.getTableMap().values()) {
			var date = LogSequence.parseUniqueRequestDate(UniqueAppliedPrefix, table.getName());
			if (date != null && date.getTime() < expired)
				table.drop();
		}
	}

	/** 只追加到调用者的业务批次；提交成功之前不发布应用完成事实。 */
	void addToBatch(RaftLog holder, RocksDatabase.Batch batch) throws RocksDBException {
		var log = holder.getLog();
		if (Objects.requireNonNull(log).getUnique().getRequestId() > 0) {
			var key = ByteBuffer.Allocate();
			Objects.requireNonNull(log).getUnique().encode(key);
			var value = ByteBuffer.encode(new UniqueRequestState(holder, true));
			database.getOrAddTable(uniqueAppliedTableName(log.getCreateTime()))
					.put(batch, key.Bytes, 0, key.WriteIndex, value.Bytes, 0, value.WriteIndex);
		}
		var watermark = ByteBuffer.Allocate(16);
		watermark.WriteLong(holder.getIndex());
		watermark.WriteLong(holder.getTerm());
		watermarkTable.put(batch, WatermarkKey, 0, WatermarkKey.length,
				watermark.Bytes, 0, watermark.WriteIndex);
	}
}
