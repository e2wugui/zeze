
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Globalization;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;
using static Zezex.Provider.ModuleProvider;

namespace Game.Rank
{
    /// <summary>
    /// 基本排行榜，实现了按long value从大到小进榜。
    /// 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
    /// 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
    public partial class ModuleRank : AbstractModule
    {

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

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
        protected long UpdateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            int maxCount = GetRankComputeCount(keyHint.RankType);

            var concurrentKey = new BConcurrentKey(
                keyHint.RankType, hash % concurrentLevel,
                keyHint.TimeType, keyHint.Year, keyHint.Offset);

            var rank = _trank.GetOrAdd(concurrentKey);
            // remove if role exist. 看看有没有更快的算法。
            BRankValue exist = null;
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                var rankValue = rank.RankList[i];
                if (rankValue.RoleId == roleId)
                {
                    exist = rankValue;
                    rank.RankList.RemoveAt(i);
                    break;
                }
            }
            // insert if in rank. 这里可以用 BinarySearch。
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].Value < value)
                {
                    rank.RankList.Insert(i, new BRankValue()
                    {
                        RoleId = roleId,
                        Value = value,
                        ValueEx = valueEx,
                        AwardTaken = null == exist ? false : exist.AwardTaken
                    });
                    if (rank.RankList.Count > maxCount)
                    {
                        rank.RankList.RemoveAt(rank.RankList.Count - 1);
                    }
                    return Procedure.Success;
                }
            }
            // A: 如果排行的Value可能减少，那么当它原来存在，但现在处于队尾时，不要再进榜。
            // 因为此时可能存在未进榜但比它大的Value。
            // B: 但是在进榜玩家比榜单数量少的时候，如果不进榜，队尾的玩家更新还在队尾就会消失。
            if (rank.RankList.Count < GetRankCount(keyHint.RankType) // B:
                || (rank.RankList.Count < maxCount && null == exist) // A:
                )
            {
                rank.RankList.Add(new BRankValue()
                {
                    RoleId = roleId, 
                    Value = value, 
                    ValueEx = valueEx 
                });
            }
            return Procedure.Success;
        }

        public class Rank
        {
            public long BuildTime { get; set; }
            public BRankList TableValue { get; set; }
        }

        ConcurrentDictionary<BConcurrentKey, Rank> Ranks = new ConcurrentDictionary<BConcurrentKey, Rank>();
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

        private Rank GetRank(BConcurrentKey keyHint)
        {
            var Rank = Ranks.GetOrAdd(keyHint, (key) => new Rank());
            lock (Rank)
            {
                long now = Zeze.Util.Time.NowUnixMillis;
                if (now - Rank.BuildTime < RebuildTime)
                {
                    return Rank;
                }
                // rebuild
                List<BRankList> datas = new List<BRankList>();
                int cocurrentLevel = GetConcurrentLevel(keyHint.RankType);
                for (int i = 0; i < cocurrentLevel; ++i)
                {
                    var concurrentKey = new BConcurrentKey(
                        keyHint.RankType, i,
                        keyHint.TimeType, keyHint.Year, keyHint.Offset);
                    var rank = _trank.GetOrAdd(concurrentKey);
                    datas.Add(rank);
                }
                int countNeed = GetRankCount(keyHint.RankType);
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
        [Zezex.ModuleRedirect()]
        public virtual void RunUpdateRank(
            BConcurrentKey keyHint,
            long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int hash = Zezex.ModuleRedirect.GetChoiceHashCode();
            App.Zeze.Run(() => UpdateRank(hash, keyHint, roleId, value, valueEx),
                nameof(RunUpdateRank), Zeze.TransactionModes.ExecuteInAnotherThread, hash);
        }

        // 名字必须和RunUpdateRankWithHash匹配，内部使用一样的实现。
        protected long UpdateRankWithHash(
            int hash, BConcurrentKey keyHint,
            long roleId, long value, Zeze.Net.Binary valueEx)
        {
            return UpdateRank(hash, keyHint, roleId, value, valueEx);
        }

        [Zezex.ModuleRedirectWithHash()]
        public virtual void RunUpdateRankWithHash(
            int hash, BConcurrentKey keyHint,
            long roleId, long value, Zeze.Net.Binary valueEx)
        {
            App.Zeze.Run(() => UpdateRankWithHash(
                hash, keyHint, roleId, value, valueEx),
                nameof(RunUpdateRankWithHash),
                Zeze.TransactionModes.ExecuteInAnotherThread, hash);
        }

        /// <summary>
        /// ModuleRedirectAll 实现要求：
        /// 1）第一个参数是调用会话id；
        /// 2）第二个参数是hash-index；
        /// 3）然后是实现自定义输入参数；
        /// 4）最后是结果回调,
        ///    a) 第一参数是会话id，
        ///    b) 第二参数hash-index，
        ///    c) 第三个参数是returnCode，
        ///    d) 剩下的是自定义参数。
        /// </summary>
        protected int GetRank(long sessionId, int hash, BConcurrentKey keyHint,
            System.Action<long, int, long, BRankList> onHashResult)
        {
            // 根据hash获取分组rank。
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            var concurrentKey = new BConcurrentKey(
                keyHint.RankType, hash % concurrentLevel,
                keyHint.TimeType, keyHint.Year, keyHint.Offset);
            onHashResult(sessionId, hash, Procedure.Success, _trank.GetOrAdd(concurrentKey));
            return Procedure.Success;
        }

        // 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
        // 需要注意在子类上下文中可以编译通过。可以是常量。
        [Zezex.ModuleRedirectAll("GetConcurrentLevel(keyHint.RankType)")]
        public virtual void RunGetRank(BConcurrentKey keyHint,
            System.Action<long, int, long, BRankList> onHashResult,
            Action<ModuleRedirectAllContext> onHashEnd
            )
        {
            // 默认实现是本地遍历调用，这里不使用App.Zeze.Run启动任务（这样无法等待），直接调用实现。
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            var ctx = new ModuleRedirectAllContext(concurrentLevel, $"{FullName}:{nameof(RunGetRank)}")
            {
                OnHashEnd = onHashEnd,
            };
            long sessionId = App.Server.AddManualContextWithTimeout(ctx); // 处理hash分组结果需要一个上下文保存收集的结果。
            for (int i = 0; i < concurrentLevel; ++i)
            {
                GetRank(sessionId, i, keyHint, onHashResult);
            }
        }

        // 使用异步方案构建rank。
        private void GetRankAsync(BConcurrentKey keyHint, System.Action<Rank> callback)
        {
            if (Ranks.TryGetValue(keyHint, out var rank))
            {
                long now = Zeze.Util.Time.NowUnixMillis;
                if (now - rank.BuildTime < RebuildTime)
                {
                    callback(rank);
                    return;
                }
            }
            // 异步方式没法锁住Rank，所以并发的情况下，可能多次去获取数据，多次构建，多次覆盖Ranks的cache。
            int countNeed = GetRankCount(keyHint.RankType);
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            RunGetRank(keyHint,
                // Action OnHashResult
                (sessionId, hash, returnCode, BRankList) =>
                {
                    App.Server.TryGetManualContext<ModuleRedirectAllContext>(sessionId)
                        ?.ProcessHash(hash, () => new Rank(), (rank) =>
                        {
                            if (returnCode != Procedure.Success) // 只有处理成功的结果才是有效的。
                                return returnCode;
                            if (rank.TableValue == null)
                                rank.TableValue = BRankList.CopyIfManaged(); // 本地实现的时候可能返回受管理的数据Bean，此时需要拷贝。
                            else
                                rank.TableValue = Merge(rank.TableValue, BRankList);
                            if (rank.TableValue.RankList.Count > countNeed) // 合并中间结果超过需要的数量可以先删除。
                            rank.TableValue.RankList.RemoveRange(countNeed, rank.TableValue.RankList.Count - countNeed);
                            return Procedure.Success;
                        });
                },
                // Action OnHashEnd
                (context) =>
                {
                    if (context.HashCodes.Count > 0)
                    {
                        // 一般是超时发生时还有未返回结果的hash分组。
                        logger.Warn($"OnHashEnd: timeout with hashs: {context.HashCodes}");
                    }

                    var rank = context.UserState as Rank;
                    rank.BuildTime = Zeze.Util.Time.NowUnixMillis;
                    Ranks[keyHint] = rank; // 覆盖最新的数据到缓存里面。
                    callback(rank);
                }
            );
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
                BConcurrentKey.RankTypeGold => 128,
                _ => 128, // default
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

        public long GetCounter(long roleId, BConcurrentKey keyHint)
        {
            var counters = _trankcounters.GetOrAdd(roleId);
            if (false == counters.Counters.TryGetValue(keyHint, out var counter))
                return 0;

            return counter.Value;
        }

        public void AddCounterAndUpdateRank(long roleId, int delta,
            BConcurrentKey keyHint, Zeze.Net.Binary valueEx = null)
        {
            var counters = _trankcounters.GetOrAdd(roleId);
            if (false == counters.Counters.TryGetValue(keyHint, out var counter))
            {
                counter = new BRankCounter();
                counters.Counters.Add(keyHint, counter);
            }
            counter.Value += delta;

            if (null == valueEx)
                valueEx = Zeze.Net.Binary.Empty;

            RunUpdateRank(keyHint, roleId, counter.Value, valueEx);
        }

        public override long ProcessCGetRankList(Protocol p)
        {
            var protocol = p as CGetRankList;
            Login.Session session = Login.Session.Get(protocol);

            var result = new SGetRankList();
            if (null == session.RoleId)
            {
                result.ResultCode = -1;
                session.SendResponse(result);
                return Procedure.LogicError;
            }
            /* 
            //异步方式获取rank
            GetRankAsync(protocol.Argument.RankType, (rank) =>
            {
                result.Argument.RankList.AddRange(rank.TableValue.RankList);
                session.SendResponse(result);
            });
            /*/
            // 同步方式获取rank
            result.Argument.RankList.AddRange(GetRank(
                NewRankKey(protocol.Argument.RankType, protocol.Argument.TimeType)
                ).TableValue.RankList);
            session.SendResponse(result);
            // */
            return Procedure.Success;
        }

        public BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId = 0)
        {
            return NewRankKey(DateTime.Now, rankType, timeType, customizeId);
        }

        public BConcurrentKey NewRankKey(DateTime time, int rankType, int timeType, long customizeId = 0)
        {
            var year = time.Year; // 后面根据TimeType可能覆盖这个值。
            long offset;

            switch (timeType)
            {
                case BConcurrentKey.TimeTypeTotal:
                    year = 0;
                    offset = 0;
                    break;

                case BConcurrentKey.TimeTypeDay:
                    offset = time.DayOfYear;
                    break;

                case BConcurrentKey.TimeTypeWeek:
                    offset = Zeze.Util.Time.GetWeekOfYear(time);
                    break;

                case BConcurrentKey.TimeTypeSeason:
                    offset = Zeze.Util.Time.GetSimpleChineseSeason(time);
                    break;

                case BConcurrentKey.TimeTypeYear:
                    offset = 0;
                    break;

                case BConcurrentKey.TimeTypeCustomize:
                    year = 0;
                    offset = customizeId;
                    break;

                default:
                    throw new Exception($"Unsupport TimeType={timeType}");
            }
            return new BConcurrentKey(rankType, 0, timeType, year, offset);
        }

        /******************************** ModuleRedirect 测试 *****************************************/
        [Zezex.ModuleRedirect()]
        public virtual TaskCompletionSource<long> RunTest1(Zeze.TransactionModes mode)
        {
            int hash = Zezex.ModuleRedirect.GetChoiceHashCode();
            return App.Zeze.Run(() => Test1(hash), nameof(Test1), mode, hash);
        }

        protected int Test1(int hash)
        {
            return Procedure.Success;
        }

        [Zezex.ModuleRedirect()]
        public virtual void RunTest2(int inData, ref int refData, out int outData)
        {
            int hash = Zezex.ModuleRedirect.GetChoiceHashCode();
            int outDataTmp = 0;
            int refDataTmp = refData;
            var future = App.Zeze.Run(() => Test2(hash, inData, ref refDataTmp, out outDataTmp), nameof(Test2), Zeze.TransactionModes.ExecuteInAnotherThread, hash);
            future.Task.Wait();
            refData = refDataTmp;
            outData = outDataTmp;
        }

        protected long Test2(int hash, int inData, ref int refData, out int outData)
        {
            outData = 1;
            ++refData;
            return Procedure.Success;
        }

        [Zezex.ModuleRedirect()]
        public virtual void RunTest3(int inData, ref int refData, out int outData, System.Action<int> resultCallback)
        {
            int hash = Zezex.ModuleRedirect.GetChoiceHashCode();
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
