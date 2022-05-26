package Zeze.Transaction;

/**
 * Server与Global之间记录锁管理机制。这里锁有三个状态，Modify,Share,Invalid。
 * 0. Acquire Rpc
 *    Server向Global申请记录锁。Release也是通过这个操作处理，锁状态的一种，即Invalid。
 *
 * 1. NormalClose Rpc
 *    Server正常退出时调用，Global释放所有分配到该Server的所有记录锁。
 *
 * 2. Login Rpc
 *    Server初次与Global建立连接时发送。Global会释放该Server旧的记录锁。
 *
 * 3. ReLogin Rpc
 *    Server与Global之间的连接短暂断开又重新连上时发送。Global简单的把新连接绑定上，不会释放已经分配的锁。
 *
 * 4. KeepAlive Rpc
 *    Server空闲时发送给Global。
 *    LastActiveTime = Acquire或者KeepAlive的活动时间。
 *    Global为每个Server都维护LastActiveTime。在收到Acquire或者KeepAlive设为now。
 *    Server为每个Global都维护LastActiveTime。在收到Acquire.Response或者KeepAlive.Response时设为now。
 *    Server每秒检查LastActiveTime，发现 now - LastActiveTime > ServerIdleTimeout(1s) 时发送KeepAlive。
 *    即超过1s没有Acquire请求的处理，则补上一个KeepAlive。
 *
 * 5. Global发现Server断开连接
 *    不做任何处理。短暂断开允许重连。锁释放由Global-AchillesHeel-Daemon处理。
 *
 * 6. Global-AchillesHeel-Daemon
 *    每5秒扫描一遍所有Server，发现 now - Server.LastActiveTime > GlobalDaemonTimeout(15秒)，释放该Server所有锁。【Important!】
 *    a) 5秒慢检查;
 *    b) 15秒，最终超时。Server必须在这之前释放自己的锁或者退出进程；
 *
 * 7. Server-AchillesHeel-Daemon
 *    Server每秒扫描一遍Global，发现 now - Global.LastActiveTime > ServerDaemonTimeout(5秒)，启动本地释放锁线程。
 *    a) 2秒，需要大于KeepAlive的空闲间隔。
 *    b) 本地释放锁必须在独立线程执行，守护线程等待释放完成，如果释放超过10秒还未完成，就自杀！【Important！】
 *    c) 守护线程一开始创建，做最简单的事情，确保需要的时候，最终的自杀能成功。【Important！】
 *
 * 8. ServerKeepAlive.IdleTimeout < ServerDaemonTimeout < GlobalDaemonTimeout
 *    rpc.Timeout? 现在Global的Rpc.Timeout设的比较长，需要重新考虑。
 *
 * *. Change
 *    不再需要的旧实现：Server在发现Global断开连接，马上释放本地资源。
 *    需要新实现：启用KeepAlive，并处理LastActiveTime。
 *
 * *. 原来的思路参见 zeze/GlobalCacheManager/Cleanup.txt。在这个基础上增加了KeepAlive。
 */
public class AchillesHeelDaemon {
}
