<!--
Module 包含 Table 默认私有，可以通过增加方法返回并暴露出去
Module 使用 partical 把生成部分放到gen下
Module 生成 IModule 和 Module，这样IModule接口方法发生增删，方法参数变更等，代码编译可以报错。

协议里面的数值默认大于等于0，支持负数需要明确声明。
Config 独立
ByteBuffer.SkipUnknownField 移到其他地方
XXX 统一 bean 和 xbean 定义的话，怎么区分生成：被 table 引用的 bean 自动生成一份到某个特别的目录
统一生成工具，应用框架和数据存储框架定义分开？两者的数据bean的定义应该是不一样的，重用的可能性很小。
	主要入口 application 数据库定义 database. database.module 必须在 application 中存在。

去掉 xbean.Const xbean.Data
去掉 select。拷贝数据直接由 bean duplicate 支持
managed
数据变更?
Net.Manager 怎么重新定义？现在这个不够灵活。

GameServer.TableCache
    ConcurrentDictionary<K, Record<K, V>> cache;
    记录在Cache中的三个状态。
    M:Modify
    S:Share
    I:Invalid(Not In Cache)

Record<K, V> LoadRecordToCache(K key)
{
    Record r = cache.GetOrAdd(key, new Record<K, V>(I)); // 使用factory可以避免每次请求浪费一个new对象

    loch(r) // 阻止同一个记录并发访问。
    {
        if (r.State is S or M)
	    return r;

        // is I
        Global.AcquireShare();
        r.Bean = Storage.Load();
        r.State = s;
        return r;
    }
}

class GlobalCacheManager 全局缓存状态管理.
{
    ConcurrentDictionary<TableKey, CacheState> global;
}

class Global.CacheState
{
    List<GameServer> shareOccupant;
    GameServer modifyOccupant; // 写拥有者，只有一个，这个不为null时，shareOccupant肯定是空的。
    // List<GameServerRequest> waitQueue; // 请求等待队列按顺序。share,modify请求都在一起。异步的时候需要吧，没想好。
}

State Global.AcquireShare(sender, tableKey)
{
    CacheState state = global.GetOrAdd(tableKey, new CacheState()); // factory
    lock(state) // 阻止同一个记录并发访问。
    {
        if (state.modifyOccupant != null)
	{
	    if (state.modifyOccupant == sender)
	    	return Success With State M; // 已经是M状态了。

	    modifyOccupant.ReduceToS();
	    modifyOccupant = null;
	    shareOccupant.Add(sender)
	    return Success With State S;
	}

	shareOccupant.Add(sender)
	return Success With State S;
    }
}

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

GameServer.ReadLock
    lockey.EnterLock(false);
    lock(originRecord) // ??? 没想好
    {
        switch (originRecord.state)
	{
		case I: // 记录不存在，将来读取的时候会启动LoadRecordToCache.
			return Redo Transaction;
		case M:
			return Success;
		case S:
			return Success;
	}
    }

GameServer.WriteLock // 请求写锁流程
    lockey.EnterLock(true);
    lock(originRecord) // ??? 没想好
    {
        switch (originRecord.state)
	{
		case I: // 记录不存在，将来读取的时候会启动LoadRecordToCache.
			return Redo Transaction;
		case M:
			return Success;
		case S:
			Global.AcquireModify();
			originRecord.state = M;
			return Success;
	}
    }

Global.AcquireModify(sender, tableKey)
{
    CacheState state = global.GetOrAdd(tableKey, new CacheState()); // factory
    lock(state) // 阻止同一个记录并发访问。
    {
        if (state.modifyOccupant != null)
	{
	    if (state.modifyOccupant == sender)
		return Success: Your State Already is M;

	    modifyOccupant.ReduceToI();
	    modifyOccupant = sender;
	    return Success With State M;
	}

	foreach (var share in shareOccupant)
	{
		if (share == sender)
			continue;
		share.RecureToI();
	}
	shareOccupant.Clear();
	modifyOccupant = sender;
	return Success With State M;
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
