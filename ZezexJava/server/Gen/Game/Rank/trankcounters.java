// auto-generated
package Game.Rank;

import Zeze.Serialize.*;

public final class trankcounters extends Zeze.Transaction.TableX<Long, Game.Rank.BRankCounters> {
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

    public final static int VAR_All = 0;
    public final static int VAR_Counters = 1;

    @Override
    public Long DecodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate();
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Game.Rank.BRankCounters NewValue() {
        return new Game.Rank.BRankCounters();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorMap(() -> new Zeze.Transaction.ChangeNoteMap2<Game.Rank.BConcurrentKey, Game.Rank.BRankCounter>(null));
                default: return null;
            }
        }


}
