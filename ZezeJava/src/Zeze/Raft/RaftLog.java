package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import RocksDbSharp.*;
import Zeze.Net.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;

public final class RaftLog implements Serializable {
	private long Term;
	public long getTerm() {
		return Term;
	}
	private void setTerm(long value) {
		Term = value;
	}
	private long Index;
	public long getIndex() {
		return Index;
	}
	private void setIndex(long value) {
		Index = value;
	}
	private Log Log;
	public Log getLog() {
		return Log;
	}
	private void setLog(Log value) {
		Log = value;
	}

	// 不会被系列化。Local Only.
	private tangible.Func1Param<Integer, Log> LogFactory;
	public tangible.Func1Param<Integer, Log> getLogFactory() {
		return LogFactory;
	}

	public RaftLog(long term, long index, Log log) {
		setTerm(term);
		setIndex(index);
		setLog(log);
	}

	public RaftLog(tangible.Func1Param<Integer, Log> logFactory) {
		LogFactory = ::logFactory;
	}

	public void Decode(ByteBuffer bb) {
		setTerm(bb.ReadLong());
		setIndex(bb.ReadLong());
		int logTypeId = bb.ReadInt4();
		setLog(LogFactory(logTypeId));
		getLog().Decode(bb);
	}

	public void Encode(ByteBuffer bb) {
		bb.WriteLong(getTerm());
		bb.WriteLong(getIndex());
		bb.WriteInt4(getLog().TypeId);
		getLog().Encode(bb);
	}

	public ByteBuffer Encode() {
		var bb = ByteBuffer.Allocate();
		Encode(bb);
		return bb;
	}

	public static RaftLog Decode(Binary data, tangible.Func1Param<Integer, Log> logFactory) {
		var raftLog = new RaftLog(logFactory);
		data.Decode(raftLog);
		return raftLog;
	}
}