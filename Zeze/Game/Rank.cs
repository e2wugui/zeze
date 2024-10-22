
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Arch.Gen;
using Zeze.Builtin.Game.Rank;

namespace Zeze.Game
{
    public class Rank : AbstractRank
    {
        public AppBase App { get; }

        public volatile Func<int, int> funcRankSize; // intRankSize (int rankType)
        public volatile Func<int, int> funcConcurrentLevel; // intConcurrentLevel (int rankType)
        public volatile float ComputeFactor = 2.5f; // RankStorageSize = funcRankSize(rankType) * ComputeFactor

        public static Rank Create(AppBase app)
        {
            return GenModule.CreateRedirectModule(app, new Rank());
	    }

        // �����ֶ����������� Redirect ����ʱ����������͡�
        internal Rank()
        {
            App = null;
        }

        public Rank(AppBase app)
        {
            this.App = app ?? throw new ArgumentException("app is null");
            RegisterZezeTables(App.Zeze);
            RegisterProtocols(App.Zeze.Redirect.ProviderApp.ProviderService);
        }

        public async Task StartAsync(string serviceNamePrefix, string providerDirectIp, int providerDirectPort)
        {
            var name = ProviderDistribute.MakeServiceName(serviceNamePrefix, Id);
            var identity = App.Zeze.Config.ServerId.ToString();
            await App.Zeze.ServiceManager.RegisterService(name, identity, 0, providerDirectIp, providerDirectPort);
        }

        public override void UnRegister()
        {
            if (App != null)
            {
                UnRegisterProtocols(App.Zeze.Redirect.ProviderApp.ProviderService);
                UnRegisterZezeTables(App.Zeze);
            }
        }
        public static BConcurrentKey NewRankKey(int rankType, int timeType, long customizeId = 0)
        {
            return NewRankKey(DateTime.Now, rankType, timeType, customizeId);
        }

        public static BConcurrentKey NewRankKey(DateTime time, int rankType, int timeType, long customizeId = 0)
        {
            var year = time.Year; // �������TimeType���ܸ������ֵ��
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

        /**
         * Ϊ���а�������Ҫ������������Ĭ��ֵ��
         */
        public int GetRankSize(int rankType)
        {
            var volatileTmp = funcRankSize;
            if (null != volatileTmp)
                return volatileTmp(rankType);
            return 100;
        }

        /**
         * Ϊ���а�������󲢷����𡣡���Ĭ��ֵ��
         * ����������ǳ���Ҫ������������ǳ���Ҫ������������ǳ���Ҫ������������ǳ���Ҫ��
         * ���������Ĳ����ȣ��ı��ʱ�򣬾�����ȫ��ʧЧ����Ҫ������ؽ���
         * һ��ѡ��һ���㹻�󣬵����ֲ���̫������ݡ�
         */
        public int GetConcurrentLevel(int rankType)
        {
            var volatileTmp = funcConcurrentLevel;
            if (null != volatileTmp)
                return volatileTmp(rankType);
            return 128; // default
        }

        /**
         * ���а��м����ݵ�����������Ĭ��ֵ��
         */
        public int GetComputeSize(int rankType)
        {
            var factor = ComputeFactor;
            if (factor < 2)
                factor = 2;
            return (int)(GetConcurrentLevel(rankType) * factor);
        }

        /// <summary>
        /// ���� value ���õ����а���
        /// </summary>
        /// <param name="hash"></param>
        /// <param name="rankType"></param>
        /// <param name="roleId"></param>
        /// <param name="value"></param>
        /// <returns>Procudure.Success...</returns>
        [RedirectHash("GetConcurrentLevel(keyHint.RankType)")]
        protected virtual async Task<long> UpdateRank(int hash, BConcurrentKey keyHint,
            long roleId, long value, Zeze.Net.Binary valueEx)
        {
            int concurrentLevel = GetConcurrentLevel(keyHint.RankType);
            int maxCount = GetComputeSize(keyHint.RankType);

            var concurrentKey = new BConcurrentKey(
                keyHint.RankType, hash % concurrentLevel,
                keyHint.TimeType, keyHint.Year, keyHint.Offset);

            var rank = await _tRank.GetOrAddAsync(concurrentKey);
            // remove if role exist. ������û�и�����㷨��
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
            // insert if in rank. ��������� BinarySearch��
            for (int i = 0; i < rank.RankList.Count; ++i)
            {
                if (rank.RankList[i].Value < value)
                {
                    rank.RankList.Insert(i, new BRankValue()
                    {
                        RoleId = roleId,
                        Value = value,
                        ValueEx = valueEx,
                    });
                    if (rank.RankList.Count > maxCount)
                    {
                        rank.RankList.RemoveAt(rank.RankList.Count - 1);
                    }
                    return 0;
                }
            }
            // A: ������е�Value���ܼ��٣���ô����ԭ�����ڣ������ڴ��ڶ�βʱ����Ҫ�ٽ���
            // ��Ϊ��ʱ���ܴ���δ���񵫱������Value��
            // B: �����ڽ�����ұȰ������ٵ�ʱ����������񣬶�β����Ҹ��»��ڶ�β�ͻ���ʧ��
            if (rank.RankList.Count < GetRankSize(keyHint.RankType) // B:
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
            return 0;
        }

        protected async Task<BRankList> GetRankAll(int hash, BConcurrentKey key)
        {
            var concurrentKey = new BConcurrentKey(key.RankType, hash, key.TimeType, key.Year, key.Offset);
            return await _tRank.GetOrAddAsync(concurrentKey);
        }

        [RedirectAll("GetConcurrentLevel(key.RankType)")]
        public virtual Task<RedirectAll<BRankList>> GetRankAll(BConcurrentKey key)
        {
            return null;
        }

        private static BRankList Merge(BRankList left, BRankList right)
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
            // ���������������ͬʱ���ڣ�ͬʱ����"������"����
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

        public async Task<BRankList> GetRankDirect(BConcurrentKey keyHint)
        {
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
            return Merge(GetRankSize(keyHint.RankType), datas);
        }

        private static BRankList Merge(int countNeed, List<BRankList> datas)
        {
            if (datas.Count == 0)
                return new BRankList();

            if (datas.Count == 1)
                return datas[0].Copy();

            // �ϲ������У�������µ� BRankList��List�е� BRankValue ���õ����С�
            // ��� Copy һ�Ρ�
            var current = datas[0];
            for (int i = 1; i < datas.Count; ++i)
            {
                current = Merge(current, datas[i]);
                if (current.RankList.Count > countNeed)
                {
                    // �ϲ��м���������Ҫ������������ɾ����
                    // ��һ��currentֱ������table.data������ɾ����
                    current.RankList.RemoveRange(countNeed, current.RankList.Count - countNeed);
                }
            }
            // current ��ѭ���󣬲���ָ��datas����Ķ�������ɾ������Ӱ�����ݿ��ڴ洢�Ķ���
            if (current.RankList.Count > countNeed)
            {
                // ���ɾ������Ľ����
                current.RankList.RemoveRange(countNeed, current.RankList.Count - countNeed);
            }
            return current;
        }

    }
}
