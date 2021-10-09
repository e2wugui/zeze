package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Util.*;
import Zeze.*;
import java.io.*;

//C# TO JAVA CONVERTER TODO TASK: The C# 'new()' constraint has no equivalent in Java:
//ORIGINAL LINE: public class ConcurrentMap<K, V> where V : Copyable<V>, new()
public class ConcurrentMap<K, V extends Copyable<V>> {
	private HugeConcurrentDictionary<K, V> Map;
	private HugeConcurrentDictionary<K, V> getMap() {
		return Map;
	}

	private enum SnapshotState {
		Zero,
		Add,
		Update,
		Remove;

		public static final int SIZE = java.lang.Integer.SIZE;

		public int getValue() {
			return this.ordinal();
		}

		public static SnapshotState forValue(int value) {
			return values()[value];
		}
	}
	private static class SnapshotValue {
		private V Value;
		public final V getValue() {
			return Value;
		}
		public final V void setValue(V value) {
			Value = value;
		}
		private SnapshotState State = SnapshotState.Zero;
		public final SnapshotState getState() {
			return State;
		}
		public final void setState(SnapshotState value) {
			State = value;
		}
	}

	private java.util.concurrent.ConcurrentHashMap<K, SnapshotValue> SnapshotLogs = new java.util.concurrent.ConcurrentHashMap<K, SnapshotValue> ();
	private java.util.concurrent.ConcurrentHashMap<K, SnapshotValue> getSnapshotLogs() {
		return SnapshotLogs;
	}
	/** 
	 【Snapshot状态和操作】
	 State | add remove update
	 Zero  | Add Remove Update
	 Add   | _   _      _
	 Update| x   Remove _
	 Remove| _   _      _   (x: Error _: No Change)
	 
	 Zero:
	 1. SnapshotValue第一次创建时：根据操作设置对应状态。
	 2. Add再Remove时设置成这个状态：此时只会发生新的Add操作。
	 Add 和 Remove:
	 黑洞，一旦进入就不会再改变。
	*/
	private void SnapshotLog(SnapshotState op, K k, V value) {
		if (false == Snapshoting) {
			return;
		}

		var ss = getSnapshotLogs().putIfAbsent(k, (_) -> new SnapshotValue());
		synchronized (ss) {
			switch (ss.State) {
				case SnapshotState.Zero:
					switch (op) {
						case Add:
							ss.State = SnapshotState.Add;
							ss.Value = value;
							break;
						case Update:
							ss.State = SnapshotState.Update;
							ss.Value = value.Copy();
							break;
						case Remove:
							ss.State = SnapshotState.Remove;
							ss.Value = value;
							break;
					}
					break;
				case SnapshotState.Add:
					// all no change
					break;
				case SnapshotState.Update:
					switch (op) {
						case Add:
							throw new RuntimeException("Update->Add Impossible");
						case Update:
							break; // no change
						case Remove:
							// Value no change
							ss.State = SnapshotState.Remove;
							break;
					}
					break;
				case SnapshotState.Remove:
					// all no change
					break;
			}
		}
	}

	public final long getCount() {
		return getMap().getCount();
	}

	// 需要外面更大锁来保护。Raft.StateMachine 的子类内加锁。
	private boolean Snapshoting = false;


	public ConcurrentMap(int buckets, int concurrencyLevel) {
		this(buckets, concurrencyLevel, 1000000);
	}

	public ConcurrentMap(int buckets) {
		this(buckets, 1024, 1000000);
	}

