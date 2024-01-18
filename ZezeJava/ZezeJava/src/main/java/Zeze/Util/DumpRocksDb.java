package Zeze.Util;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquiredState;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BCacheState;
import Zeze.Serialize.ByteBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import static Zeze.Transaction.Bean.hash32;
import static Zeze.Util.BitConverter.num2Hex;
import static java.nio.charset.StandardCharsets.UTF_8;

/*
导出RocksDB的所有记录到文本文件的工具, 可指定column(table)名, 默认是"default"
只提供数据库目录参数时会列出所有的column名
默认的导出方式输出转换为ASCII字符串的二进制key-value数据
可指定key和value的解码方法(long,string,bean,raftLog),输出更加可读的信息

附带列出数据库meta信息(所有在用的sst文件列表)功能(-meta)
附带压缩整理数据库功能(只有此功能会写数据库,其它均为只读), 可指定column(table)名, 默认是所有的column
其中有两种压缩方法, 一种是compactRange(-compact), 另一种是compactFiles(-compact1)
二者都整理log文件到sst中, 后者还会把0级文件整理合并到1级文件
*/
public final class DumpRocksDb {
	public static void main(String[] args) throws Exception {
		int argCount = args.length;
		for (; argCount > 0; argCount--) {
			var arg = args[argCount - 1];
			if (!arg.startsWith("-") || arg.length() <= 1)
				break;
			int p = arg.indexOf('=', 1);
			if (p >= 0)
				System.setProperty(arg.substring(1, p), arg.substring(p + 1));
			else
				System.setProperty(arg.substring(1), "true");
		}
		if (argCount < 1) {
			System.err.println("usage: java -cp ... " + DumpRocksDb.class.getName() +
					" inputDbPath [[columnFamilyName] outputTxtFile] [options]");
			System.err.println("options: -meta          list database meta data");
			System.err.println("         -compact       compact database files");
			System.err.println("         -compact1      compact level-0 files to level-1");
			System.err.println("         -key=long      decode key as long");
			System.err.println("              string    decode key as string");
			System.err.println("              nstring   decode key as len-prefix string");
			System.err.println("              bean      decode key as bean");
			System.err.println("         -value=bean    decode value as bean");
			System.err.println("                raftLog decode value as raft logs for global server");
			return;
		}
		var inputDbPath = args[0];
		var columnFamilyName = argCount > 2 ? args[1] : null;
		var outputTxtFile = argCount == 1 ? null : argCount == 2 ? args[1] : args[2];
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		var cfOptions = new ColumnFamilyOptions();
		var dbOptions = new DBOptions();
		for (var cf : RocksDB.listColumnFamilies(new Options(), inputDbPath))
			columnFamilies.add(new ColumnFamilyDescriptor(cf, cfOptions));
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(UTF_8), cfOptions));

		if ("true".equalsIgnoreCase(System.getProperty("meta"))) {
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			var levelCount = new int[8];
			var levelSize = new long[8];
			long totalCount = 0, totalSize = 0;
			try (var rocksDb = RocksDB.openReadOnly(dbOptions, inputDbPath, columnFamilies, outHandles)) {
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
			System.err.println("INFO: compact database in '" + inputDbPath + "'");
			var t = System.currentTimeMillis();
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			try (var rocksDb = RocksDB.open(dbOptions, inputDbPath, columnFamilies, outHandles)) {
				var selColName = columnFamilyName != null ? columnFamilyName.getBytes(UTF_8) : null;
				for (int i = 0; i < columnFamilies.size(); i++) {
					var cf = columnFamilies.get(i);
					if (selColName != null && !Arrays.equals(selColName, cf.getName()))
						continue;
					System.err.println("INFO: compacting '" + new String(cf.getName(), UTF_8) + "' ...");
					rocksDb.compactRange(outHandles.get(i));
				}
			}
			System.err.println("INFO: done! " + (System.currentTimeMillis() - t) + " ms");
			return;
		}

		if ("true".equalsIgnoreCase(System.getProperty("compact1"))) {
			if (columnFamilyName != null) {
				System.err.println("INFO: compact column family '" + columnFamilyName +
						"' from level-0 to level-1 in '" + inputDbPath + "'");
			} else
				System.err.println("INFO: compact database from level-0 to level-1 in '" + inputDbPath + "'");
			var t = System.currentTimeMillis();
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			try (var rocksDb = RocksDB.open(dbOptions, inputDbPath, columnFamilies, outHandles)) {
				var cOptions = new CompactionOptions();
				var fileList = new ArrayList<String>();
				var selColName = columnFamilyName != null ? columnFamilyName.getBytes(UTF_8) : null;
				for (int i = 0; i < columnFamilies.size(); i++) {
					var cf = columnFamilies.get(i);
					if (selColName != null && !Arrays.equals(selColName, cf.getName()))
						continue;
					for (var meta : rocksDb.getLiveFilesMetaData()) {
						if (meta.level() == 0 && Arrays.equals(meta.columnFamilyName(), cf.getName()))
							fileList.add(meta.fileName());
					}
					if (!fileList.isEmpty()) {
						System.err.println("INFO: compacting '" + new String(cf.getName(), UTF_8) + "' ...");
						rocksDb.compactFiles(cOptions, outHandles.get(i), fileList, 1, -1, null);
						fileList.clear();
					}
				}
			}
			System.err.println("INFO: done! " + (System.currentTimeMillis() - t) + " ms");
			return;
		}

		if (outputTxtFile == null) {
			var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
			try (var ignored = RocksDB.openReadOnly(dbOptions, inputDbPath, columnFamilies, outHandles)) {
				System.out.println("        ID columnFamilyName");
				System.out.println("---------------------------");
				for (var cfh : outHandles)
					System.out.format("%10d %s\n", cfh.getID(), new String(cfh.getName(), UTF_8));
				System.out.println("---------------------------");
				System.out.format("total:%4d column families\n", outHandles.size());
			}
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
			System.err.println("ERROR: not found column family '" + columnFamilyName + "'");
			return;
		}

		System.err.println("INFO: dumping column family '" + columnFamilyName + "' to '" + outputTxtFile + "' ...");
		var t = System.currentTimeMillis();
		Action2<OutputStream, ByteBuffer> keyDumper, valueDumper;
		switch (System.getProperty("key", "")) {
		case "long":
			keyDumper = DumpRocksDb::dumpLong;
			break;
		case "string":
			keyDumper = DumpRocksDb::dumpString;
			break;
		case "nstring":
			keyDumper = DumpRocksDb::dumpNString;
			break;
		case "bean":
			keyDumper = DumpRocksDb::dumpBean;
			break;
		default:
			keyDumper = DumpRocksDb::dumpRaw;
		}
		switch (System.getProperty("value", "")) {
		case "bean":
			valueDumper = DumpRocksDb::dumpBean;
			break;
		case "raftLog":
			valueDumper = DumpRocksDb::dumpRaftLog;
			break;
		default:
			valueDumper = DumpRocksDb::dumpRaw;
		}
		var outHandles = new ArrayList<ColumnFamilyHandle>(columnFamilies.size());
		try (var rocksDb = RocksDB.openReadOnly(dbOptions, inputDbPath, columnFamilies, outHandles);
			 var ro = new ReadOptions();
			 var it = rocksDb.newIterator(outHandles.get(selectCfIndex), ro);
			 var os = outputTxtFile.equals("-")
					 ? System.out
					 : new BufferedOutputStream(new FileOutputStream(outputTxtFile))) {
			long n = 0;
			var key = ByteBuffer.Wrap(ByteBuffer.Empty);
			var value = ByteBuffer.Wrap(ByteBuffer.Empty);
			for (it.seekToFirst(); it.isValid(); it.next()) {
				key.wraps(it.key());
				value.wraps(it.value());
				keyDumper.run(os, key);
				os.write(':');
				os.write(' ');
				valueDumper.run(os, value);
				os.write('\n');
				n++;
			}
			os.flush();
			System.err.println("INFO: dumped " + n + " records, " + (System.currentTimeMillis() - t) + " ms");
		}
	}

	private static void dump(@NotNull OutputStream os, @NotNull String fmt,
							 @Nullable Object... params) throws IOException {
		os.write(String.format(fmt, params).getBytes(UTF_8));
	}

	private static void dumpRaw(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws IOException {
		os.write('\'');
		dumpBytes(os, bb.Bytes);
		os.write('\'');
	}

	private static void dumpLong(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws IOException {
		dump(os, "%d", bb.ReadLong());
	}

	private static void dumpString(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws IOException {
		os.write('"');
		dumpString(os, bb.Bytes);
		os.write('"');
	}

	private static void dumpNString(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws IOException {
		os.write('"');
		dumpString(os, bb.ReadBytes());
		os.write('"');
	}

	private static void dumpBean(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws IOException {
		dumpVar(os, bb, ByteBuffer.BEAN);
	}

	private static void dumpRaftLog(@NotNull OutputStream os, @NotNull ByteBuffer bb) throws Exception {
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
						var cs = new BCacheState();
						cs.decode(bb);
						dump(os, ", modify:%d, share=%s}", cs.getModify(), cs.getShare());
					} else if (tableTName.equals("Session")) {
						var as = new BAcquiredState();
						as.decode(bb);
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

	private static void dumpString(@NotNull OutputStream os, byte @NotNull [] bytes) throws IOException {
		for (var b : bytes) {
			if (b == '"' || b == '\\') // escape
				os.write('\\');
			os.write(b);
		}
	}

	private static void dumpBytes(@NotNull OutputStream os, byte @NotNull [] bytes) throws IOException {
		for (var b : bytes) {
			if (b >= 0x20 && b <= 0x7e) { // printable
				if (b == '\'' || b == '\\') // escape
					os.write('\\');
				os.write(b);
			} else {
				os.write('\\');
				os.write(num2Hex((b >> 4) & 0xf));
				os.write(num2Hex(b & 0xf));
			}
		}
	}

	private static String toStr(byte @NotNull [] bytes) {
		var sb = new StringBuilder(bytes.length * 3);
		for (int b : bytes) {
			if (b >= 0x20 && b <= 0x7e) { // printable
				if (b == '\'' || b == '\\') // escape
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

	private static boolean isUtf8(byte @NotNull [] bytes) {
		for (int i = 0, n = bytes.length; i < n; i++) {
			int b = bytes[i];
			if (b >= 0) { // 0xxx xxxx
				if (b < 0x20 || b == 0x7f) // not printable
					return false;
			} else {
				b &= 0xff;
				if (b < 0xc0 || b >= 0xf0) // 110x xxxx | 1110 xxxx
					return false;
				if (++i >= n || (bytes[i] & 0xc0) != 0xc0) // 10xx xxxx
					return false;
				if (++i >= n || (bytes[i] & 0xc0) != 0xc0) // 10xx xxxx
					return false;
				if (b >= 0xe0 && (++i >= n || (bytes[i] & 0xc0) != 0xc0)) // 10xx xxxx
					return false;
			}
		}
		return true;
	}

	private static void dumpVar(@NotNull OutputStream os, @NotNull ByteBuffer bb, int type) throws IOException {
		switch (type & ByteBuffer.TAG_MASK) {
		case ByteBuffer.INTEGER:
			dump(os, "%d", bb.ReadLong());
			return;
		case ByteBuffer.FLOAT:
			dump(os, "%ff", bb.ReadFloat());
			return;
		case ByteBuffer.DOUBLE:
			dump(os, "%fd", bb.ReadDouble());
			return;
		case ByteBuffer.VECTOR2:
			dump(os, "V2(%f,%f)", bb.ReadFloat(), bb.ReadFloat());
			return;
		case ByteBuffer.VECTOR2INT:
			dump(os, "V2I(%d,%d)", bb.ReadLong(), bb.ReadLong());
			return;
		case ByteBuffer.VECTOR3:
			dump(os, "V3(%f,%f,%f)", bb.ReadFloat(), bb.ReadFloat(), bb.ReadFloat());
			return;
		case ByteBuffer.VECTOR3INT:
			dump(os, "V3I(%d,%d,%d)", bb.ReadLong(), bb.ReadLong(), bb.ReadLong());
			return;
		case ByteBuffer.VECTOR4:
			dump(os, "V4(%f,%f,%f,%f)", bb.ReadFloat(), bb.ReadFloat(), bb.ReadFloat(), bb.ReadFloat());
			return;
		case ByteBuffer.BYTES:
			var bytes = bb.ReadBytes();
			if (isUtf8(bytes)) {
				os.write('"');
				dumpString(os, bytes);
				os.write('"');
			} else {
				os.write('\'');
				dumpBytes(os, bytes);
				os.write('\'');
			}
			return;
		case ByteBuffer.LIST:
			os.write('[');
			int t = bb.ReadByte();
			for (int i = 0, n = bb.ReadTagSize(t); i < n; i++) {
				if (i != 0)
					os.write(',');
				dumpVar(os, bb, t);
			}
			os.write(']');
			return;
		case ByteBuffer.MAP:
			os.write('(');
			int kt = bb.ReadByte() & 0xff;
			int vt = kt & 0xf;
			kt >>= 4;
			for (int i = 0, n = bb.ReadUInt(); i < n; i++) {
				if (i != 0)
					os.write(',');
				dumpVar(os, bb, kt);
				os.write(':');
				dumpVar(os, bb, vt);
			}
			os.write(')');
			return;
		case ByteBuffer.DYNAMIC:
			dump(os, "D%d", bb.ReadLong());
			//noinspection fallthrough
		case ByteBuffer.BEAN:
			os.write('{');
			for (int varId = 0; (t = bb.ReadByte()) != 0; ) {
				if (varId != 0)
					os.write(',');
				if (t == 1) {
					dump(os, "P:"); // parent bean
					continue;
				}
				varId += bb.ReadTagSize(t);
				dump(os, "%d:", varId);
				dumpVar(os, bb, t);
			}
			os.write('}');
			return;
		default:
			throw new IllegalStateException("unknown var type: " + type);
		}
	}

	private static final int logTypeChanges = hash32("Zeze.Raft.RocksRaft.Changes");
	private static final int logTypeHeartbeat = hash32("Zeze.Raft.HeartbeatLog");
	private static final IntHashMap<Action2<OutputStream, ByteBuffer>> logDecoders = new IntHashMap<>();
	private static final @NotNull Action2<OutputStream, ByteBuffer> logBeanDecoder;

	static {
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<bool>"), (os, bb) -> dump(os, "b:%b", bb.ReadBool()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<byte>"), (os, bb) -> dump(os, "B:%d", bb.ReadLong()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<short>"), (os, bb) -> dump(os, "S:%d", bb.ReadLong()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<int>"), (os, bb) -> dump(os, "I:%d", bb.ReadLong()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<long>"), (os, bb) -> dump(os, "L:%d", bb.ReadLong()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<float>"), (os, bb) -> dump(os, "F:%f", bb.ReadFloat()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<double>"), (os, bb) -> dump(os, "D:%f", bb.ReadDouble()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<string>"), (os, bb) -> dump(os, "'%s'", bb.ReadString()));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.Log<binary>"), (os, bb) -> dump(os, "'%s'", toStr(bb.ReadBytes())));
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.LogBean"), logBeanDecoder = (os, bb) -> {
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
		logDecoders.put(hash32("Zeze.Raft.RocksRaft.LogSet1<int>"), (os, bb) -> {
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
