package Game.Bag;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class tbag extends Zeze.Transaction.Table<Long, Game.Bag.BBag> {
	public tbag() {
		super("Game_Bag_tbag");
	}

	@Override
	public boolean isMemory() {
		return false;
	}
	@Override
	public boolean isAutoKey() {
		return false;
	}

	public static final int VAR_All = 0;
	public static final int VAR_Money = 1;
	public static final int VAR_Capacity = 2;
	public static final int VAR_Items = 3;

	@Override
	public long DecodeKey(ByteBuffer _os_) {
		long _v_;
		_v_ = _os_.ReadLong();
		return _v_;
	}

	@Override
	public ByteBuffer EncodeKey(long _v_) {
		ByteBuffer _os_ = ByteBuffer.Allocate();
		_os_.WriteLong(_v_);
		return _os_;
	}

	@Override
	public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
		return switch (variableId) {
			case 0 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 1 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 2 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 3 -> new Zeze.Transaction.ChangeVariableCollectorMap(() -> new Zeze.Transaction.ChangeNoteMap2<Integer, Game.Bag.BItem>(null));
			default -> null;
		};
	}


}