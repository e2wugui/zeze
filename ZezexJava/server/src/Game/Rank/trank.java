package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class trank extends Zeze.Transaction.Table<Game.Rank.BConcurrentKey, Game.Rank.BRankList> {
	public trank() {
		super("Game_Rank_trank");
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
	public static final int VAR_RankList = 1;

	@Override
	public Game.Rank.BConcurrentKey DecodeKey(ByteBuffer _os_) {
		Game.Rank.BConcurrentKey _v_ = new Game.Rank.BConcurrentKey();
		_v_.Decode(_os_);
		return _v_;
	}

	@Override
	public ByteBuffer EncodeKey(Game.Rank.BConcurrentKey _v_) {
		ByteBuffer _os_ = ByteBuffer.Allocate();
		_v_.Encode(_os_);
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