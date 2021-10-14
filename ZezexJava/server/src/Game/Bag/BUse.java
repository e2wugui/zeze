package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

public final class BUse extends Zeze.Transaction.Bean implements BUseReadOnly {
	private int _Position;

	public int getPosition() {
		if (false == this.isManaged()) {
			return _Position;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Position;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Position)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Position;
	}
	public void setPosition(int value) {
		if (false == this.isManaged()) {
			_Position = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Position(this, value));
	}


	public BUse() {
		this(0);
	}

	public BUse(int _varId_) {
		super(_varId_);
	}

	public void Assign(BUse other) {
		setPosition(other.getPosition());
	}

	public BUse CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BUse Copy() {
		var copy = new BUse();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BUse a, BUse b) {
		BUse save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 5557142706984251058;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Position extends Zeze.Transaction.Log<BUse, Integer> {
		public Log__Position(BUse self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Position = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Bag.BUse: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Position").Append("=").Append(getPosition()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getPosition());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setPosition(_os_.ReadInt());
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
		if (getPosition() < 0) {
			return true;
		}
		return false;
	}

}