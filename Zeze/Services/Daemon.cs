#nullable enable

using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO.MemoryMappedFiles;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using NLog;
using Zeze.Serialize;
using Zeze.Util;

namespace Zeze.Services;

public static class Daemon
{
    public const string PropertyNamePort = "Zeze.ProcessDaemon.Port";
    static readonly Logger logger = LogManager.GetCurrentClassLogger();

    // Key Is ServerId。每个Server对应一个Monitor。
    // 正常使用是一个Daemon对应一个Server。
    // 写成支持多个Server是为了跑Simulate测试。
    static readonly ConcurrentDictionary<int, Monitor> Monitors = new();
    static UdpClient? UdpSocket;
    static Process? Subprocess;

    static readonly ConcurrentDictionary<long, PendingPacket> Pending = new();
    static volatile SchedulerTask? Timer;

    public static void Main(string[] args)
    {
        // udp for subprocess register
        UdpSocket = new UdpClient(new IPEndPoint(IPAddress.Loopback, 0));
        UdpSocket.Client.SendTimeout = 200;

        ProcessStartInfo startInfo = new(args[0])
        {
            Environment =
            {
                [PropertyNamePort] = ((IPEndPoint)UdpSocket.Client.LocalEndPoint!).Port.ToString()
            },
            RedirectStandardInput = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };
        for (var i = 1; i < args.Length; i++)
            startInfo.ArgumentList.Add(args[i]);

        Subprocess = Process.Start(startInfo)!;

        try
        {
            while (true)
            {
                var exitCode = mainRun();
                if (0 == exitCode)
                    break;
                joinMonitors();
                logger.Warn($"Subprocess Restart! ExitCode={exitCode}");
                Subprocess = Process.Start(startInfo)!;
            }
        }
        catch (Exception ex)
        {
            logger.Error(ex, "Daemon.main");
        }
        finally
        {
            // 退出的时候，确保销毁服务进程。
            Subprocess.Kill();
        }
    }

    static int mainRun()
    {
        while (true)
        {
            try
            {
                // 轮询：等待Global配置以及等待子进程退出。
                try
                {
                    var cmd = receiveCommand(UdpSocket!);
                    switch (cmd.command())
                    {
                        case Register.Command:
                            var reg = (Register)cmd;
                            var code = 0;
                            if (Monitors.ContainsKey(reg.ServerId))
                                code = 1;
                            else
                            {
                                var monitor = new Monitor(reg);
                                monitor.thread = new Thread(monitor.run);
                                Monitors[reg.ServerId] = monitor;
                                monitor.thread.Start();
                            }

                            sendCommand(UdpSocket!, cmd.Peer!, new CommonResult(reg.ReliableSerialNo, code));
                            logger.Info($"Register! Server={reg.ServerId} code={code}");
                            break;

                        case GlobalOn.Command:
                            var on = (GlobalOn)cmd;
                            Monitors[on.ServerId].setConfig(on.GlobalIndex, on.GlobalConfig);
                            sendCommand(UdpSocket!, cmd.Peer!, new CommonResult(on.ReliableSerialNo, 0));
                            logger.Info($"GlobalOn! Server={on.ServerId}" +
                                        $" ServerDaemonTimeout={on.GlobalConfig.ServerDaemonTimeout}" +
                                        $" ServerReleaseTimeout={on.GlobalConfig.ServerReleaseTimeout}");
                            break;
                    }
                }
                catch (SocketException)
                {
                    // skip
                }

                if (Subprocess!.WaitForExit(1))
                {
                    return Subprocess.ExitCode;
                }
            }
            catch (Exception ex)
            {
                logger.Fatal(ex, "Daemon.mainRun");
                fatalExit();
                return -1; // never get here
            }
        }
    }

    public static void fatalExit()
    {
        Subprocess?.Kill();
        LogManager.Shutdown();
        Process.GetCurrentProcess().Kill();
    }

    public static void joinMonitors()
    {
        foreach (var monitor in Monitors.Values)
            monitor.stopAndJoin();
        Monitors.Clear();
    }

    public static void destroySubprocess()
    {
        Subprocess?.Kill();
        joinMonitors();
    }

    public class PendingPacket
    {
        public readonly UdpClient Socket;
        public readonly IPEndPoint Peer;
        public readonly byte[] Packet;
        public long SendTime = Time.NowUnixMillis;

