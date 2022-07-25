using System;
using System.Diagnostics;
using System.IO;
using System.IO.MemoryMappedFiles;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using NLog;
using Zeze.Serialize;
using Zeze.Services;
using Zeze.Util;

namespace Zeze.Transaction;

/// <summary>
/// 说明详见：zeze/ZezeJava/.../AchillesHeelDaemon.java
/// </summary>
public class AchillesHeelDaemon
{
    static readonly Logger logger = LogManager.GetCurrentClassLogger();

    readonly Application Zeze;
    readonly GlobalAgentBase[] Agents;
    readonly ThreadDaemon TD;
    readonly ProcessDaemon PD;

    public AchillesHeelDaemon(Application zeze, GlobalAgentBase[] agents)
    {
        Zeze = zeze;
        Agents = (GlobalAgentBase[])agents.Clone();
        var peerPort = Environment.GetEnvironmentVariable(Daemon.PropertyNamePort);
        if (peerPort != null)
        {
            PD = new ProcessDaemon(this, int.Parse(peerPort));
            TD = null;
        }
        else
        {
            PD = null;
            TD = new ThreadDaemon(this);
        }
    }

    public void Start()
    {
        TD?.Start();
        PD?.Start();
    }

    public void StopAndJoin()
    {
        TD?.StopAndJoin();
        PD?.StopAndJoin();
    }

    public void onInitialize(GlobalAgentBase agent)
    {
        if (PD != null)
        {
            try
            {
                var config = agent.Config;
                Daemon.sendCommand(PD.UdpSocket, PD.DaemonSocketAddress,
                    new Daemon.GlobalOn(Zeze.Config.ServerId, agent.GlobalCacheManagerHashIndex,
                        config.ServerDaemonTimeout, config.ServerReleaseTimeout));
            }
            catch (IOException e)
            {
                logger.Error(e, string.Empty);
            }
        }
    }

    public void setProcessDaemonActiveTime(GlobalAgentBase agent, long value)
    {
        PD?.SetActiveTime(agent, value);
    }

    sealed class ProcessDaemon
    {
        readonly AchillesHeelDaemon AchillesHeelDaemon;
        readonly Thread Thread;
        internal readonly UdpClient UdpSocket = new(new IPEndPoint(IPAddress.Loopback, 0));
        internal readonly IPEndPoint DaemonSocketAddress;
        readonly MemoryMappedFile MMap;
        readonly MemoryMappedViewAccessor MMapAccessor;
        readonly long[] LastReportTime;
        volatile bool Running = true;

        public ProcessDaemon(AchillesHeelDaemon ahd, int peer)
        {
            AchillesHeelDaemon = ahd;
            var fileName = Path.GetTempFileName();
            MMap = MemoryMappedFile.CreateFromFile(fileName);
            MMapAccessor = MMap.CreateViewAccessor(0, 8L * ahd.Agents.Length);
            DaemonSocketAddress = new IPEndPoint(IPAddress.Loopback, peer);
            LastReportTime = new long[ahd.Agents.Length];
            UdpSocket.Client.SendTimeout = 200;
            Daemon.sendCommand(UdpSocket, DaemonSocketAddress,
                new Daemon.Register(ahd.Zeze.Config.ServerId, ahd.Agents.Length, fileName));
            Thread = new Thread(Run);
        }

        public void SetActiveTime(GlobalAgentBase agent, long value)
        {
            // 优化！活动时间设置很频繁，降低报告频率。
            var reportDiff = agent.GetActiveTime() - LastReportTime[agent.GlobalCacheManagerHashIndex];
            if (reportDiff < 1000)
                return;
            LastReportTime[agent.GlobalCacheManagerHashIndex] = agent.GetActiveTime();

            var bb = ByteBuffer.Allocate(8);
            bb.WriteLong8(value);

            // TODO 不同的GlobalAgent能并发起来。由于上面的低频率报告优化，这个不是很必要了。
            MMapAccessor.WriteArray(agent.GlobalCacheManagerHashIndex * 8, bb.Bytes, 0, 8);
        }

