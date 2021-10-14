// auto-generated
package Game.Rank;

public class CGetRankList extends Zeze.Net.Protocol1<Game.Rank.BGetRankList> {
    public final static int ModuleId_ = 9;
    public final static int ProtocolId_ = 22795;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CGetRankList() {
        Argument = new Game.Rank.BGetRankList();
    }

}
