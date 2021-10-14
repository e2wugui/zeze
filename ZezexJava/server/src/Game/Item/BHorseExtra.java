package Game.Item;

import Zeze.Serialize.*;
import Game.*;

public final class BHorseExtra extends Zeze.Transaction.Bean implements BHorseExtraReadOnly {
	private int _Speed;

	public int getSpeed() {
		if (false == this.isManaged()) {
			return _Speed;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		if (txn == null) {
			return _Speed;
		}
		txn.VerifyRecordAccessed(this, true);
		var log = (Log__Speed)txn.GetLog(this.getObjectId() + 1);
		return log != null ? log.getValue() : _Speed;
	}
	public void setSpeed(int value) {
		if (false == this.isManaged()) {
			_Speed = value;
			return;
		}
		var txn = Zeze.Transaction.Transaction.Current;
		txn.VerifyRecordAccessed(this, false);
		txn.PutLog(new Log__Speed(this, value));
	}


	public BHorseExtra() {
		this(0);
	}

	public BHorseExtra(int _varId_) {
		super(_varId_);
	}

	public void Assign(BHorseExtra other) {
		setSpeed(other.getSpeed());
	}

	public BHorseExtra CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BHorseExtra Copy() {
		var copy = new BHorseExtra();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BHorseExtra a, BHorseExtra b) {
		BHorseExtra save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = -6414823809446200925;
	@Override
	public long getTypeId() {
		return TYPEID;
	}

	private final static class Log__Speed extends Zeze.Transaction.Log<BHorseExtra, Integer> {
		public Log__Speed(BHorseExtra self, int value) {
			super(self, value);
		}
		@Override
		public long getLogKey() {
			return this.Bean.ObjectId + 1;
		}
		@Override
		public void Commit() {
			this.getBeanTyped()._Speed = this.getValue();
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Item.BHorseExtra: {").Append(System.lineSeparator());
		level++;
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Speed").Append("=").Append(getSpeed()).Append("").Append(System.lineSeparator());
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(1); // Variables.Count
		_os_.WriteInt(ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT);
		_os_.WriteInt(getSpeed());
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
				case ByteBuffer.INT | 1 << ByteBuffer.TAG_SHIFT:
					setSpeed(_os_.ReadInt());
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
		if (getSpeed() < 0) {
			return true;
		}
		return false;
	}

}