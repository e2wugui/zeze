package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BSend extends Zeze.Transaction.Bean implements BSendReadOnly {
	private Zeze.Transaction.Collections.PSet1<Long> _linkSids;
	private int _protocolType;
	private Zeze.Net.Binary _protocolWholeData; // 完整的协议打包，包括了 type, size
	private long _ConfirmSerialId; // 不为0的时候，linkd发送SendConfirm回逻辑服务器

	public Zeze.Transaction.Collections.PSet1<Long> getLinkSids() {
		return _linkSids;
	}
	private System.Collections.Generic.IReadOnlySet<Long> Zezex.Provider.BSendReadOnly.LinkSids -> _linkSids;

	public int getProtocolType() {
		if (false == this.isManaged()) {
			return _protocolType;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _protocolType;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__protocolType)txn.GetLog(this.getObjectId() + 2);
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
		var log = (Log__protocolWholeData)txn.GetLog(this.getObjectId() + 3);
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


	public BSend() {
		this(0);
	}

	public BSend(int _varId_) {
		super(_varId_);
		_linkSids = new Zeze.Transaction.Collections.PSet1<Long>(getObjectId() + 1, _v -> new Log__linkSids(this, _v));
		_protocolWholeData = Zeze.Net.Binary.Empty;
	}

	public void Assign(BSend other) {
		getLinkSids().Clear();
		for (var e : other.getLinkSids()) {
			getLinkSids().Add(e);
		}
		setProtocolType(other.getProtocolType());
		setProtocolWholeData(other.getProtocolWholeData());
		setConfirmSerialId(other.getConfirmSerialId());
	}

	public BSend CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BSend Copy() {
		var copy = new BSend();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BSend a, BSend b) {
		BSend save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 9160848190830466174;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__linkSids extends Zeze.Transaction.Collections.PSet1<Long>.LogV {
		public Log__linkSids(BSend host, System.Collections.Immutable.ImmutableHashSet<Long> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 1;
		}
		public BSend getBeanTyped() {
			return (BSend)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._linkSids);
		}
	}

	private final static class Log__protocolType extends Zeze.Transaction.Log<BSend, Integer> {
		public Log__protocolType(BSend self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolType = this.getValue();
		}
	}

	private final static class Log__protocolWholeData extends Zeze.Transaction.Log<BSend, Zeze.Net.Binary> {
		public Log__protocolWholeData(BSend self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._protocolWholeData = this.getValue();
		}
	}

	private final static class Log__ConfirmSerialId extends Zeze.Transaction.Log<BSend, Long> {
		public Log__ConfirmSerialId(BSend self, long value) {
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BSend: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("LinkSids").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getLinkSids()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolType").Append("=").Append(getProtocolType()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ProtocolWholeData").Append("=").Append(getProtocolWholeData()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ConfirmSerialId").Append("=").Append(getConfirmSerialId()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(4); // Variables.Count
		_os_.WriteInt(ByteBuffer.SET | 1 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.LONG);
			_os_.WriteInt(getLinkSids().Count);
			for (var _v_ : getLinkSids()) {
				_os_.WriteLong(_v_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getProtocolType());
		_os_.WriteInt(ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getProtocolWholeData());
		_os_.WriteInt(ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getConfirmSerialId());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.SET | 1 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getLinkSids().Clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							long _v_;
							_v_ = _os_.ReadLong();
							getLinkSids().Add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setProtocolType(_os_.ReadInt());
					break;
				case ByteBuffer.BYTES | 3 << ByteBuffer.TAG_SHIFT:
					setProtocolWholeData(_os_.ReadBinary());
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
		_linkSids.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		for (var _v_ : getLinkSids()) {
			if (_v_ < 0) {
				return true;
			}
		}
		if (getProtocolType() < 0) {
			return true;
		}
		if (getConfirmSerialId() < 0) {
			return true;
		}
		return false;
	}

}