        public PendingPacket(UdpClient socket, IPEndPoint peer, byte[] packet)
        {
            Socket = socket;
            Peer = peer;
            Packet = packet;
        }
    }

    public static void sendCommand(UdpClient socket, IPEndPoint peer, Command cmd)
    {
        var bb = ByteBuffer.Allocate();
        bb.WriteInt(cmd.command());
        cmd.Encode(bb);
        var p = bb.Copy();
        if (cmd.IsRequest())
        {
            if (!Pending.TryAdd(cmd.ReliableSerialNo, new PendingPacket(socket, peer, p)))
                throw new Exception("Duplicate ReliableSerialNo=" + cmd.ReliableSerialNo);

            // auto start Timer
            if (null == Timer)
            {
                lock (Pending)
                {
                    if (null == Timer)
                    {
                        Timer = Scheduler.Schedule(_ =>
                        {
                            var now = Time.NowUnixMillis;
                            foreach (var pending in Pending.Values)
                            {
                                if (now - pending.SendTime > 1000)
                                {
                                    pending.Socket.Send(pending.Packet, pending.Peer);
                                    pending.SendTime = now;
                                }
                            }
                        }, 1000, 1000);
                        // Zeze.Util.ShutdownHook.add(()->Timer.cancel(false));
                    }
                }
            }
        }

        socket.Send(p, peer);
    }

    public static Command receiveCommand(UdpClient socket)
    {
        var cmd = _receiveCommand(socket);
        if (cmd.ReliableSerialNo != 0)
            Pending.Remove(cmd.ReliableSerialNo, out _);
        return cmd;
    }

    static Command _receiveCommand(UdpClient socket)
    {
        var peer = new IPEndPoint(IPAddress.Any, 0);
        var buf = socket.Receive(ref peer);
        var bb = ByteBuffer.Wrap(buf);
        var cmd = bb.ReadInt();
        switch (cmd)
        {
            case Register.Command:
                return new Register(bb, peer);
            case CommonResult.Command:
                return new CommonResult(bb, peer);
            case GlobalOn.Command:
                return new GlobalOn(bb, peer);
            case Release.Command:
                return new Release(bb, peer);
        }

        throw new Exception("Unknown Command =" + cmd);
    }

    public sealed class Monitor
    {
        public readonly int ServerId;
        public readonly IPEndPoint PeerSocketAddress;
        readonly MemoryMappedFile MMap;
        readonly MemoryMappedViewAccessor MMapAccessor;
        readonly AchillesHeelConfig?[] GlobalConfigs;
        internal Thread? thread;
        volatile bool Running = true;

        public Monitor(Register reg)
        {
            ServerId = reg.ServerId;
            PeerSocketAddress = reg.Peer!;
            GlobalConfigs = new AchillesHeelConfig[reg.GlobalCount];

            MMap = MemoryMappedFile.CreateFromFile(reg.MMapFileName);
            MMapAccessor = MMap.CreateViewAccessor(0, reg.GlobalCount * 8);
        }

        public AchillesHeelConfig? getConfig(int index)
        {
            lock (this)
            {
                return GlobalConfigs[index];
            }
        }

        public void setConfig(int index, AchillesHeelConfig config)
        {
            lock (this)
            {
                GlobalConfigs[index] = config;
            }
        }

        public void Dispose()
        {
            MMapAccessor.Dispose();
            MMap.Dispose();
        }

        ByteBuffer copyMMap()
        {
            var copy = new byte[GlobalConfigs.Length * 8];
            MMapAccessor.ReadArray(0, copy, 0, copy.Length);
            return ByteBuffer.Wrap(copy);
        }

