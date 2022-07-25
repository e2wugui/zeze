#nullable enable

using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
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
    static UdpClient UdpSocket = null!;
    static Process? Subprocess;

    static readonly ConcurrentDictionary<long, PendingPacket> Pendings = new();
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

        try
        {
            while (true)
            {
                Subprocess = Process.Start(startInfo)!;
                var exitCode = MainRun();
                if (exitCode == 0)
                    break;
                JoinMonitors();
                logger.Warn($"Subprocess Restart! ExitCode={exitCode}");
            }
        }
        catch (Exception ex)
        {
            logger.Error(ex, "Daemon.main");
        }
        finally
        {
            // 退出的时候，确保销毁服务进程。
            Subprocess?.Kill();
        }
    }

    static int MainRun()
    {
        while (true)
        {
            try
            {
                // 轮询：等待Global配置以及等待子进程退出。
                try
                {
                    var cmd = ReceiveCommand(UdpSocket);
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
                                Monitors[reg.ServerId] = monitor;
                                monitor.Start();
                            }

                            SendCommand(UdpSocket, cmd.Peer!, new CommonResult(reg.ReliableSerialNo, code));
                            logger.Info($"Register! Server={reg.ServerId} code={code}");
                            break;

                        case GlobalOn.Command:
                            var on = (GlobalOn)cmd;
                            code = 0;
                            if (Monitors.TryGetValue(on.ServerId, out var mon))
                            {
                                mon.SetConfig(on.GlobalIndex, on.GlobalConfig);
                                logger.Info($"GlobalOn! Server={on.ServerId}" +
                                            $" ServerDaemonTimeout={on.GlobalConfig.ServerDaemonTimeout}" +
                                            $" ServerReleaseTimeout={on.GlobalConfig.ServerReleaseTimeout}");
                            }
                            else
                            {
                                logger.Warn($"GlobalOn! not found ServerId={on.ServerId}" +
                                            $" ServerDaemonTimeout={on.GlobalConfig.ServerDaemonTimeout}" +
                                            $" ServerReleaseTimeout={on.GlobalConfig.ServerReleaseTimeout}");
                                code = 1;
                            }

                            SendCommand(UdpSocket, cmd.Peer!, new CommonResult(on.ReliableSerialNo, code));
                            break;
                    }
                }
                catch (SocketException)
                {
                    // skip
                }

                if (Subprocess!.WaitForExit(1))
                    return Subprocess.ExitCode;
            }
            catch (Exception ex)
            {
                logger.Fatal(ex, "Daemon.mainRun");
                FatalExit();
                return -1; // never get here
            }
        }
    }

    static void FatalExit()
    {
        Subprocess!.Kill();
        LogManager.Shutdown();
        Process.GetCurrentProcess().Kill();
    }

    static void JoinMonitors()
    {
        foreach (var monitor in Monitors.Values)
            monitor.StopAndJoin();
        Monitors.Clear();
    }

    static void DestroySubprocess()
    {
        Subprocess!.Kill();
        JoinMonitors();
    }

    sealed class PendingPacket
    {
        public readonly UdpClient Socket;
        public readonly IPEndPoint Peer;
        public readonly ByteBuffer Packet;
        public long SendTime = Time.NowUnixMillis;

        public PendingPacket(UdpClient socket, IPEndPoint peer, ByteBuffer packet)
        {
            Socket = socket;
            Peer = peer;
            Packet = packet;
        }
    }

    public static void SendCommand(UdpClient socket, IPEndPoint peer, Command cmd)
    {
        var bb = ByteBuffer.Allocate(5);
        bb.WriteInt(cmd.command());
        cmd.Encode(bb);
        if (cmd.IsRequest)
        {
            if (!Pendings.TryAdd(cmd.ReliableSerialNo, new PendingPacket(socket, peer, bb)))
                throw new Exception("Duplicate ReliableSerialNo=" + cmd.ReliableSerialNo);

            // auto start Timer
            if (Timer == null)
            {
                lock (Pendings)
                {
                    // ReSharper disable once ConvertIfStatementToNullCoalescingAssignment
                    if (Timer == null)
                    {
                        Timer = Scheduler.Schedule(_ =>
                        {
                            var now = Time.NowUnixMillis;
                            foreach (var pending in Pendings.Values)
                            {
                                if (now - pending.SendTime > 1000)
                                {
                                    pending.SendTime = now;
                                    pending.Socket.Send(pending.Packet.Bytes.AsSpan(0, pending.Packet.WriteIndex), pending.Peer);
                                }
                            }
                        }, 1000, 1000);
                        // Zeze.Util.ShutdownHook.add(()->Timer.cancel(false));
                    }
                }
            }
        }

        socket.Send(bb.Bytes.AsSpan(0, bb.WriteIndex), peer);
    }

    public static Command ReceiveCommand(UdpClient socket)
    {
        var peer = new IPEndPoint(IPAddress.Any, 0);
        var buf = socket.Receive(ref peer);
        var bb = ByteBuffer.Wrap(buf);
        var c = bb.ReadInt();
        Command cmd = c switch
        {
            Register.Command => new Register(bb, peer),
            CommonResult.Command => new CommonResult(bb, peer),
            GlobalOn.Command => new GlobalOn(bb, peer),
            Release.Command => new Release(bb, peer),
            _ => throw new Exception("Unknown Command =" + c)
        };
        if (cmd.ReliableSerialNo != 0)
            Pendings.Remove(cmd.ReliableSerialNo, out _);
        return cmd;
    }

    sealed class Monitor
    {
        readonly IPEndPoint PeerSocketAddress;
        readonly AchillesHeelConfig?[] GlobalConfigs;
        readonly string FileName;
        readonly MemoryMappedFile MMap;
        readonly MemoryMappedViewAccessor MMapAccessor;
        readonly Thread thread;
        volatile bool Running = true;

        public Monitor(Register reg)
        {
            PeerSocketAddress = reg.Peer!;
            GlobalConfigs = new AchillesHeelConfig[reg.GlobalCount];
            FileName = reg.MMapFileName;
            MMap = MemoryMappedFile.CreateFromFile(FileName);
            MMapAccessor = MMap.CreateViewAccessor(0, reg.GlobalCount * 8);
            thread = new Thread(Run);
        }

        AchillesHeelConfig? GetConfig(int index)
        {
            return Volatile.Read(ref GlobalConfigs[index]);
        }

        internal void SetConfig(int index, AchillesHeelConfig config)
        {
            Volatile.Write(ref GlobalConfigs[index], config);
        }

        internal void Start()
        {
            thread.Start();
        }

        ByteBuffer CopyMMap()
        {
            var copy = new byte[GlobalConfigs.Length * 8];
            MMapAccessor.ReadArray(0, copy, 0, copy.Length);
            return ByteBuffer.Wrap(copy);
        }

        void Run()
        {
            try
            {
                while (Running)
                {
                    var bb = CopyMMap();
                    var now = Time.NowUnixMillis;
                    for (var i = 0; i < GlobalConfigs.Length; ++i)
                    {
                        var activeTime = bb.ReadLong8();
                        var config = GetConfig(i);
                        if (config == null)
                            continue; // skip not ready global

                        var idle = now - activeTime;
                        if (idle > config.ServerReleaseTimeout)
                        {
                            logger.Info($"destroySubprocess {now} - {activeTime} > {config.ServerReleaseTimeout}");
                            DestroySubprocess();
                            // daemon main will restart subprocess!
                        }
                        else if (idle > config.ServerDaemonTimeout)
                        {
                            logger.Info($"sendCommand Release-{i} {now} - {activeTime} > {config.ServerDaemonTimeout}");
                            // 在Server执行Release期间，命令可能重复发送。
                            // 重复命令的处理由Server完成，
                            // 这里重发也是需要的，刚好解决Udp不可靠性。
                            SendCommand(UdpSocket, PeerSocketAddress, new Release(i));
                        }

                        Thread.Sleep(1000);
                    }
                }
            }
            catch (Exception ex)
            {
                logger.Fatal(ex, "Monitor.run");
                FatalExit();
            }
        }

        public void StopAndJoin()
        {
            Running = false;
            thread.Join();

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

            try
            {
                File.Delete(FileName); // try delete
            }
            catch (Exception)
            {
                // ignored
            }
        }
    }

    public abstract class Command : Serializable
    {
        static long Seed;

        public IPEndPoint? Peer;
        public long ReliableSerialNo;
        public bool IsRequest { get; private set; }

        public abstract int command();

        protected void SetReliableSerialNo()
        {
            do
                ReliableSerialNo = Interlocked.Increment(ref Seed);
            while (ReliableSerialNo == 0);
            IsRequest = true;
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
            SetReliableSerialNo(); // enable reliable
        }

        public Register(ByteBuffer bb, IPEndPoint peer)
        {
            Peer = peer;
            MMapFileName = "";
            Decode(bb);
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
            SetReliableSerialNo(); // enable reliable
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
