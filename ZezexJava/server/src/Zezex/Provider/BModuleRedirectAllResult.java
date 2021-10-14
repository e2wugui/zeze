package Zezex.Provider;

import Zeze.Serialize.*;
import Zezex.*;

public final class BModuleRedirectAllResult extends Zeze.Transaction.Bean implements BModuleRedirectAllResultReadOnly {
	private int _ModuleId;
	private int _ServerId; // 目标server的id。
	private long _SourceProvider; // 从BModuleRedirectAllRequest里面得到。
	private String _MethodFullName; // format="ModuleFullName:MethodName"
	private long _SessionId; // 发起请求者初始化，返回结果时带回。
	private Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash> _Hashs; // 发送给具体进程时需要处理的分组hash-index（目前由linkd填写）
	private Zeze.Transaction.Collections.PMapReadOnly<Integer,Zezex.Provider.BModuleRedirectAllHashReadOnly,Zezex.Provider.BModuleRedirectAllHash> _HashsReadOnly;

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

	public int getServerId() {
		if (false == this.isManaged()) {
			return _ServerId;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ServerId;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ServerId)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _ServerId;
	}
	public void setServerId(int value) {
		if (false == this.isManaged()) {
			_ServerId = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ServerId(this, value));
	}

	public long getSourceProvider() {
		if (false == this.isManaged()) {
			return _SourceProvider;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _SourceProvider;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__SourceProvider)txn.GetLog(this.getObjectId() + 3);
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

	public String getMethodFullName() {
		if (false == this.isManaged()) {
			return _MethodFullName;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _MethodFullName;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__MethodFullName)txn.GetLog(this.getObjectId() + 4);
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

	public Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash> getHashs() {
		return _Hashs;
	}
	private System.Collections.Generic.IReadOnlyDictionary<Integer,Zezex.Provider.BModuleRedirectAllHashReadOnly> Zezex.Provider.BModuleRedirectAllResultReadOnly.Hashs -> _HashsReadOnly;


	public BModuleRedirectAllResult() {
		this(0);
	}

	public BModuleRedirectAllResult(int _varId_) {
		super(_varId_);
		_MethodFullName = "";
		_Hashs = new Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash>(getObjectId() + 6, _v -> new Log__Hashs(this, _v));
		_HashsReadOnly = new Zeze.Transaction.Collections.PMapReadOnly<Integer,Zezex.Provider.BModuleRedirectAllHashReadOnly,Zezex.Provider.BModuleRedirectAllHash>(_Hashs);
	}

	public void Assign(BModuleRedirectAllResult other) {
		setModuleId(other.getModuleId());
		setServerId(other.getServerId());
		setSourceProvider(other.getSourceProvider());
		setMethodFullName(other.getMethodFullName());
		setSessionId(other.getSessionId());
		getHashs().Clear();
		for (var e : other.getHashs()) {
			getHashs().Add(e.Key, e.Value.Copy());
		}
	}

	public BModuleRedirectAllResult CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BModuleRedirectAllResult Copy() {
		var copy = new BModuleRedirectAllResult();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BModuleRedirectAllResult a, BModuleRedirectAllResult b) {
		BModuleRedirectAllResult save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 1951985867510056420;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__ModuleId extends Zeze.Transaction.Log<BModuleRedirectAllResult, Integer> {
		public Log__ModuleId(BModuleRedirectAllResult self, int value) {
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

	private final static class Log__ServerId extends Zeze.Transaction.Log<BModuleRedirectAllResult, Integer> {
		public Log__ServerId(BModuleRedirectAllResult self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ServerId = this.getValue();
		}
	}

	private final static class Log__SourceProvider extends Zeze.Transaction.Log<BModuleRedirectAllResult, Long> {
		public Log__SourceProvider(BModuleRedirectAllResult self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._SourceProvider = this.getValue();
		}
	}

	private final static class Log__MethodFullName extends Zeze.Transaction.Log<BModuleRedirectAllResult, String> {
		public Log__MethodFullName(BModuleRedirectAllResult self, String value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 4;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._MethodFullName = this.getValue();
		}
	}

	private final static class Log__SessionId extends Zeze.Transaction.Log<BModuleRedirectAllResult, Long> {
		public Log__SessionId(BModuleRedirectAllResult self, long value) {
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

	private final static class Log__Hashs extends Zeze.Transaction.Collections.PMap2<Integer, Zezex.Provider.BModuleRedirectAllHash>.LogV {
		public Log__Hashs(BModuleRedirectAllResult host, System.Collections.Immutable.ImmutableDictionary<Integer, Zezex.Provider.BModuleRedirectAllHash> value) {
			super(host, value);
		}
		@Override
		public long getLogKey() {
			return Bean.ObjectId + 6;
		}
		public BModuleRedirectAllResult getBeanTyped() {
			return (BModuleRedirectAllResult)Bean;
		}
		@Override
		public void Commit() {
			Commit(getBeanTyped()._Hashs);
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Zezex.Provider.BModuleRedirectAllResult: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ModuleId").Append("=").Append(getModuleId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ServerId").Append("=").Append(getServerId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("SourceProvider").Append("=").Append(getSourceProvider()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("MethodFullName").Append("=").Append(getMethodFullName()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("SessionId").Append("=").Append(getSessionId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Hashs").Append("=[").Append(System.lineSeparator());
		level++;
		for (var _kv_ : getHashs()) {
			sb.append("(").Append(System.lineSeparator());
			var Key = _kv_.Key;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Key").Append("=").Append(Key).Append(",").Append(System.lineSeparator());
			var Value = _kv_.Value;
			sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Value").Append("=").Append(System.lineSeparator());
			Value.BuildString(sb, level + 1);
			sb.append(",").Append(System.lineSeparator());
			sb.append(")").Append(System.lineSeparator());
		}
		level--;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("]").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(6); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getModuleId());
		_os_.WriteInt(ByteBuffer.INT | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getServerId());
		_os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getSourceProvider());
		_os_.WriteInt(ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT);
		_os_.WriteString(getMethodFullName());
		_os_.WriteInt(ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getSessionId());
		_os_.WriteInt(ByteBuffer.MAP | 6 << ByteBuffer.TAG_SHIFT); {
			int _state_;
			tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
			_os_.BeginWriteSegment(tempOut__state_);
		_state_ = tempOut__state_.outArgValue;
			_os_.WriteInt(ByteBuffer.INT);
			_os_.WriteInt(ByteBuffer.BEAN);
			_os_.WriteInt(getHashs().Count);
			for (var _e_ : getHashs()) {
				_os_.WriteInt(_e_.Key);
				_e_.Value.Encode(_os_);
			}
			_os_.EndWriteSegment(_state_);
		}
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
					setServerId(_os_.ReadInt());
					break;
				case ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT:
					setSourceProvider(_os_.ReadLong());
					break;
				case ByteBuffer.STRING | 4 << ByteBuffer.TAG_SHIFT:
					setMethodFullName(_os_.ReadString());
					break;
				case ByteBuffer.LONG | 5 << ByteBuffer.TAG_SHIFT:
					setSessionId(_os_.ReadLong());
					break;
				case ByteBuffer.MAP | 6 << ByteBuffer.TAG_SHIFT: {
						int _state_;
						tangible.OutObject<Integer> tempOut__state_ = new tangible.OutObject<Integer>();
						_os_.BeginReadSegment(tempOut__state_);
					_state_ = tempOut__state_.outArgValue;
						_os_.ReadInt(); // skip key typetag
						_os_.ReadInt(); // skip value typetag
						getHashs().Clear();
						for (int size = _os_.ReadInt(); size > 0; --size) {
							int _k_;
							_k_ = _os_.ReadInt();
							Zezex.Provider.BModuleRedirectAllHash _v_ = new Zezex.Provider.BModuleRedirectAllHash();
							_v_.Decode(_os_);
							getHashs().Add(_k_, _v_);
						}
						_os_.EndReadSegment(_state_);
				}
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Hashs.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getModuleId() < 0) {
			return true;
		}
		if (getServerId() < 0) {
			return true;
		}
		if (getSourceProvider() < 0) {
			return true;
		}
		if (getSessionId() < 0) {
			return true;
		}
		for (var _v_ : getHashs().Values) {
			if (_v_.NegativeCheck()) {
				return true;
			}
		}
		return false;
	}

}