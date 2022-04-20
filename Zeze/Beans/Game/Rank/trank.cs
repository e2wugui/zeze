// auto-generated
using Zeze.Serialize;

namespace Zeze.Beans.Game.Rank
{
    public sealed class trank : Zeze.Transaction.Table<Zeze.Beans.Game.Rank.BConcurrentKey, Zeze.Beans.Game.Rank.BRankList>
    {
        public trank() : base("Zeze_Beans_Game_Rank_trank")
        {
        }

        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_All = 0;
        public const int VAR_RankList = 1;

        public override Zeze.Beans.Game.Rank.BConcurrentKey DecodeKey(ByteBuffer _os_)
        {
            Zeze.Beans.Game.Rank.BConcurrentKey _v_ = new Zeze.Beans.Game.Rank.BConcurrentKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Beans.Game.Rank.BConcurrentKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        public override Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId)
        {
            return variableId switch
            {
                0 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                1 => new Zeze.Transaction.ChangeVariableCollectorChanged(),
                _ => null,
            };
        }
    }
}
