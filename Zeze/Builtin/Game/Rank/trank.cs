// auto-generated
using Zeze.Serialize;
using Zeze.Transaction;
using System.Threading.Tasks;

// ReSharper disable JoinDeclarationAndInitializer RedundantNameQualifier
namespace Zeze.Builtin.Game.Rank
{
    public sealed class tRank : Table<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList>, TableReadOnly<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList, Zeze.Builtin.Game.Rank.BRankListReadOnly>
    {
        public tRank() : base("Zeze_Builtin_Game_Rank_tRank")
        {
        }

        public override int Id => -2123575790;
        public override bool IsMemory => false;
        public override bool IsAutoKey => false;

        public const int VAR_RankList = 1;

        public override Zeze.Builtin.Game.Rank.BConcurrentKey DecodeKey(ByteBuffer _os_)
        {
            var _v_ = new Zeze.Builtin.Game.Rank.BConcurrentKey();
            _v_.Decode(_os_);
            return _v_;
        }

        public override ByteBuffer EncodeKey(Zeze.Builtin.Game.Rank.BConcurrentKey _v_)
        {
            ByteBuffer _os_ = ByteBuffer.Allocate();
            _v_.Encode(_os_);
            return _os_;
        }

        async Task<Zeze.Builtin.Game.Rank.BRankListReadOnly> TableReadOnly<Zeze.Builtin.Game.Rank.BConcurrentKey, Zeze.Builtin.Game.Rank.BRankList, Zeze.Builtin.Game.Rank.BRankListReadOnly>.GetAsync(Zeze.Builtin.Game.Rank.BConcurrentKey key)
        {
            return await GetAsync(key);
        }
    }
}
