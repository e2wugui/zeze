
using Org.BouncyCastle.Asn1.Cms;
using System;
using System.Collections.Concurrent;
using System.Globalization;
using System.Threading.Tasks;
using Zeze.Builtin.DelayRemove;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Component
{
	public class DelayRemove : AbstractDelayRemove
	{
		/**
		 * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
		 */
		private static ConcurrentDictionary<int, DelayRemove> delays = new();

		public static async Task RemoveAsync(Table table, object key)
		{
			var serverId = table.Zeze.Config.ServerId;
			var delay = delays.GetOrAdd(serverId, (_key_) => new DelayRemove(table.Zeze));
			var value = new BTableKey()
			{
				TableName = table.Name,
				EncodedKey = new Binary(table.EncodeKey(key)),
				EnqueueTime = Util.Time.NowUnixMillis,
			};
			await delay.queue.AddAsync(value);
		}

		private Zeze.Collections.Queue<BTableKey> queue;
        public Zeze.Application Zeze { get; }
        public const string eTimerNamePrefix = "Zeze.Component.DelayRemove.";

		private DelayRemove(Zeze.Application zz)
		{
			this.Zeze = zz;
			var serverId = zz.Config.ServerId;
			queue = zz.Queues.Open<BTableKey>("__GCTableQueue#" + serverId);

            // TODO start timer to gc. work on queue.pollNode? peekNode? poll? peek?
            var name = eTimerNamePrefix + serverId;

            //zz.Timer.AddHandle(name, (context) => OnTimer(serverId));

            // 根据配置的Timer的时间范围，按分钟精度随机出每天的开始时间，最后计算延迟，然后按24小时间隔执行。
            var now = new DateTime();
            var at = new DateTime(now.Year, now.Month, now.Day, Zeze.Config.DelayRemoveHourStart, 0, 0);
            var minutes = 60 * (Zeze.Config.DelayRemoveHourEnd - Zeze.Config.DelayRemoveHourStart);
            if (minutes <= 0)
                minutes = 60;
            minutes = Util.Random.Instance.Next(minutes);
            at.AddMinutes(minutes);
            if (at.CompareTo(now) < 0)
                at = at.AddDays(1);
            var delay = Util.Time.DateTimeToUnixMillis(at) - Util.Time.DateTimeToUnixMillis(now);
            var period = 24 * 3600 * 1000; // 24 hours
            //zz.Timer.ScheduleNamed(name, delay, period, name, null);
        }

        private void OnTimer(int serverId)
        {
            // delayRemove可能需要删除很多记录，不嵌入Timer事务，启动新的线程执行新的事务。
            // 这里仅利用Timer的触发。
            // 每个节点的记录删除一个事务执行。
            _ = Mission.CallAsync(Zeze.NewProcedure(() => RunDelayRemove(serverId), "delayRemoveProcedure"));
        }

        private async Task<long> RunDelayRemove(int serverId)
        {
            // 已经在事务中了。
            var days = Zeze.Config.DelayRemoveDays;
            if (days < 7)
                days = 7;
            var diffMills = days * 24 * 3600 * 1000;

            var maxTime = 0L; // 放到外面可以处理下面的node.getValues().isEmpty()的情况。
            var node = await queue.PollNodeAsync();
            foreach (var value in node.Values)
            {
                var tableKey = (BTableKey)value.Value.Bean;
                // queue是按时间顺序的，记住最后一条即可，这样写能适应不按顺序的。
                maxTime = Math.Max(maxTime, tableKey.EnqueueTime);
                var table = Zeze.GetTableSlow(tableKey.TableName);
                if (null != table)
                    await table.RemoveAsync(tableKey.EncodedKey);
            }
            if (diffMills < Util.Time.NowUnixMillis - maxTime)
                OnTimer(serverId); // 都是最老的，再次尝试删除。
            return 0;
        }
    }
}
