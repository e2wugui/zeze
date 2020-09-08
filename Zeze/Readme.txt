TODO 
协议里面的数值默认大于等于0，支持负数需要明确声明。
数据变更?

其他 
1 lockey读写锁 和 lock(record) 的关系要理一下。
2 现在的流程都是同步的，如果是异步，将增加很多状态，会变得很复杂。
  考虑先实现同步版本。
3 Cache.Remove 也要同步状态。
4 Checkpoint收集多个Reduce的能力：使用flushReadLock，在flushWriteLock之后不能再加入新的请求。
* Checkpoint发现Reduce，等待的问题。
7 同时在一个进程内建立多个App是可行的，在多个App中仍然支持事务，但是这两个App不能同时属于一个GlobalCacheManager.

（010）82818652 博瑞祥宏4s点保养。
