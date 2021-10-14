// auto-generated
package Game.Rank;

public class SGetRankList extends Zeze.Net.Protocol1<Game.Rank.BRankListResult> {
    public final static int ModuleId_ = 9;
    public final static int ProtocolId_ = 40411;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SGetRankList() {
        Argument = new Game.Rank.BRankListResult();
    }

}
