
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Game.Rank
{
    /// <summary>
    /// 基本排行榜，实现了按long value从大到小进榜。
    /// 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
    /// 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
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
        /// <returns>Procudure.Success...</returns>
        protected int UpdateRank(int hash, int rankType, long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int concurrentLevel = GetConcurrentLevel(rankType);
            int maxCount = GetRankComputeCount(rankType);

            var concurrentKey = new BConcurrentKey(rankType, hash % concurrentLevel);
            var rank = _trank.GetOrAdd(concurrentKey);
            // remove if role exist. 看看有没有更快的算法。
            bool found = false;
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].RoleId == roleId)
                {
                    rank.RankList.RemoveAt(i);
                    found = true;
                    break;
                }
            }
            // insert if in rank. 这里可以用 BinarySearch。
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].Value < value)
                {
                    rank.RankList.Insert(i, new BRankValue() { RoleId = roleId, Value = value, ValueEx = valueEx });
                    if (rank.RankList.Count > maxCount)
                    {
                        rank.RankList.RemoveAt(rank.RankList.Count - 1);
                    }
                    return Procedure.Success;
                }
            }
            // try append. 原来进榜，又排到了队尾，此时可能在未进榜的数据中存在比当前大的，所以不要放进去。
            if (rank.RankList.Count < maxCount && false == found)
            {
                rank.RankList.Add(new BRankValue() { RoleId = roleId, Value = value, ValueEx = valueEx });
            }
            return Procedure.Success;
        }

        public class Rank
        {
            public long BuildTime { get; set; }
            public BRankList TableValue { get; set; }
        }

        ConcurrentDictionary<int, Rank> Ranks = new ConcurrentDictionary<int, Rank>();
        public const long RebuildTime = 5 * 60 * 1000; // 5 min

        private BRankList Merge(BRankList left, BRankList right)
        {
            BRankList result = new BRankList();
            int indexLeft = 0;
            int indexRight = 0;
            while (indexLeft < left.RankList.Count && indexRight < right.RankList.Count)
            {
                if (left.RankList[indexLeft].Value >= right.RankList[indexRight].Value)
                {
                    result.RankList.Add(left.RankList[indexLeft]);
                    ++indexLeft;
                }
                else
                {
                    result.RankList.Add(right.RankList[indexRight]);
                    ++indexRight;
                }
            }
            // 下面两种情况不会同时存在，同时存在"在上面"处理。
            if (indexLeft < left.RankList.Count)
            {
                while (indexLeft < left.RankList.Count)
                {
                    result.RankList.Add(left.RankList[indexLeft]);
                    ++indexLeft;
                }
            }
            else if (indexRight < right.RankList.Count)
            {
                while (indexRight < right.RankList.Count)
                {
                    result.RankList.Add(right.RankList[indexRight]);
                    ++indexRight;
                }
            }
            return result;
        }

        private Rank GetRank(int rankType)
        {
            var Rank = Ranks.GetOrAdd(rankType, (key) => new Rank());
            lock (Rank)
            {
                long now = Zeze.Util.Time.NowUnixMillis;
                if (now - Rank.BuildTime < RebuildTime)
                {
                    return Rank;
                }
                // rebuild
                List<BRankList> datas = new List<BRankList>();
                int cocurrentLevel = GetConcurrentLevel(rankType);
                for (int i = 0; i < cocurrentLevel; ++i)
                {
                    var concurrentKey = new BConcurrentKey(rankType, i);
                    var rank = _trank.GetOrAdd(concurrentKey);
                    datas.Add(rank);
                }
                int countNeed = GetRankCount(rankType);
                switch (datas.Count)
                {
                    case 0:
                        Rank.TableValue = new BRankList();
                        break;

                    case 1:
                        Rank.TableValue = datas[0].Copy();
                        break;

                    default:
                        // 合并过程中，结果是新的 BRankList，List中的 BRankValue 引用到表中。
                        // 最后 Copy 一次。
                        BRankList current = datas[0];
                        for (int i = 1; i < datas.Count; ++i)
                        {
                            current = Merge(current, datas[i]);
                            if (current.RankList.Count > countNeed)
                            {
                                // 合并中间结果超过需要的数量可以先删除。
                                // 第一个current直接引用table.data，不能删除。
                                current.RankList.RemoveRange(countNeed, current.RankList.Count - countNeed);
                            }
                        }
                        Rank.TableValue = current.Copy(); // current 可能还直接引用第一个，虽然逻辑上不大可能。先Copy。
                        break;
                }
                Rank.BuildTime = now;
                if (Rank.TableValue.RankList.Count > countNeed) // 再次删除多余的结果。
                {
                    Rank.TableValue.RankList.RemoveRange(countNeed, Rank.TableValue.RankList.Count - countNeed);
                }
            }
            return Rank;
        }

        /// <summary>
        /// 相关数据变化时，更新排行榜。
        /// 最好在事务成功结束或者处理快完的时候或者ChangeListener中调用这个方法更新排行榜。
        /// 比如使用 Transaction.Current.RunWhileCommit(() => RunUpdateRank(...));
        /// </summary>
        /// <param name="rankType"></param>
        /// <param name="roleId"></param>
        /// <param name="value"></param>
        /// <param name="valueEx">只保存，不参与比较。如果需要参与比较，需要另行实现自己的Update和Get。</param>
        [ModuleRedirect()]
        public virtual void RunUpdateRank(int rankType, long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int hash = Game.ModuleRedirect.GetChoiceHashCode();
            App.Zeze.Run(() => UpdateRank(hash, rankType, roleId, value, valueEx), nameof(UpdateRank), Zeze.TransactionModes.ExecuteInAnotherThread, hash);
        }

        /// <summary>
        /// 为排行榜设置最大并发级别。【有默认值】
        /// 【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】【这个参数非常重要】
        /// 决定了最大的并发度，改变的时候，旧数据全部失效，需要清除，重建。
        /// 一般选择一个足够大，但是又不能太大的数据。
        /// </summary>
        public int GetConcurrentLevel(int rankType)
        {
            return rankType switch
            {
                BConcurrentKey.RankTypeGold => 1024,
                _ => 1024, // default
            };
        }

        // 为排行榜设置需要的数量。【有默认值】
        public int GetRankCount(int rankType)
        {
            return rankType switch
            {
                BConcurrentKey.RankTypeGold => 100,
                _ => 100,
            };
        }

        // 排行榜中间数据的数量。【有默认值】
        public int GetRankComputeCount(int rankType)
        {
            return rankType switch
            {
                BConcurrentKey.RankTypeGold => 500,
                _ => GetRankCount(rankType) * 5,
            };
        }

        public override int ProcessCGetRankList(CGetRankList protocol)
        {
            Login.Session session = Login.Session.Get(protocol);

            var result = new SGetRankList();
            if (null == session.RoleId)
            {
                result.ResultCode = -1;
                session.SendResponse(result);
                return Procedure.LogicError;
            }
            result.Argument.RankList.AddRange(GetRank(protocol.Argument.RankType).TableValue.RankList);
            session.SendResponse(result);
            return Procedure.Success;
        }

        /******************************** ModuleRedirect 测试 *****************************************/
        [ModuleRedirect()]
        public virtual TaskCompletionSource<int> RunTest1(Zeze.TransactionModes mode)
        {
            int hash = Game.ModuleRedirect.GetChoiceHashCode();
            return App.Zeze.Run(() => Test1(hash), nameof(Test1), mode, hash);
        }

        protected int Test1(int hash)
        {
            return Procedure.Success;
        }

        [ModuleRedirect()]
        public virtual void RunTest2(int inData, ref int refData, out int outData)
        {
            int hash = Game.ModuleRedirect.GetChoiceHashCode();
            int outDataTmp = 0;
            int refDataTmp = refData;
            var future = App.Zeze.Run(() => Test2(hash, inData, ref refDataTmp, out outDataTmp), nameof(Test2), Zeze.TransactionModes.ExecuteInAnotherThread, hash);
            future.Task.Wait();
            refData = refDataTmp;
            outData = outDataTmp;
        }

        protected int Test2(int hash, int inData, ref int refData, out int outData)
        {
            outData = 1;
            ++refData;
            return Procedure.Success;
        }

        [ModuleRedirect()]
        public virtual void RunTest3(int inData, ref int refData, out int outData, System.Action<int> resultCallback)
        {
            int hash = Game.ModuleRedirect.GetChoiceHashCode();
            int outDataTmp = 0;
            int refDataTmp = refData;
            var future = App.Zeze.Run(() => Test3(hash, inData, ref refDataTmp, out outDataTmp, resultCallback), nameof(Test3), Zeze.TransactionModes.ExecuteInAnotherThread, hash);
            future.Task.Wait();
            refData = refDataTmp;
            outData = outDataTmp;
        }

        /*
         * 一般来说 ref|out 和 Action 回调方式不该一起用。存粹为了测试。
         * 如果混用，首先 Action 回调先发生，然后 ref|out 的变量才会被赋值。这个当然吧。
         * 
         * 可以包含多个 Action。纯粹为了...可以用 Empty 表示 null，嗯，总算找到理由了。
         * 如果包含多个，按照真正的实现的回调顺序回调，不是定义顺序。这个也当然吧。
         * 
         * ref|out 方式需要同步等待，【不建议使用这种方式】【不建议使用这种方式】【不建议使用这种方式】
         */
        protected int Test3(int hash, int inData, ref int refData, out int outData, System.Action<int> resultCallback)
        {
            outData = 1;
            ++refData;
            resultCallback(1);
            return Procedure.Success;
        }
    }
}
