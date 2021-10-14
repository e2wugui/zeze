package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class trole extends Zeze.Transaction.Table<Long, Game.Login.BRoleData> {
	public trole() {
		super("Game_Login_trole");
	}

	@Override
	public boolean isMemory() {
		return false;
	}
	@Override
	public boolean isAutoKey() {
		return true;
	}

	public static final int VAR_All = 0;
	public static final int VAR_Name = 1;

	public long Insert(Game.Login.BRoleData value) {
		long key = getAutoKey().Next();
		Insert(key, value);
		return key;
	}

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
			default -> null;
		};
	}


}