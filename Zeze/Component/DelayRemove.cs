using System;
using System.Threading.Tasks;
using Zeze.Builtin.DelayRemove;
using Zeze.Net;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Component
{
    /**
     * 每个ServerId分配一个独立的GC队列。Server之间不会争抢。如果一个Server一直没有起来，那么它的GC就一直不会执行。
     */
    public class DelayRemove : AbstractDelayRemove
	{
		public async Task RemoveAsync(Table table, object key)
		{
			var value = new BTableKey()
			{
				TableName = table.Name,
				EncodedKey = new Binary(table.EncodeKey(key)),
				EnqueueTime = Util.Time.NowUnixMillis,
			};
			await queue.AddAsync(value);
		}

		private readonly Zeze.Collections.Queue<BTableKey> queue;
        public Zeze.Application Zeze { get; }
        private Util.SchedulerTask Timer;

		public DelayRemove(Zeze.Application zz)
		{
			this.Zeze = zz;
			var serverId = zz.Config.ServerId;
			queue = zz.Queues.Open<BTableKey>("__GCTableQueue#" + serverId);
        }

        public void Start()
        {
            if (null != Timer)
                return;

            // start timer to gc. work on queue.pollNode? peekNode? poll? peek?
            // 根据配置的Timer的时间范围，按分钟精度随机出每天的开始时间，最后计算延迟，然后按24小时间隔执行。
            var now = DateTime.Now;
            var at = new DateTime(now.Year, now.Month, now.Day, Zeze.Config.DelayRemoveHourStart, 0, 0);
            var minutes = 60 * (Zeze.Config.DelayRemoveHourEnd - Zeze.Config.DelayRemoveHourStart);
            if (minutes <= 0)
                minutes = 60;
            minutes = Util.Random.Instance.Next(minutes);
            at = at.AddMinutes(minutes);
            if (at.CompareTo(now) < 0)
                at = at.AddDays(1);
            var delay = Util.Time.DateTimeToUnixMillis(at) - Util.Time.DateTimeToUnixMillis(now);
            var period = 24 * 3600 * 1000; // 24 hours
            Timer = Util.Scheduler.Schedule(OnTimer, delay, period);
        }

        public void Stop()
        {
            Timer?.Cancel();
            Timer = null;
        }

        private void OnTimer(SchedulerTask ThisTask)
        {
            // delayRemove可能需要删除很多记录，不能在一个事务内完成全部删除。
            // 这里按每个节点的记录的删除在一个事务中执行，节点间用不同的事务。
            var days = Zeze.Config.DelayRemoveDays;
            if (days < 7)
                days = 7;
            var diffMills = days * 24 * 3600 * 1000;

            var removing = true;
            while (removing)
            {
                Zeze.NewProcedure(async () =>
                {
                    var node = await queue.PollNodeAsync();
                    if (node == null)
                    {
                        removing = false;
                        return 0;
                    }

                    // 检查节点的第一个（最老的）项是否需要删除。
                    // 如果不需要，那么整个节点都不会删除，并且中断循环。
                    // 如果需要，那么整个节点都删除，即使中间有一些没有达到过期。
                    // 这是个不精确的删除过期的方法。
                    if (node.Values.Count > 0)
                    {
                        var first = (BTableKey)node.Values[0].Value.Bean;
                        if (diffMills < Util.Time.NowUnixMillis - first.EnqueueTime)
                        {
                            removing = false;
                            return 0;
                        }
                    }

                    // node.getValues().isEmpty，这一项将保持0，循环后设置removing.value将基本是true。
                    // 即，空节点总是尝试继续删除。
                    var maxTime = 0L;
                    foreach (var value in node.Values)
                    {
                        var tableKey = (BTableKey)value.Value.Bean;
                        // queue是按时间顺序的，记住最后一条即可。
                        maxTime = tableKey.EnqueueTime;
                        var table = Zeze.GetTableSlow(tableKey.TableName);
                        if (null != table)
                            await table.RemoveAsync(tableKey.EncodedKey);
                    }
                    removing = diffMills < Util.Time.NowUnixMillis - maxTime;
                    return 0;
                }, "delayRemoveProcedure").CallSynchronously();
            }
        }
    }
}