        public void run()
        {
            try
            {
                while (Running)
                {
                    var bb = copyMMap();
                    var now = Time.NowUnixMillis;
                    for (var i = 0; i < GlobalConfigs.Length; ++i)
                    {
                        var activeTime = bb.ReadLong8();
                        var config = getConfig(i);
                        if (null == config)
                            continue; // skip not ready global

                        var idle = now - activeTime;
                        if (idle > config.ServerReleaseTimeout)
                        {
                            logger.Info($"destroySubprocess {now} - {activeTime} > {config.ServerReleaseTimeout}");
                            destroySubprocess();
                            // daemon main will restart subprocess!
                        }
                        else if (idle > config.ServerDaemonTimeout)
                        {
                            logger.Info($"sendCommand Release-{i} {now} - {activeTime} > {config.ServerDaemonTimeout}");
                            // 在Server执行Release期间，命令可能重复发送。
                            // 重复命令的处理由Server完成，
                            // 这里重发也是需要的，刚好解决Udp不可靠性。
                            sendCommand(UdpSocket!, PeerSocketAddress, new Release(i));
                        }

                        //noinspection BusyWait
                        Thread.Sleep(1000);
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Fatal(ex, "Monitor.run");
                fatalExit();
            }
        }

        public void stopAndJoin()
        {
            Running = false;
            thread?.Join();
        }
    }

    public abstract class Command : Serializable
    {
        static long Seed;

        public IPEndPoint? Peer;
        public long ReliableSerialNo;
        bool isRequest;

        public abstract int command();

        public bool IsRequest()
        {
            return isRequest;
        }

        protected void setReliableSerialNo()
        {
            do
                ReliableSerialNo = Interlocked.Increment(ref Seed);
            while (ReliableSerialNo == 0);
            isRequest = true;
        }

        public virtual void Encode(ByteBuffer bb)
        {
            bb.WriteLong(ReliableSerialNo);
        }

        public virtual void Decode(ByteBuffer bb)
        {
            ReliableSerialNo = bb.ReadLong();
        }
    }

    // 精简版本配置。仅传递Daemon需要的参数过来。
    public sealed class AchillesHeelConfig : Serializable
    {
        public int ServerDaemonTimeout;
        public int ServerReleaseTimeout;

        public void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerDaemonTimeout);
            bb.WriteInt(ServerReleaseTimeout);
        }

        public void Decode(ByteBuffer bb)
        {
            ServerDaemonTimeout = bb.ReadInt();
            ServerReleaseTimeout = bb.ReadInt();
        }
    }

    public sealed class Register : Command
    {
        public const int Command = 0;

        public int ServerId;
        public int GlobalCount;
        public string MMapFileName;

        public Register(int serverId, int c, string name)
        {
            ServerId = serverId;
            GlobalCount = c;
            MMapFileName = name;
            setReliableSerialNo(); // enable reliable
        }

        public Register(ByteBuffer bb, IPEndPoint peer)
        {
            Decode(bb);
            Peer = peer;
        }

        public override int command()
        {
            return Command;
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalCount);
            bb.WriteString(MMapFileName);
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            ServerId = bb.ReadInt();
            GlobalCount = bb.ReadInt();
            MMapFileName = bb.ReadString();
        }
    }

    public sealed class GlobalOn : Command
    {
        public const int Command = 1;

        public int ServerId;
        public int GlobalIndex;
        public readonly AchillesHeelConfig GlobalConfig = new();

        public GlobalOn(int serverId, int index, int server, int release)
        {
            ServerId = serverId;
            GlobalIndex = index;
            GlobalConfig.ServerDaemonTimeout = server;
            GlobalConfig.ServerReleaseTimeout = release;
            setReliableSerialNo(); // enable reliable
        }

        public GlobalOn(ByteBuffer bb, IPEndPoint peer)
        {
            Decode(bb);
            Peer = peer;
        }

        public override int command()
        {
            return Command;
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalIndex);
            GlobalConfig.Encode(bb);
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            ServerId = bb.ReadInt();
            GlobalIndex = bb.ReadInt();
            GlobalConfig.Decode(bb);
        }
    }

    public sealed class CommonResult : Command
    {
        public const int Command = 2;

        public int Code;

        public CommonResult(long serial, int code)
        {
            ReliableSerialNo = serial;
            Code = code;
        }

        public CommonResult(ByteBuffer bb, IPEndPoint peer)
        {
            Decode(bb);
            Peer = peer;
        }

        public override int command()
        {
            return Command;
        }

        public override void Encode(ByteBuffer bb)
        {
            base.Encode(bb);
            bb.WriteInt(Code);
        }

        public override void Decode(ByteBuffer bb)
        {
            base.Decode(bb);
            Code = bb.ReadInt();
        }
    }

    public sealed class Release : Command
    {
        public const int Command = 3;

        public int GlobalIndex;

        public Release(int index)
        {
            GlobalIndex = index;
        }

        public Release(ByteBuffer bb, IPEndPoint peer)
        {
            Decode(bb);
            Peer = peer;
        }

        public override int command()
        {
            return Command;
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(GlobalIndex);
        }

        public override void Decode(ByteBuffer bb)
        {
            GlobalIndex = bb.ReadInt();
        }
    }
}
