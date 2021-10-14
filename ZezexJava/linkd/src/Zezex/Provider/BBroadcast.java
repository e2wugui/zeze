package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BBroadcast extends Zeze.Transaction.Bean implements BBroadcastReadOnly {
	private int _protocolType;
	private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
	private int _time;
	private long _ConfirmSerialId; // 不为0的时候，linkd发送SendConfirm回逻辑服务器

	public int getProtocolType() {
		if (false == this.isManaged()) {
			return _protocolType;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _protocolType;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _protocolType;
	}
	public void setProtocolType(int value) {
		if (false == this.isManaged()) {
			_protocolType = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__protocolType(this, value));
	}

	public Zeze.Net.Binary getProtocolWholeData() {
		if (false == this.isManaged()) {
			return _protocolWholeData;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _protocolWholeData;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__protocolWholeData)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _protocolWholeData;
	}
	public void setProtocolWholeData(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_protocolWholeData = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__protocolWholeData(this, value));
	}

	public int getTime() {
		if (false == this.isManaged()) {
			return _time;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _time;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__time)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _time;
	}
	public void setTime(int value) {
		if (false == this.isManaged()) {
			_time = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__time(this, value));
	}

	public long getConfirmSerialId() {
		if (false == this.isManaged()) {
			return _ConfirmSerialId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ConfirmSerialId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ConfirmSerialId)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _ConfirmSerialId;
	}
	public void setConfirmSerialId(long value) {
		if (false == this.isManaged()) {
			_ConfirmSerialId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ConfirmSerialId(this, value));
	}


	public BBroadcast() {
		this(0);
	}

	public BBroadcast(int _varId_) {
		super(_varId_);
		_protocolWholeData = Zeze.Net.Binary.Empty;
	}

	public void Assign(BBroadcast other) {
		setProtocolType(other.getProtocolType());
		setProtocolWholeData(other.getProtocolWholeData());
		setTime(other.getTime());
		setConfirmSerialId(other.getConfirmSerialId());
	}

	public BBroadcast CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBroadcast Copy() {
		var copy = new BBroadcast();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBroadcast a, BBroadcast b) {
		BBroadcast save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 3473896187150859593;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__protocolType extends Zeze.Transaction.Log<BBroadcast, Integer> {
		public Log__protocolType(BBroadcast self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolType = this.getValue();
		}
	}

	private final static class Log__protocolWholeData extends Zeze.Transaction.Log<BBroadcast, Zeze.Net.Binary> {
		public Log__protocolWholeData(BBroadcast self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolWholeData = this.getValue();
		}
	}

	private final static class Log__time extends Zeze.Transaction.Log<BBroadcast, Integer> {
		public Log__time(BBroadcast self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._time = this.getValue();
		}
	}

	private final static class Log__ConfirmSerialId extends Zeze.Transaction.Log<BBroadcast, Long> {
		public Log__ConfirmSerialId(BBroadcast self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ConfirmSerialId = this.getValue();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		BuildString(sb, 0);
		sb.append(System.lineSeparator());
		return sb.toString();
	}

	@Override
	public void BuildString(StringBuilder sb, int level) {
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BBroadcast: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolType").Append("=").Append(getProtocolType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolWholeData").Append("=").Append(getProtocolWholeData()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Time").Append("=").Append(getTime()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ConfirmSerialId").Append("=").Append(getConfirmSerialId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(4); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getProtocolType());
		_os_.WriteInt(ByteBuffer.BYTES | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getProtocolWholeData());
		_os_.WriteInt(ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getTime());
		_os_.WriteInt(ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getConfirmSerialId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setProtocolType(_os_.ReadInt());
					break;
				case ByteBuffer.BYTES | 2 << ByteBuffer.TAG_SHIFT:
					setProtocolWholeData(_os_.ReadBinary());
					break;
				case ByteBuffer.INT | 3 << ByteBuffer.TAG_SHIFT:
					setTime(_os_.ReadInt());
					break;
				case ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT:
					setConfirmSerialId(_os_.ReadLong());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}

	@Override
	public boolean NegativeCheck() {
		if (getProtocolType() < 0) {
			return true;
		}
		if (getTime() < 0) {
			return true;
		}
		if (getConfirmSerialId() < 0) {
			return true;
		}
		return false;
	}

}