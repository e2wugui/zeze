package Zeze.Transaction;

/**
 * 【问题】 Server失联，Global回收记录锁怎么处理？
 * Server与Global之间记录锁管理机制。这里锁有三个状态，Modify,Share,Invalid。
 * 下面分析Server-Global之间所有的交互。
 *
 * 0. Acquire Rpc
 *    Server向Global申请记录锁。Release也是通过这个操作处理，Release的锁状态是Invalid。
 *
 * 1. NormalClose Rpc
 *    Server正常退出时发送，Global主动释放所有分配到该Server的记录锁。
 *
 * 2. Login Rpc
 *    Server初次与Global建立连接时发送。Global会释放该Server上已经分配的记录锁。
 *    一般是Server宕机又重启了。
 *
 * 3. ReLogin Rpc
 *    Server与Global之间的连接短暂断开又重新连上时发送。Global简单的把新连接绑定上，不会释放已经分配的锁。
 *
 * 4. KeepAlive Rpc
 *    Server空闲时发送给Global。
 *    LastActiveTime = Acquire或者KeepAlive的活动时间。
 *    Global为每个Server都维护LastActiveTime。在收到Acquire或者KeepAlive设为now。
 *    Server为每个Global都维护LastActiveTime。在收到Acquire.Response或者KeepAlive.Response时设为now。
 *    Server每秒检查LastActiveTime，发现 now - LastActiveTime > ServerIdleTimeout 时发送KeepAlive。
 *
 * 5. Global发现Server断开连接
 *    不做任何处理。短暂断开允许重连。锁释放由Global-AchillesHeel-Daemon处理。
 *
 * 6. Global-AchillesHeel-Daemon
 *    每5秒扫描一遍所有Server，发现 now - Server.LastActiveTime > GlobalDaemonTimeout，释放该Server所有锁。【Important!】
 *    a) 5秒慢检查;如果Server很多，避免轮询消耗太多cpu。慢检查会造成实际回收时间超出超时设置，但不会造成锁状态问题。
 *    b) GlobalDaemonTimeout，最终超时。Server必须在这之前释放自己持有的锁或者退出进程；
 *
 * 7. Server-AchillesHeel-Daemon
 *    Server每秒扫描一遍Global，发现 now - Global.LastActiveTime > ServerDaemonTimeout，启动本地释放锁线程。
 *    a) ServerDaemonTimeout需要大于KeepAlive的空闲间隔 + 尝试重连的时间。
 *    b) 本地释放锁必须在独立线程执行，守护线程等待释放完成，如果释放线程超过ServerReleaseTimeout还未完成，就自杀！【Important！】
 *    c) 守护线程一开始创建，做最简单的事情，确保需要的时候，最终的自杀能成功。【Important！】
 *
 * 8. Timeout
 *    a) ServerKeepAlive.IdleTimeout < ServerDaemonTimeout;
 *    b) ServerDaemonTimeout + ServerReleaseTimeout < GlobalDaemonTimeout; 必须满足而且不能太接近【Important！】
 *    c) 其他Timeout：Acquire.Timeout, Reduce.Timeout, KeepAlive.Timeout, Server.FastErrorPeriod, Global.ForbidPeriod
 *
 * 9. Timeout Config
 *    a) 在Global配置两个参数：MaxNetPing=1000, ServerProcessTime=500
 *    b) 其他Timeout配置全部从上面两个参数按一定比例计算得出。
 *    c) Gs不独立配置，Login的时候从Global得到配置。避免由于配置不一致导致问题。
 *    d) Global多个实例允许不一样的配置，异构网络里面可能需要。
 *
 * 10. Timeout Compute
 *    *) Reconnect.Timer = 1000;
 *    a) ServerKeepAlive.IdleTimeout = MaxNetPing;
 *    b) ServerDaemonTimeout = MaxNetPing * 4; // 期间允许4次重连尝试
 *    c) ServerReleaseTimeout = 10 * 1000;
 *    d) GlobalDaemonTimeout = (ServerDaemonTimeout + ServerReleaseTimeout) * 1.2f; // 多出20%
 *    e) Reduce.Timeout = MaxNetPing + ServerProcessTime;
 *    f) Acquire.Timeout = Reduce.Timeout + MaxNetPing
 *    g) KeepAlive.Timeout = MaxNetPing;
 *    h) Server.FastErrorPeriod = ServerDaemonTimeout / 2; // Global请求失败一次即进入这个超时，期间所有的Acquire都本地马上失败。
 *    i) Global.ForbidPeriod = ServerDaemonTimeout / 2; // Reduce失败一次即进入这个超时，期间所有的Reduce马上失败。
 *
 * 11. Change Log
 *    不再需要的旧实现：Server在发现Global断开连接，马上释放本地资源。改成由AchillesHeelDaemon处理。
 *
 * *. 原来的思路参见 zeze/GlobalCacheManager/Cleanup.txt。在这个基础上增加了KeepAlive。
 */
public class AchillesHeelDaemon {
}
