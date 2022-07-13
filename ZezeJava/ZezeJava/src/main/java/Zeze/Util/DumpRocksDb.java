package Zeze.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import Zeze.Builtin.GlobalCacheManagerWithRaft.AcquiredState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.CacheState;
import Zeze.Serialize.ByteBuffer;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompactionOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.LiveFileMetaData;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.SstFileMetaData;
import static Zeze.Transaction.Bean.Hash32;
import static Zeze.Util.BitConverter.num2Hex;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DumpRocksDb {
	public static void main(String[] args) throws Throwable {
		int argCount = args.length;
		for (; argCount > 0; argCount--) {
			var arg = args[argCount - 1];
			if (!arg.startsWith("-"))
				break;
			int p = arg.indexOf('=', 1);
			if (p >= 0)
				System.setProperty(arg.substring(1, p), arg.substring(p + 1));
			else
				System.setProperty(arg.substring(1), "true");
		}
		if (argCount < 1) {
			System.out.println("usage: java -cp ... " + DumpRocksDb.class.getName() +
					" inputDbPath [[columnFamilyName] outputTxtFile] [options]");
			System.out.println("options: -meta      list database meta data");
			System.out.println("         -compact   compact all the database files");
			System.out.println("         -compact1  compact all the level-0 files to level-1");
			System.out.println("         -raftLog   decode raft logs for global server");
			return;
		}
		var inputDbPath = args[0];
		var columnFamilyName = argCount > 2 ? args[1] : null;
		var outputTxtFile = argCount == 1 ? null : argCount == 2 ? args[1] : args[2];
		Action3<OutputStream, byte[], byte[]> dumpAction = "true".equalsIgnoreCase(System.getProperty("raftLog"))
				? DumpRocksDb::dumpRaftLog
				: DumpRocksDb::dumpRawKV;

		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		var cfOptions = new ColumnFamilyOptions();
		for (var cf : RocksDB.listColumnFamilies(new Options(), inputDbPath))
			columnFamilies.add(new ColumnFamilyDescriptor(cf, cfOptions));
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(UTF_8), cfOptions));

		if ("true".equalsIgnoreCase(System.getProperty("meta"))) {
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			var levelCount = new int[8];
			var levelSize = new long[8];
			long totalCount = 0, totalSize = 0;
			try (var rocksDb = RocksDB.openReadOnly(new DBOptions(), inputDbPath, columnFamilies, outHandles)) {
				System.out.println("lvl fileName     size  seqNumMin  seqNumMax reads entry delete columnFamilyName");
				System.out.println("-------------------------------------------------------------------------------");
				var metaList = rocksDb.getLiveFilesMetaData();
				metaList.sort(
						Comparator.comparingInt(LiveFileMetaData::level).thenComparing(SstFileMetaData::fileName));
				for (var meta : metaList) {
					var fileName = meta.fileName();
					if (fileName.startsWith("/"))
						fileName = fileName.substring(1);
					System.out.format("%d %10s%9d %10d %10d %5d %5d %6d %s\n", meta.level(), fileName, meta.size(),
							meta.smallestSeqno(), meta.largestSeqno(), meta.numReadsSampled(), meta.numEntries(),
							meta.numDeletions(), new String(meta.columnFamilyName(), UTF_8));
					levelCount[meta.level()]++;
					levelSize[meta.level()] += meta.size();
					totalCount++;
					totalSize += meta.size();
				}
				System.out.println("-------------------------------------------------------------------------------");
			}
			for (int i = 0; i < levelCount.length; i++) {
				var n = levelCount[i];
				if (n != 0)
					System.out.format("count(L%d)  =%6d %,15d bytes\n", i, n, levelSize[i]);
			}
			System.out.format("count(ALL) =%6d %,15d bytes\n", totalCount, totalSize);
			return;
		}

		if ("true".equalsIgnoreCase(System.getProperty("compact"))) {
			System.out.println("INFO: compacting database in '" + inputDbPath + "' ...");
			var t = System.currentTimeMillis();
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			try (var rocksDb = RocksDB.open(new DBOptions(), inputDbPath, columnFamilies, outHandles)) {
				var selColName = columnFamilyName != null ? columnFamilyName.getBytes(UTF_8) : null;
				for (int i = 0; i < columnFamilies.size(); i++) {
					var cf = columnFamilies.get(i);
					if (selColName != null && !Arrays.equals(selColName, cf.getName()))
						continue;
					System.out.println("INFO: compacting '" + new String(cf.getName(), UTF_8) + "' ...");
					rocksDb.compactRange(outHandles.get(i));
				}
			}
			System.out.println("INFO: done! " + (System.currentTimeMillis() - t) + " ms");
			return;
		}

		if ("true".equalsIgnoreCase(System.getProperty("compact1"))) {
			System.out.println("INFO: compacting level-0 to level-1 in '" + inputDbPath + "' ...");
			var t = System.currentTimeMillis();
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			try (var rocksDb = RocksDB.open(new DBOptions(), inputDbPath, columnFamilies, outHandles)) {
				var cOptions = new CompactionOptions();
				var fileList = new ArrayList<String>();
				var selColName = columnFamilyName != null ? columnFamilyName.getBytes(UTF_8) : null;
				for (int i = 0; i < columnFamilies.size(); i++) {
					var cf = columnFamilies.get(i);
					if (selColName != null && !Arrays.equals(selColName, cf.getName()))
						continue;
					for (var meta : rocksDb.getLiveFilesMetaData())
						if (meta.level() == 0 && Arrays.equals(meta.columnFamilyName(), cf.getName()))
							fileList.add(meta.fileName());
					if (!fileList.isEmpty()) {
						System.out.println("INFO: compacting '" + new String(cf.getName(), UTF_8) + "' ...");
						rocksDb.compactFiles(cOptions, outHandles.get(i), fileList, 1, -1, null);
						fileList.clear();
					}
				}
			}
			System.out.println("INFO: done! " + (System.currentTimeMillis() - t) + " ms");
			return;
		}

		if (outputTxtFile == null) {
			System.out.println("INFO: found " + columnFamilies.size() + " column families:");
			for (var cf : columnFamilies)
				System.out.println(new String(cf.getName(), UTF_8));
			return;
		}

		int selectCfIndex = -1;
		if (columnFamilyName == null)
			columnFamilyName = "default";
		for (int i = 0, n = columnFamilies.size(); i < n; i++) {
			if (new String(columnFamilies.get(i).getName(), UTF_8).equals(columnFamilyName)) {
				selectCfIndex = i;
				break;
			}
		}
		if (selectCfIndex < 0) {
			System.out.println("ERROR: not found column family name: '" + columnFamilyName + "'");
			return;
		}

		System.out.println("INFO: dumping column family '" + columnFamilyName + "' to '" + outputTxtFile + "' ...");
		var t = System.currentTimeMillis();
		var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
		try (var rocksDb = RocksDB.openReadOnly(new DBOptions(), inputDbPath, columnFamilies, outHandles);
			 var it = rocksDb.newIterator(outHandles.get(selectCfIndex), new ReadOptions());
			 var os = new BufferedOutputStream(new FileOutputStream(outputTxtFile))) {
			long n = 0;
			for (it.seekToFirst(); it.isValid(); it.next()) {
				dumpAction.run(os, it.key(), it.value());
				n++;
			}
			os.flush();
			System.out.println("INFO: dumped " + n + " records, " + (System.currentTimeMillis() - t) + " ms");
		}
	}

	private static void dump(OutputStream os, String fmt, Object... params) throws IOException {
		os.write(String.format(fmt, params).getBytes(UTF_8));
	}

	private static void dumpRawKV(OutputStream os, byte[] key, byte[] value) throws IOException {
		os.write('\'');
		dumpBytes(os, key);
		os.write('\'');
		os.write(':');
		os.write(' ');
		os.write('\'');
		dumpBytes(os, value);
		os.write('\'');
		os.write('\n');
	}

	private static void dumpRaftLog(OutputStream os, byte[] key, byte[] value) throws Throwable {
		dump(os, "%d: ", ByteBuffer.Wrap(key).ReadLong());
		var bb = ByteBuffer.Wrap(value);
		dump(os, "{term:%d, index:%d, log:", bb.ReadLong(), bb.ReadLong());
		var logType = bb.ReadInt4();
		if (logType == logTypeChanges) {
			int recordCount = bb.ReadUInt();
			if (recordCount > 0)
				dump(os, "changes");
			for (int i = 0; i < recordCount; i++) {
				var tableTId = bb.ReadUInt();
				var tableTName = bb.ReadString();
				var keyName = bb.ReadString();
				var keyBytes = bb.ReadBytes();
				var state = bb.ReadLong();
				dump(os, "{table:%d:%s, key:%s:'%s', s:%d", tableTId, tableTName, keyName, toStr(keyBytes), state);
				if (state == 1) { // Put
					if (tableTName.equals("Global")) {
						var cs = new CacheState();
						cs.Decode(bb);
						dump(os, ", modify:%d, share=%s}", cs.getModify(), cs.getShare());
					} else if (tableTName.equals("Session")) {
						var as = new AcquiredState();
						as.Decode(bb);
						dump(os, ", state:%d}", as.getState());
					} else
						throw new UnsupportedOperationException("unknown table template name: " + tableTName);
				} else if (state == 2) { // Edit
					dump(os, ", edit");
					for (var logBeanCount = bb.ReadUInt(); logBeanCount > 0; logBeanCount--)
						logBeanDecoder.run(os, bb);
				} else if (state != 0) // Remove
					throw new UnsupportedOperationException("unknown state: " + state);
				os.write('}');
			}
			int atomicCount = bb.ReadUInt();
			if (atomicCount > 0) {
				dump(os, " atomics:{");
				for (int i = 0; i < atomicCount; i++)
					dump(os, i == 0 ? "%d:%d" : ",%d:%d", bb.ReadUInt(), bb.ReadLong());
				os.write('}');
			}
		} else if (logType == logTypeHeartbeat) {
			dump(os, "heartbeat{clientId:'%s', reqId:%d, ctime:%d, rpcRes:'%s', op=%d, info='%s'}", bb.ReadString(),
					bb.ReadLong(), bb.ReadLong(), toStr(bb.ReadBytes()), bb.ReadLong(), bb.ReadString());
		} else
			throw new UnsupportedOperationException("unknown raft log type: " + logType);
		os.write('\n');
	}

	private static void dumpBytes(OutputStream os, byte[] bytes) throws IOException {
		for (int b : bytes) {
			if (b >= 0x20 && b <= 0x7e) {
				if (b == '\'' || b == '\\')
					os.write('\\');
				os.write(b);
			} else {
				os.write('\\');
				os.write(num2Hex((b >> 4) & 0xf));
				os.write(num2Hex(b & 0xf));
			}
		}
	}

	private static String toStr(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes) {
			if (b >= 0x20 && b <= 0x7e) {
				if (b == '\'' || b == '\\')
					sb.append('\\');
				sb.append((char)b);
			} else {
				sb.append('\\');
				sb.append((char)num2Hex((b >> 4) & 0xf));
				sb.append((char)num2Hex(b & 0xf));
			}
		}
		return sb.toString();
	}

	private static final int logTypeChanges = Hash32("Zeze.Raft.RocksRaft.Changes");
	private static final int logTypeHeartbeat = Hash32("Zeze.Raft.HeartbeatLog");
	private static final IntHashMap<Action2<OutputStream, ByteBuffer>> logDecoders = new IntHashMap<>();
	private static final Action2<OutputStream, ByteBuffer> logBeanDecoder;

	static {
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<bool>"), (os, bb) -> dump(os, "b:%b", bb.ReadBool()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<byte>"), (os, bb) -> dump(os, "B:%d", bb.ReadLong()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<short>"), (os, bb) -> dump(os, "S:%d", bb.ReadLong()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<int>"), (os, bb) -> dump(os, "I:%d", bb.ReadLong()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<long>"), (os, bb) -> dump(os, "L:%d", bb.ReadLong()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<float>"), (os, bb) -> dump(os, "F:%f", bb.ReadFloat()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<double>"), (os, bb) -> dump(os, "D:%f", bb.ReadDouble()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<string>"), (os, bb) -> dump(os, "'%s'", bb.ReadString()));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.Log<binary>"), (os, bb) -> dump(os, "'%s'", toStr(bb.ReadBytes())));
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.LogBean"), logBeanDecoder = (os, bb) -> {
			os.write('{');
			for (int i = 0, varCount = bb.ReadUInt(); i < varCount; i++) {
				var logTypeId = bb.ReadInt4();
				var logVarId = bb.ReadUInt();
				dump(os, i == 0 ? "%d:" : ",%d:", logVarId);
				var dec = logDecoders.get(logTypeId);
				if (dec == null)
					throw new UnsupportedOperationException("unknown change log type: " + logTypeId);
				dec.run(os, bb);
			}
			os.write('}');
		});
		logDecoders.put(Hash32("Zeze.Raft.RocksRaft.LogSet1<int>"), (os, bb) -> {
			os.write('{');
			for (int i = 0, n = bb.ReadUInt(); i < n; i++)
				dump(os, i == 0 ? "+:%d" : ",%d", bb.ReadLong());
			os.write(';');
			for (int i = 0, n = bb.ReadUInt(); i < n; i++)
				dump(os, i == 0 ? "-:%d" : ",%d", bb.ReadLong());
			os.write('}');
		});
	}

	private DumpRocksDb() {
	}
}
