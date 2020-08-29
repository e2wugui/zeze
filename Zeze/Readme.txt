TODO 
协议里面的数值默认大于等于0，支持负数需要明确声明。
数据变更?

GameServer.TableCache
    ConcurrentDictionary<K, Record<K, V>> cache;
    记录在Cache中的三个状态。
    M:Modify
    S:Share
    I:Invalid(Not In Cache)

GameServer.ReduceToS()
{
    Record r = cache.Get(key);
    if (null == r) // I
        return Success With Do Nothing. 哪里肯定出错了。

    lockey.EnterWriteLock(); // 锁住本地记录锁。和事务并发，好像需要，还没细想。
    try
    {
        lock (r)
        {
            switch (r.state)
            {
	        case S:
		    return Success With S; // 已经是S状态，肯定哪里出错了。
		case M:
		    r.State = S; // 马上修改状态。事务如果是读，没有问题，如果是写将发送新的AcquireModify并堵在global上。
		    Checkpoint.AsyncStartWithPendingAction(r -> S); // 把当前记录加入Pending操作，启动Checkpoint。
		    return No Result; // 结果在 Checkpoint 后发送，异步的。
	    }
        }
    }
    finally
    {
    	lockey.ExitWriteLock();
    }
}

GameServer.ReduceToI()
{
    Record r = cache.Get(key);
    if (null == r) // I
        return Success With Do Nothing. 哪里肯定出错了。

    lockey.EnterWriteLock(); // 锁住本地记录锁。和事务并发，好像需要，还没细想。
    try
    {
        lock (r)
        {
            switch (r.state)
            {
	        case S:
		    cache.Remove(key); // S 状态，马上删除，如果有并发事务，会重做。
		    return Success With I;
		case M:
		    // 马上删除，Checkpont不会去Cache中查找东西，不会有问题。
		    // 这可能会导致新的LoadRecordToCache最终会被堵在Global的lock(cachestate)上。
		    cache.Remove(I);
		    Checkpoint.AsyncStartWithPendingAction(r -> I); // 把当前记录加入Pending操作，启动Checkpoint。
		    return No Result; // 结果在 Checkpoint 后继续处理，异步的。
	    }
        }
    }
    finally
    {
    	lockey.ExitWriteLock();
    }
}

Checkpoint.AsyncStartWithPendingAction(r -> S or I)
{
	// TODO 细节还需要考虑
	1 必须在完全写到后端数据库后才能修改global的记录状态。
}

其他 
1 lockey读写锁 和 lock(record) 的关系要理一下。
2 现在的流程都是同步的，如果是异步，将增加很多状态，会变得很复杂。
  考虑先实现同步版本。
3 Cache.Remove 也要同步状态。
4 Checkpoing收集多个Reduce的能力：使用flushReadLock，在flushWriteLock之后不能再加入新的请求。