	public ConcurrentMap() {
		this(16, 1024, 1000000);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public ConcurrentMap(int buckets = 16, int concurrencyLevel = 1024, long initialCapacity = 1000000)
	public ConcurrentMap(int buckets, int concurrencyLevel, long initialCapacity) {
		Map = new HugeConcurrentDictionary<K, V>(buckets, concurrencyLevel, initialCapacity);
	}

	public final V GetOrAdd(K key) {
		return GetOrAdd(key, (_) -> new V());
	}

	public final V GetOrAdd(K key, tangible.Func1Param<K, V> valueFactory) {
		return getMap().GetOrAdd(key, (k) -> {
					V v = valueFactory.invoke(k);
					SnapshotLog(SnapshotState.Add, k, v);
					return v;
		});
	}

	public final void Update(K k, tangible.Action1Param<V> updator) {
		Update(k, GetOrAdd(k), updator);
	}

	public final void Update(K k, V v, tangible.Action1Param<V> updator) {
		SnapshotLog(SnapshotState.Update, k, v);
		// log before real update
		updator.invoke(v);
	}

	public final void Remove(K k) {
		V removed;
		tangible.OutObject<V> tempOut_removed = new tangible.OutObject<V>();
		if (getMap().TryRemove(k, tempOut_removed)) {
		removed = tempOut_removed.outArgValue;
			SnapshotLog(SnapshotState.Remove, k, removed);
		}
	else {
		removed = tempOut_removed.outArgValue;
	}
	}

	// 线程不安全，需要外面更大的锁来保护。
	public final boolean StartSerialize() {
		if (Snapshoting) {
			return false;
		}
		Snapshoting = true;
		getSnapshotLogs().clear();
		return true;
	}

	// 线程不安全，需要外面更大的锁来保护。
	public final void EndSerialize() {
		Snapshoting = false;
		getSnapshotLogs().clear();
	}

	private void WriteTo(OutputStream stream, K k, V v) {
		// 外面使用 Update 修改记录，第一次修改时会复制，所以这个 lock 不是必要的。
		synchronized (v) {
			var bb = ByteBuffer.Allocate();

			int state;
			tangible.OutObject<Integer> tempOut_state = new tangible.OutObject<Integer>();
			bb.BeginWriteWithSize4(tempOut_state);
		state = tempOut_state.outArgValue;
			SerializeHelper<K>.Encode(bb, k);
			v.Encode(bb);
			bb.EndWriteWithSize4(state);

			stream.write(bb.getBytes(), bb.getReadIndex(), bb.getSize());
		}
	}


	private long WriteLong8To(System.IO.Stream stream, long i) {
		return WriteLong8To(stream, i, -1);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: private long WriteLong8To(System.IO.Stream stream, long i, long offset = -1)
	private long WriteLong8To(OutputStream stream, long i, long offset) {
		if (offset >= 0) {
			stream.Seek(offset, System.IO.SeekOrigin.Begin);
		}
		var position = stream.Position;
		stream.Write(BitConverter.GetBytes(i));
		if (offset >= 0) {
			stream.Seek(0, System.IO.SeekOrigin.End);
		}
		return position;
	}

	private long ReadLong8From(InputStream stream) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var bytes = new byte[8];
		var bytes = new byte[8];
		stream.Read(bytes);
		return BitConverter.ToInt64(bytes);
	}

	private int ReadInt4From(InputStream stream) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var bytes = new byte[4];
		var bytes = new byte[4];
		stream.Read(bytes);
		return BitConverter.ToInt32(bytes);
	}

//C# TO JAVA CONVERTER TODO TASK: C# to Java Converter cannot determine whether this System.IO.Stream is input or output:
	public final void SerializeTo(System.IO.Stream stream) {
		var position = WriteLong8To(stream, getMap().getCount());

		long SnapshotCount = 0;
		for (var cur : getMap()) {
			TValue log;
			tangible.OutObject<TValue> tempOut_log = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getSnapshotLogs().TryGetValue(cur.Key, tempOut_log)) {
			log = tempOut_log.outArgValue;
				switch (log.State) {
					case SnapshotState.Add:
						// 新增的记录不需要写出去。
						continue;
					case SnapshotState.Remove:
						// Remove状态后面统一处理
						break;
					case SnapshotState.Update:
						// changed 里面保存的是Update前的项。
						WriteTo(stream, cur.Key, log.Value);
						break;
				}
			}
			else {
			log = tempOut_log.outArgValue;
				WriteTo(stream, cur.Key, cur.Value);
			}
			++SnapshotCount;
		}
		for (var e : getSnapshotLogs()) {
			if (e.getValue().State == SnapshotState.Remove) {
				// 删除前的项。
				WriteTo(stream, e.Key, e.getValue().Value);
				++SnapshotCount;
			}
		}
		WriteLong8To(stream, SnapshotCount, position);
		stream.Seek(0, System.IO.SeekOrigin.End);
	}

	// 线程不安全，需要外面更大的锁保护。
	public final void UnSerializeFrom(InputStream stream) {
		if (Snapshoting) {
			throw new RuntimeException("Coucurrent Error: In Snapshoting");
		}

		getMap().Clear();
		for (long count = ReadLong8From(stream); count > 0; --count) {
			int kvsize = ReadInt4From(stream);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: var kvbytes = new byte[kvsize];
			var kvbytes = new byte[kvsize];
			stream.Read(kvbytes);

			var bb = ByteBuffer.Wrap(kvbytes);

			K key = SerializeHelper<K>.Decode(bb);
			V value = new V();
			value.Decode(bb);
			getMap().set(key, value); // ignore result
		}
	}
}