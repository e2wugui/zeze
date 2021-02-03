
using System;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Game.Rank
{
    public partial class ModuleRank : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        /// <summary>
        /// 根据 value 设置到排行榜中
        /// </summary>
        /// <param name="hash"></param>
        /// <param name="rankType"></param>
        /// <param name="roleId"></param>
        /// <param name="value"></param>
        /// <returns>排名，从1开始。-1表示没有进榜</returns>
        private int SetRank(int hash, int rankType, long roleId, long value, int concurrentLevel, int maxCount)
        {
            var concurrentKey = new BConcurrentKey(rankType, hash % concurrentLevel);
            var rank = _trank.GetOrAdd(concurrentKey);
            // remove if role exist. 看看有没有更快的算法。
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].RoleId == roleId)
                {
                    rank.RankList.RemoveAt(i);
                    break;
                }
            }
            // insert if in rank. 这里可以用 BinarySearch。
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].Value < value)
                {
                    rank.RankList.Insert(i, new BRankValue() { RoleId = roleId, Value = value });
                    if (rank.RankList.Count > maxCount)
                    {
                        rank.RankList.RemoveAt(rank.RankList.Count - 1);
                    }
                    return i + 1;
                }
            }
            // try append
            if (rank.RankList.Count < maxCount)
            {
                rank.RankList.Add(new BRankValue() { RoleId = roleId, Value = value });
                return rank.RankList.Count;
            }
            return -1;
        }

        private int GetRank(int hash, int rankType, long roleId, int concurrentLevel)
        {
            var concurrentKey = new BConcurrentKey(rankType, hash % concurrentLevel);
            var rank = _trank.GetOrAdd(concurrentKey);
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].RoleId == roleId)
                {
                    return i + 1;
                }
            }
            return -1;
        }
        //////////////////////////////////////////////////////////////////////
        /// 金钱排行榜实现接口 RankTypeGold
        /// <summary>
        /// 【这个参数非常重要】
        /// 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
        /// 一般选择一个足够大，但是又不能太大的数据。
        /// </summary>
        public const int RankTypeGoldConcurrentLevel = 1024;
        public const int RankTypeGoldCount = 100; // 需要的排名数量
        public const int RankTypeGoldCountCompute = 500; // 排名计算数量，比RankTypeGoldCount大。
        
        protected int _SetRankWhenGoldChange(int hash, long roleId, long goldNumber)
        {
            SetRank(hash, BConcurrentKey.RankTypeGold, roleId, goldNumber, RankTypeGoldConcurrentLevel, RankTypeGoldCountCompute);
            return Procedure.Success;
        }

        [ModuleRedirect()]
        public virtual TaskCompletionSource<int> SetRankWhenGoldChange(long roleId, long goldNumber, Zeze.TransactionModes mode = Zeze.TransactionModes.ExecuteInAnotherThread)
        {
            int hash = (Transaction.Current.UserState as Login.Session).Account.GetHashCode();
            return App.Zeze.Run(() => _SetRankWhenGoldChange(hash, roleId, goldNumber), nameof(SetRankWhenGoldChange), mode, roleId);
        }

        protected int _GeRankGold(int hash, long roleId, Action<int> outCallback)
        {
            int rankOrder = GetRank(hash, BConcurrentKey.RankTypeGold, roleId, RankTypeGoldConcurrentLevel);
            outCallback(rankOrder);
            return Procedure.Success;
        }

        [ModuleRedirect()]
        public virtual TaskCompletionSource<int> GeRankGold(long roleId, Action<int> outCallback, Zeze.TransactionModes mode = Zeze.TransactionModes.ExecuteInAnotherThread)
        {
            int hash = (Transaction.Current.UserState as Login.Session).Account.GetHashCode();
            return App.Zeze.Run(() => _GeRankGold(hash, roleId, outCallback), nameof(GeRankGold), mode, roleId);
        }
    }
}
