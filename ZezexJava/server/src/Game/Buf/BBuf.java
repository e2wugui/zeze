package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

public final class BBuf extends Zeze.Transaction.Bean implements BBufReadOnly {
	private int _Id;
	private long _AttachTime; // 加入时间
	private long _ContinueTime; // 持续时间
	private Zeze.Transaction.DynamicBean _Extra;

	public int getId() {
		if (false == this.isManaged()) {
			return _Id;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Id;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Id)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Id;
	}
	public void setId(int value) {
		if (false == this.isManaged()) {
			_Id = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Id(this, value));
	}

	public long getAttachTime() {
		if (false == this.isManaged()) {
			return _AttachTime;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _AttachTime;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__AttachTime)txn.GetLog(this.getObjectId() + 2);
		return log != null ? log.getValue() : _AttachTime;
	}
	public void setAttachTime(long value) {
		if (false == this.isManaged()) {
			_AttachTime = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__AttachTime(this, value));
	}

	public long getContinueTime() {
		if (false == this.isManaged()) {
			return _ContinueTime;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _ContinueTime;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__ContinueTime)txn.GetLog(this.getObjectId() + 3);
		return log != null ? log.getValue() : _ContinueTime;
	}
	public void setContinueTime(long value) {
		if (false == this.isManaged()) {
			_ContinueTime = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__ContinueTime(this, value));
	}

	public Zeze.Transaction.DynamicBean getExtra() {
		return _Extra;
	}
	private Zeze.Transaction.DynamicBeanReadOnly Game.Buf.BBufReadOnly.Extra -> getExtra();

	public Game.Buf.BBufExtra getExtraGameBufBBufExtra() {
		return (Game.Buf.BBufExtra)getExtra().Bean;
	}
	public void setExtraGameBufBBufExtra(Game.Buf.BBufExtra value) {
		getExtra().Bean = value;
	}

	private Game.Buf.BBufExtraReadOnly Game.Buf.BBufReadOnly.Extra_Game_Buf_BBufExtra -> getExtraGameBufBBufExtra();


	public BBuf() {
		this(0);
	}

	public BBuf(int _varId_) {
		super(_varId_);
		_Extra = new Zeze.Transaction.DynamicBean(4, GetSpecialTypeIdFromBean_Extra, CreateBeanFromSpecialTypeId_Extra);
	}

	public void Assign(BBuf other) {
		setId(other.getId());
		setAttachTime(other.getAttachTime());
		setContinueTime(other.getContinueTime());
		getExtra().Assign(other.getExtra());
	}

	public BBuf CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBuf Copy() {
		var copy = new BBuf();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBuf a, BBuf b) {
		BBuf save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -4634900835369009583;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Id extends Zeze.Transaction.Log<BBuf, Integer> {
		public Log__Id(BBuf self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Id = this.getValue();
		}
	}

	private final static class Log__AttachTime extends Zeze.Transaction.Log<BBuf, Long> {
		public Log__AttachTime(BBuf self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 2;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._AttachTime = this.getValue();
		}
	}

	private final static class Log__ContinueTime extends Zeze.Transaction.Log<BBuf, Long> {
		public Log__ContinueTime(BBuf self, long value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 3;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._ContinueTime = this.getValue();
		}
	}

	public static long GetSpecialTypeIdFromBean_Extra(Zeze.Transaction.Bean bean) {
		switch (bean.TypeId) {
			case Zeze.Transaction.EmptyBean.TYPEID:
				return Zeze.Transaction.EmptyBean.TYPEID;
			case 7506982410669108623:
				return 7506982410669108623; // Game.Buf.BBufExtra
		}
		throw new RuntimeException("Unknown Bean! dynamic@Game.Buf.BBuf:Extra");
	}

	public static Zeze.Transaction.Bean CreateBeanFromSpecialTypeId_Extra(long typeId) {
		switch (typeId) {
			case 7506982410669108623:
				return new Game.Buf.BBufExtra();
		}
		return null;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Buf.BBuf: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Id").Append("=").Append(getId()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("AttachTime").Append("=").Append(getAttachTime()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("ContinueTime").Append("=").Append(getContinueTime()).Append(",").Append(System.lineSeparator());
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Extra").Append("=").Append(System.lineSeparator());
		getExtra().Bean.BuildString(sb, level + 1);
		sb.append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(4); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getId());
		_os_.WriteInt(ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getAttachTime());
		_os_.WriteInt(ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT);
		_os_.WriteLong(getContinueTime());
		_os_.WriteInt(ByteBuffer.DYNAMIC | 4 << ByteBuffer.TAG_SHIFT);
		getExtra().Encode(_os_);
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setId(_os_.ReadInt());
					break;
				case ByteBuffer.LONG | 2 << ByteBuffer.TAG_SHIFT:
					setAttachTime(_os_.ReadLong());
					break;
				case ByteBuffer.LONG | 3 << ByteBuffer.TAG_SHIFT:
					setContinueTime(_os_.ReadLong());
					break;
				case ByteBuffer.DYNAMIC | 4 << ByteBuffer.TAG_SHIFT:
					getExtra().Decode(_os_);
					break;
				default:
					ByteBuffer.SkipUnknownField(_tagid_, _os_);
					break;
			}
		}
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
		_Extra.InitRootInfo(root, this);
	}

	@Override
	public boolean NegativeCheck() {
		if (getId() < 0) {
			return true;
		}
		if (getAttachTime() < 0) {
			return true;
		}
		if (getContinueTime() < 0) {
			return true;
		}
		return false;
	}

}