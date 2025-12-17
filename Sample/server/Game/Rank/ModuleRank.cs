using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Arch;
using Zeze.Util;
using DotNext.Threading;
using System.Threading;

namespace Game.Rank
{
    /// <summary>
    /// 基本排行榜，实现了按long value从大到小进榜。
    /// 增加排行被类型。在 solution.xml::beankey::BConcurrentKey中增加类型定义。
    /// 然后在数据变化时调用 RunUpdateRank 方法更行排行榜。
    public partial class ModuleRank : AbstractModule
    {
        // private static readonly ILogger logger = LogManager.GetLogger(typeof(ModuleRank));

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
        [RedirectHash("GetConcurrentLevel(keyHint.RankType)")]
        protected virtual async Task<long> UpdateRank(int hash, BConcurrentKey keyHint, long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            int maxCount = GetRankComputeCount(keyHint.RankType);

            var concurrentKey = new BConcurrentKey(
                keyHint.RankType, hash % concurrentLevel,
                keyHint.TimeType, keyHint.Year, keyHint.Offset);

            var rank = await _tRank.GetOrAddAsync(concurrentKey);
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
                        AwardTaken = null != exist && exist.AwardTaken
                    });
                    if (rank.RankList.Count > maxCount)
                    {
                        rank.RankList.RemoveAt(rank.RankList.Count - 1);
                    }
                    return ResultCode.Success;
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
            return ResultCode.Success;
        }

        public class Rank
        {
            public long BuildTime { get; set; }
            public BRankList TableValue { get; set; }
            public AsyncLock Mutex { get; } = new();
        }

        readonly ConcurrentDictionary<BConcurrentKey, Rank> Ranks = new();
        public const long RebuildTime = 5 * 60 * 1000; // 5 min

        private BRankList Merge(BRankList left, BRankList right)
        {
            var result = new BRankList();
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

        public async Task<Rank> GetRankDirect(BConcurrentKey keyHint)
        {
            var Rank = Ranks.GetOrAdd(keyHint, (key) => new Rank());
            using (await Rank.Mutex.AcquireAsync(CancellationToken.None))
            {
                long now = Zeze.Util.Time.NowUnixMillis;
                if (now - Rank.BuildTime < RebuildTime)
                {
                    return Rank;
                }
                // rebuild
                var datas = new List<BRankList>();
                int cocurrentLevel = GetConcurrentLevel(keyHint.RankType);
                for (int i = 0; i < cocurrentLevel; ++i)
                {
                    var concurrentKey = new BConcurrentKey(
                        keyHint.RankType, i,
                        keyHint.TimeType, keyHint.Year, keyHint.Offset);
                    var rank = await _tRank.GetOrAddAsync(concurrentKey);
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

        protected async Task<BRankList> GetRankAll(int hash, BConcurrentKey key)
        {
            var concurrentKey = new BConcurrentKey(key.RankType, hash, key.TimeType, key.Year, key.Offset);
            return await _tRank.GetOrAddAsync(concurrentKey);
        }

        // 属性参数是获取总的并发分组数量的代码，直接复制到生成代码中。
        // 需要注意在子类上下文中可以编译通过。可以是常量。
        [RedirectAll("GetConcurrentLevel(keyHint.RankType)")]
        public virtual Task<RedirectAll<BRankList>> GetRankAll(BConcurrentKey keyHint)
        {
            return null;
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

        protected override async Task<long> ProcessGetRankListRequest(Protocol p)
        {
            var r = p as GetRankList;
            var session = ProviderUserSession.Get(r);

            if (null == session.RoleId)
            {
                r.ResultCode = -1;
                session.SendResponseWhileCommit(r);
                return ResultCode.LogicError;
            }
            // 下面使用RedirectAll的方式查询并合并结果。
            // 目前没有使用本地get-cache，这种机制参考GetRankDirect的实现。
            var current = new BRankList();
            int countNeed = GetRankCount(r.Argument.RankType);
            if (countNeed > 0)
            {
                var result = await GetRankAll(NewRankKey(r.Argument.RankType, r.Argument.TimeType));
                foreach (var hr in result.HashResults)
                {
                    current = Merge(current, hr.Value);
                    if (current.RankList.Count > countNeed)
                    {
                        // 合并中间结果超过需要的数量可以先删除。
                        current.RankList.RemoveRange(countNeed, current.RankList.Count - countNeed);
                    }
                }
            }
            r.Result.RankList.AddRange(current.RankList);
            session.SendResponseWhileCommit(r);
            // */
            return ResultCode.Success;
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
        [RedirectToServer()]
        public virtual Task TestToServer(int serverId, int param, Action<int, int> result)
        {
            result(param, App.Zeze.Config.ServerId);
            return Task.CompletedTask;
        }

        [RedirectHash()]
        public virtual Task TestHash(int hash, int param, Action<int, int> result)
        {
            result(param, App.Zeze.Config.ServerId);
            return Task.CompletedTask;
        }

        [RedirectToServer()]
        public virtual Task<long> TestToServerResult(int serverId, int param, Action<int, int> result)
        {
            result(param, App.Zeze.Config.ServerId);
            return Task.FromResult(12345L);
        }

        [RedirectHash()]
        public virtual Task<long> TestHashResult(int hash, int param, Action<int, int> result)
        {
            result(param, App.Zeze.Config.ServerId);
            return Task.FromResult(12345L);
        }

        [RedirectToServer()]
        public virtual void TestToServerNoWait(int serverId, Action<int, int> result, int param)
        {
            result(param, App.Zeze.Config.ServerId);
        }

        [RedirectHash()]
        public virtual void TestHashNoWait(int hash, int param)
        {
        }


        // broardcast awaitable?, collect awaitable!
        protected Task TestAllNoResult(int hash, int param)
        {
            return Task.CompletedTask;
        }

        [RedirectAll("100")]
        public virtual Task<RedirectAll> TestAllNoResult(int param)
        {
            return null;
        }

        protected Task<long> TestAllResult(int hash, int param)
        {
            return Task.FromResult((long)App.Zeze.Config.ServerId << 48 | (long)hash << 32 | (uint)param);
        }

        [RedirectAll("100")]
        public virtual Task<RedirectAll<long>> TestAllResult(int param)
        {
            return null;
        }

        public class MyResult : Zeze.Arch.Gen.Result
        {
            public int Value;
        }

        protected Task<MyResult> TestAllResultProcessing(int hash, int param)
        {
            return Task.FromResult(new MyResult());
        }

        [RedirectAll("100")]
        public virtual Task<RedirectAll<MyResult>> TestAllResultProcessing(int param, Func<RedirectAll<MyResult>, Task> processing)
        {
            return null;
        }
    }
}
