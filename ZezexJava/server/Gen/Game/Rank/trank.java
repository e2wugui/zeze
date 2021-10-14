// auto-generated
package Game.Rank;

import Zeze.Serialize.*;

public final class trank extends Zeze.Transaction.TableX<Game.Rank.BConcurrentKey, Game.Rank.BRankList> {
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

    public final static int VAR_All = 0;
    public final static int VAR_RankList = 1;

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
    public Game.Rank.BRankList NewValue() {
        return new Game.Rank.BRankList();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch(variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
                default: return null;
            }
        }


}
