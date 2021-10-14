package Game.Rank;

import Zeze.Serialize.*;
import Game.*;

// auto-generated



public final class trankcounters extends Zeze.Transaction.Table<Long, Game.Rank.BRankCounters> {
	public trankcounters() {
		super("Game_Rank_trankcounters");
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
	public static final int VAR_Counters = 1;

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
			case 1 -> new Zeze.Transaction.ChangeVariableCollectorMap(() -> new Zeze.Transaction.ChangeNoteMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter>(null));
			default -> null;
		};
	}


}