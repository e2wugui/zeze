package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BModuleRedirectAllRequest extends Zeze.Transaction.Bean implements BModuleRedirectAllRequestReadOnly {
	private int _ModuleId;
	private int _HashCodeConcurrentLevel; // 总的并发分组数量
	private Zeze.Transaction.Collections.PSet1<Integer> _HashCodes; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
	private long _SourceProvider; // linkd 转发的时候填写本地provider的sessionId。
	private long _SessionId; // 发起请求者初始化，返回结果时带回。
	private String _MethodFullName; // format="ModuleFullName:MethodName"
	private Zeze.Net.Binary _Params;
	private String _ServiceNamePrefix;

	public int getModuleId() {
		if (false == this.isManaged()) {
			return _ModuleId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ModuleId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ModuleId)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _ModuleId;
	}
	public void setModuleId(int value) {
		if (false == this.isManaged()) {
			_ModuleId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ModuleId(this, value));
	}

	public int getHashCodeConcurrentLevel() {
		if (false == this.isManaged()) {
			return _HashCodeConcurrentLevel;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _HashCodeConcurrentLevel;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__HashCodeConcurrentLevel)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _HashCodeConcurrentLevel;
	}
	public void setHashCodeConcurrentLevel(int value) {
		if (false == this.isManaged()) {
			_HashCodeConcurrentLevel = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__HashCodeConcurrentLevel(this, value));
	}

	public Zeze.Transaction.Collections.PSet1<Integer> getHashCodes() {
		return _HashCodes;
	}
	private System.Collections.Generic.IReadOnlySet<Integer> Zezex.Provider.BModuleRedirectAllRequestReadOnly.HashCodes -> _HashCodes;

	public long getSourceProvider() {
		if (false == this.isManaged()) {
			return _SourceProvider;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _SourceProvider;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__SourceProvider)txn.GetLog(this.getObjectId() + 4);
		return log != null ? log.getValue() : _SourceProvider;
	}
	public void setSourceProvider(long value) {
		if (false == this.isManaged()) {
			_SourceProvider = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__SourceProvider(this, value));
	}

	public long getSessionId() {
		if (false == this.isManaged()) {
			return _SessionId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _SessionId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__SessionId)txn.GetLog(this.getObjectId() + 5);
		return log != null ? log.getValue() : _SessionId;
	}
	public void setSessionId(long value) {
		if (false == this.isManaged()) {
			_SessionId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__SessionId(this, value));
	}

	public String getMethodFullName() {
		if (false == this.isManaged()) {
			return _MethodFullName;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _MethodFullName;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__MethodFullName)txn.GetLog(this.getObjectId() + 6);
		return log != null ? log.getValue() : _MethodFullName;
	}
	public void setMethodFullName(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_MethodFullName = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__MethodFullName(this, value));
	}

	public Zeze.Net.Binary getParams() {
		if (false == this.isManaged()) {
			return _Params;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Params;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Params)txn.GetLog(this.getObjectId() + 7);
		return log != null ? log.getValue() : _Params;
	}
	public void setParams(Zeze.Net.Binary value) {
		if (null == value) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_Params = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Params(this, value));
	}

	public String getServiceNamePrefix() {
		if (false == this.isManaged()) {
			return _ServiceNamePrefix;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServiceNamePrefix;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServiceNamePrefix)txn.GetLog(this.getObjectId() + 8);
		return log != null ? log.getValue() : _ServiceNamePrefix;
	}
	public void setServiceNamePrefix(String value) {
		if (value.equals(null)) {
			throw new NullPointerException();
		}
		if (false == this.isManaged()) {
			_ServiceNamePrefix = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServiceNamePrefix(this, value));
	}


	public BModuleRedirectAllRequest() {
		this(0);
	}

	public BModuleRedirectAllRequest(int _varId_) {
		super(_varId_);
		_HashCodes = new Zeze.Transaction.Collections.PSet1<Integer>(getObjectId() + 3, _v -> new Log__HashCodes(this, _v));
		_MethodFullName = "";
		_Params = Zeze.Net.Binary.Empty;
		_ServiceNamePrefix = "";
	}

	public void Assign(BModuleRedirectAllRequest other) {
		setModuleId(other.getModuleId());
		setHashCodeConcurrentLevel(other.getHashCodeConcurrentLevel());
		getHashCodes().Clear();
		for (var e : other.getHashCodes()) {
			getHashCodes().Add(e);
		}
		setSourceProvider(other.getSourceProvider());
		setSessionId(other.getSessionId());
		setMethodFullName(other.getMethodFullName());
		setParams(other.getParams());
		setServiceNamePrefix(other.getServiceNamePrefix());
	}

	public BModuleRedirectAllRequest CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BModuleRedirectAllRequest Copy() {
		var copy = new BModuleRedirectAllRequest();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BModuleRedirectAllRequest a, BModuleRedirectAllRequest b) {
		BModuleRedirectAllRequest save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -7064366435104852864;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ModuleId extends Zeze.Transaction.Log<BModuleRedirectAllRequest, Integer> {
		public Log__ModuleId(BModuleRedirectAllRequest self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ModuleId = this.getValue();
		}
	}

	private final static class Log__HashCodeConcurrentLevel extends Zeze.Transaction.Log<BModuleRedirectAllRequest, Integer> {
		public Log__HashCodeConcurrentLevel(BModuleRedirectAllRequest self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._HashCodeConcurrentLevel = this.getValue();
		}
	}

	private final static class Log__HashCodes extends Zeze.Transaction.Collections.PSet1<Integer>.LogV {
		public Log__HashCodes(BModuleRedirectAllRequest host, System.Collections.Immutable.ImmutableHashSet<Integer> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 3;
		}
		public BModuleRedirectAllRequest getBeanTyped() {
			return (BModuleRedirectAllRequest)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._HashCodes);
		}
	}

	private final static class Log__SourceProvider extends Zeze.Transaction.Log<BModuleRedirectAllRequest, Long> {
		public Log__SourceProvider(BModuleRedirectAllRequest self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._SourceProvider = this.getValue();
		}
	}

	private final static class Log__SessionId extends Zeze.Transaction.Log<BModuleRedirectAllRequest, Long> {
		public Log__SessionId(BModuleRedirectAllRequest self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 5;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._SessionId = this.getValue();
		}
	}

	private final static class Log__MethodFullName extends Zeze.Transaction.Log<BModuleRedirectAllRequest, String> {
		public Log__MethodFullName(BModuleRedirectAllRequest self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 6;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._MethodFullName = this.getValue();
		}
	}

	private final static class Log__Params extends Zeze.Transaction.Log<BModuleRedirectAllRequest, Zeze.Net.Binary> {
		public Log__Params(BModuleRedirectAllRequest self, Zeze.Net.Binary value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 7;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Params = this.getValue();
		}
	}

	private final static class Log__ServiceNamePrefix extends Zeze.Transaction.Log<BModuleRedirectAllRequest, String> {
		public Log__ServiceNamePrefix(BModuleRedirectAllRequest self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 8;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServiceNamePrefix = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BModuleRedirectAllRequest: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ModuleId").Append("=").Append(getModuleId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("HashCodeConcurrentLevel").Append("=").Append(getHashCodeConcurrentLevel()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("HashCodes").Append("=[").Append(System.lineSeparator());
		level++;
		for (var Item : getHashCodes()) {
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Item").Append("=").Append(Item).Append(",").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("],").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("SourceProvider").Append("=").Append(getSourceProvider()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("SessionId").Append("=").Append(getSessionId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("MethodFullName").Append("=").Append(getMethodFullName()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Params").Append("=").Append(getParams()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServiceNamePrefix").Append("=").Append(getServiceNamePrefix()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(8); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getModuleId());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getHashCodeConcurrentLevel());
		_os_.WriteInt(ByteBuffer.SET | 3 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(getHashCodes().Count);
			for (var _v_ : getHashCodes()) {
				_os_.WriteInt(_v_);
			}
			_os_.EndWriteSegment(_state_);
		}
		_os_.WriteInt(ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getSourceProvider());
		_os_.WriteInt(ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getSessionId());
		_os_.WriteInt(ByteBuffer.STRING | 6 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getMethodFullName());
		_os_.WriteInt(ByteBuffer.BYTES | 7 << ByteBuffer.TAG_SHIFT);
		_os_.WriteBinary(getParams());
		_os_.WriteInt(ByteBuffer.STRING | 8 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getServiceNamePrefix());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setModuleId(_os_.ReadInt());
					break;
				case ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT:
					setHashCodeConcurrentLevel(_os_.ReadInt());
					break;
				case ByteBuffer.SET | 3 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip collection.value typetag
						getHashCodes().Clear();
						for (int _size_ = _os_.ReadInt(); _size_ > 0; --_size_) {
							int _v_;
							_v_ = _os_.ReadInt();
							getHashCodes().Add(_v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				case ByteBuffer.LONG | 4 << ByteBuffer.TAG_SHIFT:
					setSourceProvider(_os_.ReadLong());
					break;
				case ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT:
					setSessionId(_os_.ReadLong());
					break;
				case ByteBuffer.STRING | 6 << ByteBuffer.TAG_SHIFT:
					setMethodFullName(_os_.ReadString());
					break;
				case ByteBuffer.BYTES | 7 << ByteBuffer.TAG_SHIFT:
					setParams(_os_.ReadBinary());
					break;
				case ByteBuffer.STRING | 8 << ByteBuffer.TAG_SHIFT:
					setServiceNamePrefix(_os_.ReadString());
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_HashCodes.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getModuleId() < 0) {
			return true;
		}
		if (getHashCodeConcurrentLevel() < 0) {
			return true;
		}
		for (var _v_ : getHashCodes()) {
			if (_v_ < 0) {
				return true;
			}
		}
		if (getSourceProvider() < 0) {
			return true;
		}
		if (getSessionId() < 0) {
			return true;
		}
		return false;
	}

}