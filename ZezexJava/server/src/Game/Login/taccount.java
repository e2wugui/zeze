package Game.Login;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class taccount extends Zeze.Transaction.Table<String, Game.Login.BAccount> {
	public taccount() {
		super("Game_Login_taccount");
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
	public static final int VAR_Name = 1;
	public static final int VAR_Roles = 2;
	public static final int VAR_LastLoginRoleId = 3;

	@Override
	public String DecodeKey(ByteBuffer _os_) {
		String _v_;
		_v_ = _os_.ReadString();
		return _v_;
	}

	@Override
	public ByteBuffer EncodeKey(String _v_) {
		ByteBuffer _os_ = ByteBuffer.Allocate();
		_os_.WriteString(_v_);
		return _os_;
	}

	@Override
	public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
		return switch (variableId) {
			case 0 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 1 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 2 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			case 3 -> new Zeze.Transaction.ChangeVariableCollectorChanged();
			default -> null;
		};
	}


}