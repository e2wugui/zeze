package Game.Fight;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class tfighters extends Zeze.Transaction.Table<Game.Fight.BFighterId, Game.Fight.BFighter> {
	public tfighters() {
		super("Game_Fight_tfighters");
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
	public static final int VAR_Attack = 1;
	public static final int VAR_Defence = 2;

	@Override
	public Game.Fight.BFighterId DecodeKey(ByteBuffer _os_) {
		Game.Fight.BFighterId _v_ = new Game.Fight.BFighterId();
		_v_.Decode(_os_);
		return _v_;
	}

	@Override
	public ByteBuffer EncodeKey(Game.Fight.BFighterId _v_) {
		ByteBuffer _os_ = ByteBuffer.Allocate();
		_v_.Encode(_os_);
		return _os_;
	}

	@Override
	public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
		return switch (variableId) {
			case 0 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 1 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 2 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			default -> null;
		};
	}


}