        void Run()
        {
            try
            {
                while (Running)
                {
                    try
                    {
                        var cmd = Daemon.receiveCommand(UdpSocket);
                        //noinspection SwitchStatementWithTooFewBranches
                        switch (cmd.command())
                        {
                            case Daemon.Release.Command:
                                var r = (Daemon.Release)cmd;
                                logger.Info($"receiveCommand {r.GlobalIndex}");
                                var agent = AchillesHeelDaemon.Agents[r.GlobalIndex];
                                var config = agent.Config;
                                var rr = agent.CheckReleaseTimeout(
                                    Time.NowUnixMillis, config.ServerReleaseTimeout);
                                if (rr == GlobalAgentBase.CheckReleaseResult.Timeout)
                                {
                                    // 本地发现超时，先自杀，不用等进程守护来杀。
                                    logger.Fatal($"ProcessDaemon.AchillesHeelDaemon global release timeout. index={r.GlobalIndex}");
                                    LogManager.Shutdown();
                                    Process.GetCurrentProcess().Kill();
                                }

                                if (rr != GlobalAgentBase.CheckReleaseResult.Releasing)
                                {
                                    // 这个判断只能避免正在Releasing时不要启动新的Release。
                                    // 如果Global一直恢复不了，那么每ServerDaemonTimeout会再次尝试Release，
                                    // 这里没法快速手段判断本Server是否存在从该Global获取的记录锁。
                                    // 在Agent中增加获得的计数是个方案，但挺烦的。
                                    logger.Warn($"ProcessDaemon.StartRelease ServerDaemonTimeout={config.ServerDaemonTimeout}");
                                    agent.StartRelease(AchillesHeelDaemon.Zeze);
                                }

                                break;
                        }
                    }
                    catch (SocketException)
                    {
                        // skip
                    }

                    // 执行KeepAlive
                    var now = Time.NowUnixMillis;
                    foreach (var agent in AchillesHeelDaemon.Agents)
                    {
                        var config = agent.Config;
                        if (null == config)
                            continue; // skip agent not login

                        var idle = now - agent.GetActiveTime();
                        if (idle > config.ServerKeepAliveIdleTimeout)
                        {
                            //logger.debug("KeepAlive ServerKeepAliveIdleTimeout={}", config.ServerKeepAliveIdleTimeout);
                            agent.KeepAlive();
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                // 这个线程不准出错。除了里面应该忽略的。
                logger.Fatal(ex, "ProcessDaemon.AchillesHeelDaemon");
                LogManager.Shutdown();
                Process.GetCurrentProcess().Kill();
            }
        }

        public void Start()
        {
            Thread.Start();
        }

        public void StopAndJoin()
        {
            Running = false;
            try
            {
                Thread.Join();
            }
            catch (Exception e)
            {
                logger.Error(e, "ProcessDaemon.stopAndJoin");
            }

            try
            {
                MMapAccessor.Dispose();
            }
            catch (Exception e)
            {
                logger.Error(e, "MMapAccessor.Dispose");
            }

            try
            {
                MMap.Dispose();
            }
            catch (Exception e)
            {
                logger.Error(e, "MMap.Dispose");
            }
        }
    }

    class ThreadDaemon
    {
        readonly AchillesHeelDaemon AchillesHeelDaemon;
        readonly Thread Thread;
        volatile bool Running = true;

        public ThreadDaemon(AchillesHeelDaemon achilles)
        {
            AchillesHeelDaemon = achilles;
            Thread = new Thread(Run);
        }

        public void Start()
        {
            Thread.Start();
        }

        public void StopAndJoin()
        {
            Running = false;
            Thread.Join();
        }

        void Run()
        {
            lock (this)
            {
                try
                {
                    while (Running)
                    {
                        var now = Time.NowUnixMillis;
                        for (var i = 0; i < AchillesHeelDaemon.Agents.Length; i++)
                        {
                            var agent = AchillesHeelDaemon.Agents[i];
                            var config = agent.Config;
                            if (config == null)
                                continue; // skip agent not login

                            var rr = agent.CheckReleaseTimeout(now, config.ServerReleaseTimeout);
                            if (rr == GlobalAgentBase.CheckReleaseResult.Timeout)
                            {
                                logger.Fatal($"AchillesHeelDaemon global release timeout. index={i}");
                                LogManager.Shutdown();
                                Process.GetCurrentProcess().Kill();
                            }

                            var idle = now - agent.GetActiveTime();
                            if (idle > config.ServerKeepAliveIdleTimeout)
                            {
                                // logger.Debug($"KeepAlive ServerKeepAliveIdleTimeout={config.ServerKeepAliveIdleTimeout}");
                                agent.KeepAlive();
                            }

                            if (idle > config.ServerDaemonTimeout)
                            {
                                if (rr != GlobalAgentBase.CheckReleaseResult.Releasing)
                                {
                                    // 这个判断只能避免正在Releasing时不要启动新的Release。
                                    // 如果Global一直恢复不了，那么每ServerDaemonTimeout会再次尝试Release，
                                    // 这里没法快速手段判断本Server是否存在从该Global获取的记录锁。
                                    // 在Agent中增加获得的计数是个方案，但挺烦的。
                                    logger.Warn($"StartRelease ServerDaemonTimeout {config.ServerReleaseTimeout}");
                                    agent.StartRelease(AchillesHeelDaemon.Zeze);
                                }
                            }
                        }

                        Monitor.Wait(this, 1000);
                    }
                }
                catch (Exception ex)
                {
                    // 这个线程不准出错。
                    logger.Fatal(ex, "AchillesHeelDaemon");
                    LogManager.Shutdown();
                    Process.GetCurrentProcess().Kill();
                }
            }
        }
    }
}
