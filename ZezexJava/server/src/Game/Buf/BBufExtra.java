package Game.Buf;

import Zeze.Serialize.*;
import Game.*;

public final class BBufExtra extends Zeze.Transaction.Bean implements BBufExtraReadOnly {


	public BBufExtra() {
		this(0);
	}

	public BBufExtra(int _varId_) {
		super(_varId_);
	}

	public void Assign(BBufExtra other) {
	}

	public BBufExtra CopyIfManaged() {
		return isManaged() ? Copy() :this;
	}

	public BBufExtra Copy() {
		var copy = new BBufExtra();
		copy.Assign(this);
		return copy;
	}

	public static void Swap(BBufExtra a, BBufExtra b) {
		BBufExtra save = a.Copy();
		a.Assign(b);
		b.Assign(save);
	}

	@Override
	public Zeze.Transaction.Bean CopyBean() {
		return Copy();
	}

	public static final long TYPEID = 7506982410669108623;
	@Override
	public long getTypeId() {
		return TYPEID;
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
		sb.append(tangible.StringHelper.repeatChar(' ', level * 4)).Append("Game.Buf.BBufExtra: {").Append(System.lineSeparator());
		level++;
		sb.append("}");
	}

	@Override
	public void Encode(ByteBuffer _os_) {
		_os_.WriteInt(0); // Variables.Count
	}

	@Override
	public void Decode(ByteBuffer _os_) {
		for (int _varnum_ = _os_.ReadInt(); _varnum_ > 0; --_varnum_) { // Variables.Count
			int _tagid_ = _os_.ReadInt();
			switch (_tagid_) {
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
		return false;
	}

}