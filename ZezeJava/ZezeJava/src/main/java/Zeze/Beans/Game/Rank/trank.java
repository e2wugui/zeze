// auto-generated @formatter:off
package Zeze.Beans.Game.Rank;

import Zeze.Serialize.ByteBuffer;

public final class trank extends Zeze.Transaction.TableX<Zeze.Beans.Game.Rank.BConcurrentKey, Zeze.Beans.Game.Rank.BRankList> {
    public trank() {
        super("Zeze_Beans_Game_Rank_trank");
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
    public Zeze.Beans.Game.Rank.BConcurrentKey DecodeKey(ByteBuffer _os_) {
        Zeze.Beans.Game.Rank.BConcurrentKey _v_ = new Zeze.Beans.Game.Rank.BConcurrentKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Beans.Game.Rank.BConcurrentKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Beans.Game.Rank.BRankList NewValue() {
        return new Zeze.Beans.Game.Rank.BRankList();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch (variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            default: return null;
        }
    }
}
