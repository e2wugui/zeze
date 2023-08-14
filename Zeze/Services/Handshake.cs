using System;
using System.Collections.Generic;
using System.Globalization;
using System.Net;
using System.Numerics;
using System.Security.Cryptography;
using Zeze.Net;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Util;

// 使用dh算法交换密匙把连接加密。
// 如果dh交换失败，现在依赖加密压缩实现以及后面的协议解析的错误检查来发现。
// 有没有好的安全的dh交换失败的检测手段。
namespace Zeze.Services
{
    public class Constant
    {
        public const int eEncryptTypeDisable = 0;
        public const int eEncryptTypeAes = 1;

        public const int eCompressTypeDisable = 0;
        public const int eCompressTypeMppc = 1;
        public const int eCompressTypeZstd = 2;
    }

    /// <summary>
    /// 服务器客户端定义在一起
    /// </summary>
    public class HandshakeOptions
    {
        // for HandshakeServer
        // ReSharper disable once CollectionNeverQueried.Global
        public HashSet<int> DhGroups = new HashSet<int>();
        public byte[] SecureIp;
        public int CompressS2c = Constant.eCompressTypeDisable;
        public int CompressC2s = Constant.eCompressTypeDisable;

        // for HandshakeClient
        public int EncryptType = Constant.eEncryptTypeDisable;

        public readonly List<int> SupportedEncrypt = new List<int>();
        public readonly List<int> SupportedCompress = new List<int>();

        public HandshakeOptions()
        {
            AddDhGroup(1);
            AddDhGroup(2);
            AddDhGroup(5);

            AddSupportedCompress(Constant.eCompressTypeMppc);
            AddSupportedCompress(Constant.eCompressTypeZstd);

            AddSupportedEncrypt(Constant.eEncryptTypeAes);
        }

        public void AddSupportedCompress(int c)
        {
            SupportedCompress.Add(c);
        }

        public void AddSupportedEncrypt(int e)
        {
            SupportedEncrypt.Add(e);
        }

        public bool IsSupportedCompress(int c)
        {
            return SupportedCompress.Contains(c);
        }

        public bool IsSupportedEncrypt(int e)
        {
            return SupportedEncrypt.Contains(e);
        }

        public void AddDhGroup(int group)
        {
            if (Handshake.Helper.IsDHGroupSupported(group))
                DhGroups.Add(group);
        }
    }

    public class HandshakeBase : Service
    {
        private new static readonly ILogger logger = LogManager.GetLogger(typeof(HandshakeBase));

        private readonly HashSet<long> HandshakeProtocols = new HashSet<long>();

        class Context
        {
            public readonly BigInteger DhRandom;
            public SchedulerTask TimeoutTask;

            public Context(BigInteger rand)
            {
                DhRandom = rand;
            }
        }

        // For Client Only
        private readonly ConcurrentDictionary<long, Context> DHContext = new ConcurrentDictionary<long, Context>();

        public HandshakeBase(string name, Config config) : base(name, config)
        {
        }

        public HandshakeBase(string name, Application app) : base(name, app)
        {
        }

        public override bool IsHandshakeProtocol(long typeId)
        {
            return HandshakeProtocols.Contains(typeId);
        }

        protected void AddHandshakeServerFactoryHandle()
        {
            {
                var tmp = new Handshake.CHandshake();
                HandshakeProtocols.Add(tmp.TypeId);
                AddFactoryHandle(tmp.TypeId, new ProtocolFactoryHandle
                {
                    Factory = () => new Handshake.CHandshake(),
                    Handle = ProcessCHandshake,
                    TransactionLevel = Transaction.TransactionLevel.None
                });
            }
            {
                var tmp = new Handshake.CHandshakeDone();
                HandshakeProtocols.Add(tmp.TypeId);
                AddFactoryHandle(tmp.TypeId, new ProtocolFactoryHandle
                {
                    Factory = () => new Handshake.CHandshakeDone(),
                    Handle = ProcessCHandshakeDone,
                    TransactionLevel = Transaction.TransactionLevel.None
                });
            }
        }

        private Task<long> ProcessCHandshakeDone(Protocol p)
        {
            p.Sender.VerifySecurity();
            OnHandshakeDone(p.Sender);
            return Task.FromResult(0L);
        }

        private int ServerCompressS2c(int s2cHint)
        {
            var options = Config.HandshakeOptions;
            if (options.CompressS2c != 0)
            {
                if (s2cHint != Constant.eCompressTypeDisable && options.IsSupportedCompress(s2cHint))
                    return s2cHint;
                return Constant.eCompressTypeMppc;
            }
            if (s2cHint == 0)
                return 0;
            if (options.IsSupportedCompress(s2cHint))
                return s2cHint;
            return Constant.eCompressTypeMppc;
        }

        private int ServerCompressC2s(int c2sHint)
        {
            var options = Config.HandshakeOptions;
            if (options.CompressC2s != 0)
            {
                if (c2sHint != Constant.eCompressTypeDisable && options.IsSupportedCompress(c2sHint))
                    return c2sHint;
                return Constant.eCompressTypeMppc;
            }
            if (c2sHint == 0)
                return 0;
            if (options.IsSupportedCompress(c2sHint))
                return c2sHint;
            return Constant.eCompressTypeMppc;
        }

        private Task<long> ProcessCHandshake(Protocol _p)
        {
            try
            {
                byte[] inputKey = null;
                byte[] outputKey = null;
                byte[] response = Array.Empty<byte>();
                const int group = 1;

                var p = (Handshake.CHandshake)_p;
                if (p.Argument.EncryptType == Constant.eEncryptTypeAes)
                {
                    // 当group采用客户端参数时需要检查参数正确性，现在统一采用了1，不需要检查了。
                    /*
                    if (!Config.HandshakeOptions.DhGroups.Contains(group))
                    {
                        p.Sender.Close(new Exception("dhGroup Not Supported"));
                        return Task.FromResult(0L);
                    }
                    */
                    Array.Reverse(p.Argument.EncryptParam);
                    var data = new BigInteger(p.Argument.EncryptParam);
                    var rand = Handshake.Helper.MakeDHRandom();
                    byte[] material = Handshake.Helper.ComputeDHKey(group, data, rand).ToByteArray();
                    Array.Reverse(material);
                    IPAddress ipaddress = ((IPEndPoint)p.Sender.Socket.LocalEndPoint).Address;
                    // logger.Debug(ipaddress);
                    if (ipaddress.IsIPv4MappedToIPv6)
                        ipaddress = ipaddress.MapToIPv4();
                    byte[] key = Config.HandshakeOptions.SecureIp ?? ipaddress.GetAddressBytes();
                    logger.Debug("{0} localIp={1}", p.Sender.SessionId, BitConverter.ToString(key));
                    int half = material.Length / 2;
                    inputKey = Digest.HmacMd5(key, material, 0, half);
                    response = Handshake.Helper.GenerateDHResponse(group, rand).ToByteArray();
                    Array.Reverse(response);
                    outputKey = Digest.HmacMd5(key, material, half, material.Length - half);
                }
                var s2c = ServerCompressS2c(p.Argument.CompressS2c);
                var c2s = ServerCompressC2s(p.Argument.CompressC2s);
                p.Sender.SetInputSecurityCodec(inputKey, c2s);

                var sHandshake = new Handshake.SHandshake();
                sHandshake.Argument.EncryptParam = response;
                sHandshake.Argument.CompressS2c = s2c;
                sHandshake.Argument.CompressC2s = c2s;
                sHandshake.Argument.EncryptType = p.Argument.EncryptType;
                sHandshake.Send(p.Sender);
                p.Sender.SetOutputSecurityCodec(outputKey, s2c);

                // 为了防止服务器在Handshake以后马上发送数据，
                // 导致未加密数据和加密数据一起到达Client，这种情况很难处理。
                // 这个本质上是协议相关的问题：就是前面一个协议的处理结果影响后面数据处理。
                // 所以增加CHandshakeDone协议，在Client进入加密以后发送给Server。
                // OnHandshakeDone(p.Sender);

                return Task.FromResult(0L);
            }
            catch (Exception ex)
            {
                _p.Sender.Close(ex);
            }
            return Task.FromResult(0L);
        }

        protected void AddHandshakeClientFactoryHandle()
        {
            {
                var tmp = new Handshake.SHandshake();
                HandshakeProtocols.Add(tmp.TypeId);
                AddFactoryHandle(tmp.TypeId, new ProtocolFactoryHandle
                {
                    Factory = () => new Handshake.SHandshake(),
                    Handle = ProcessSHandshake,
                    TransactionLevel = Transaction.TransactionLevel.None
                });
            }
            {
                var tmp = new Handshake.SHandshake0();
                HandshakeProtocols.Add(tmp.TypeId);
                AddFactoryHandle(tmp.TypeId, new ProtocolFactoryHandle
                {
                    Factory = () => new Handshake.SHandshake0(),
                    Handle = ProcessSHandshake0,
                    TransactionLevel = Transaction.TransactionLevel.None
                });
            }
        }

        private Task<long> ProcessSHandshake0(Protocol _p)
        {
            try
            {
                var p = (Handshake.SHandshake0)_p;
                if (p.Argument.EncryptType != 0 || p.Argument.CompressS2c != 0 || p.Argument.CompressC2s != 0)
                {
                    StartHandshake(p.Argument, p.Sender);
                }
                else
                {
                    new Handshake.CHandshakeDone().Send(p.Sender);
                    OnHandshakeDone(p.Sender);
                }
            }
            catch (Exception ex)
            {
                _p.Sender.Close(ex);
            }
            return Task.FromResult(0L);
        }

        private Task<long> ProcessSHandshake(Protocol _p)
        {
            try
            {
                var p = (Handshake.SHandshake)_p;
                if (DHContext.TryRemove(p.Sender.SessionId, out var ctx))
                {
                    try
                    {
                        byte[] inputKey = null;
                        byte[] outputKey = null;
                        if (p.Argument.EncryptType == Constant.eEncryptTypeAes)
                        {
                            Array.Reverse(p.Argument.EncryptParam);
                            byte[] material = Handshake.Helper.ComputeDHKey(
                                1,
                                new BigInteger(p.Argument.EncryptParam),
                                ctx.DhRandom).ToByteArray();
                            Array.Reverse(material);
                            IPAddress ipaddress = ((IPEndPoint)p.Sender.Socket.RemoteEndPoint).Address;
                            if (ipaddress.IsIPv4MappedToIPv6) ipaddress = ipaddress.MapToIPv4();
                            byte[] key = ipaddress.GetAddressBytes();
                            logger.Debug("{0} remoteIp={1}", p.Sender.SessionId, BitConverter.ToString(key));
                            int half = material.Length / 2;
                            outputKey = Digest.HmacMd5(key, material, 0, half);
                            inputKey = Digest.HmacMd5(key, material, half, material.Length - half);
                        }

                        p.Sender.SetOutputSecurityCodec(outputKey, p.Argument.CompressC2s);
                        p.Sender.SetInputSecurityCodec(inputKey, p.Argument.CompressS2c);

                        new Handshake.CHandshakeDone().Send(p.Sender);
                        OnHandshakeDone(p.Sender);
                    }
                    finally
                    {
                        ctx.TimeoutTask?.Cancel();
                    }
                    return Task.FromResult(0L);
                }
                p.Sender.Close(new Exception("handshake lost context."));
            }
            catch (Exception ex)
            {
                _p.Sender.Close(ex);
            }
            return Task.FromResult(0L);
        }

        private int ClientCompress(int c)
        {
            // 客户端检查一下当前版本是否支持推荐的压缩算法。
            // 如果不支持则统一使用最老的。
            // 这样当服务器新增了压缩算法，并且推荐了新的，客户端可以兼容它。
            if (c == Constant.eCompressTypeDisable)
                return c; // 推荐关闭压缩就关闭
            var options = Config.HandshakeOptions;
            if (options.IsSupportedCompress(c))
                return c; // 支持的压缩，直接使用推荐的。
            return Constant.eCompressTypeMppc; // 使用最老的压缩。
        }

        protected void StartHandshake(Handshake.SHandshake0Argument arg, AsyncSocket so)
        {
            try
            {
                var ctx = new Context(Handshake.Helper.MakeDHRandom());
                if (!DHContext.TryAdd(so.SessionId, ctx))
                    throw new Exception("handshake duplicate context for same session.");
                var cHandshake = new Handshake.CHandshake();
                cHandshake.Argument.EncryptType = arg.EncryptType;
                if (arg.EncryptType == Constant.eEncryptTypeAes)
                {
                    byte[] response = Handshake.Helper.GenerateDHResponse(1, ctx.DhRandom).ToByteArray();
                    Array.Reverse(response);
                    cHandshake.Argument.EncryptParam = response;
                }
                else
                    cHandshake.Argument.EncryptParam = Array.Empty<byte>();
                cHandshake.Argument.CompressS2c = ClientCompress(arg.CompressS2c);
                cHandshake.Argument.CompressC2s = ClientCompress(arg.CompressC2s);
                cHandshake.Send(so);
                ctx.TimeoutTask = Scheduler.Schedule(thisTask =>
                {
                    if (DHContext.TryRemove(so.SessionId, out _))
                        so.Close(new Exception("Handshake Timeout"));
                }, 5000);
            }
            catch (Exception ex)
            {
                so.Close(ex);
            }
        }
    }

    public class HandshakeServer : HandshakeBase
    {
        public HandshakeServer(string name, Config config) : base(name, config)
        {
            AddHandshakeServerFactoryHandle();
        }

        public HandshakeServer(string name, Application app) : base(name, app)
        {
            AddHandshakeServerFactoryHandle();
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            SocketMap.TryAdd(so.SessionId, so);

            var hand0 = new Handshake.SHandshake0();
            var options = Config.HandshakeOptions;
            hand0.Argument.EncryptType = options.EncryptType;
            hand0.Argument.SupportedEncryptList = options.SupportedEncrypt;
            hand0.Argument.CompressS2c = options.CompressS2c;
            hand0.Argument.CompressC2s = options.CompressC2s;
            hand0.Argument.SupportedCompressList = options.SupportedCompress;
            hand0.Send(so);
        }
    }

    public class HandshakeClient : HandshakeBase
    {
        public HandshakeClient(string name, Config config) : base(name, config)
        {
            AddHandshakeClientFactoryHandle();
        }

        public HandshakeClient(string name, Application app) : base(name, app)
        {
            AddHandshakeClientFactoryHandle();
        }

        public void Connect(string hostNameOrAddress, int port, bool autoReconnect = true)
        {
            Config.TryGetOrAddConnector(hostNameOrAddress, port, autoReconnect, out var c);
            c.Start();
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            SocketMap.TryAdd(so.SessionId, so);
        }
    }

    public class HandshakeBoth : HandshakeBase
    {
        public HandshakeBoth(string name, Config config) : base(name, config)
        {
            AddHandshakeClientFactoryHandle();
            AddHandshakeServerFactoryHandle();
        }

        public HandshakeBoth(string name, Application app) : base(name, app)
        {
            AddHandshakeClientFactoryHandle();
            AddHandshakeServerFactoryHandle();
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            SocketMap.TryAdd(so.SessionId, so);

            var hand0 = new Handshake.SHandshake0();
            var options = Config.HandshakeOptions;
            hand0.Argument.EncryptType = options.EncryptType;
            hand0.Argument.SupportedEncryptList = options.SupportedEncrypt;
            hand0.Argument.CompressS2c = options.CompressS2c;
            hand0.Argument.CompressC2s = options.CompressC2s;
            hand0.Argument.SupportedCompressList = options.SupportedCompress;
            hand0.Send(so);
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            SocketMap.TryAdd(so.SessionId, so);
        }
    }
}

namespace Zeze.Services.Handshake
{
    public static class Helper
    {
        private static readonly BigInteger dh_g = new BigInteger(2);

        private static readonly BigInteger[] dh_group =
        {
            BigInteger.Zero,
            // ReSharper disable StringLiteralTypo
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group1, rfc2049 768
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group2, rfc2049 1024
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group5 rfc3526 1536
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group14, rfc3526 2048
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group15,rfc3526 3072
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934063199FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group16,rfc3526 4096
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C93402849236C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AACC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E6DCC4024FFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier), // dh_group17,rfc3526 6144
            BigInteger.Parse(
                "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C93402849236C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AACC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E6DBE115974A3926F12FEE5E438777CB6A932DF8CD8BEC4D073B931BA3BC832B68D9DD300741FA7BF8AFC47ED2576F6936BA424663AAB639C5AE4F5683423B4742BF1C978238F16CBE39D652DE3FDB8BEFC848AD922222E04A4037C0713EB57A81A23F0C73473FC646CEA306B4BCBC8862F8385DDFA9D4B7FA2C087E879683303ED5BDD3A062B3CF5B3A278A66D2A13F83F44F82DDF310EE074AB6A364597E899A0255DC164F31CC50846851DF9AB48195DED7EA1B1D510BD7EE74D73FAF36BC31ECFA268359046F4EB879F924009438B481C6CD7889A002ED5EE382BC9190DA6FC026E479558E4475677E9AA9E3050E2765694DFC81F56E880B96E7160C980DD98EDD3DFFFFFFFFFFFFFFFFF",
                NumberStyles.AllowHexSpecifier) // dh_group18,rfc3526 8192
            // ReSharper restore StringLiteralTypo
        };

        public static readonly RandomNumberGenerator RandomNumberGenerator = RandomNumberGenerator.Create();

        public static byte[] MakeRandValues(int bytes)
        {
            byte[] v = new byte[bytes];
            RandomNumberGenerator.GetNonZeroBytes(v);
            return v;
        }

        public static bool IsDHGroupSupported(int group)
        {
            return group >= 0 && group < dh_group.Length && !dh_group[group].Equals(BigInteger.Zero);
        }

        public static BigInteger MakeDHRandom()
        {
            byte[] r = MakeRandValues(17);
            r[16] = 0;
            return new BigInteger(r);
        }

        public static BigInteger GenerateDHResponse(int group, BigInteger rand)
        {
            return BigInteger.ModPow(dh_g, rand, dh_group[group]);
        }

        public static BigInteger ComputeDHKey(int group, BigInteger response, BigInteger rand)
        {
            return BigInteger.ModPow(response, rand, dh_group[group]);
        }
    }

#if USE_CONFCS
    public sealed class CHandshakeArgument : ConfBean
    {
        public override long TypeId => FixedHash.Hash64(typeof(CHandshakeArgument).FullName);
#else
    public sealed class CHandshakeArgument : Transaction.Bean
    {
#endif
        public int EncryptType;
        public byte[] EncryptParam;
        public int CompressS2c;
        public int CompressC2s;

        public override void Decode(ByteBuffer bb)
        {
            EncryptType = bb.ReadInt();
            EncryptParam = bb.ReadBytes();

            // 兼容旧版客户端
            if (bb.WriteIndex > bb.ReadIndex)
            {
                CompressS2c = bb.ReadInt();
                CompressC2s = bb.ReadInt();
            }
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteByte(EncryptType);
            bb.WriteBytes(EncryptParam);
            bb.WriteInt(CompressS2c);
            bb.WriteInt(CompressC2s);
        }

        public override void ClearParameters()
        {
            EncryptType = 0;
            EncryptParam = null;
            CompressS2c = 0;
            CompressC2s = 0;
        }
    }

#if USE_CONFCS
    public sealed class SHandshakeArgument : ConfBean
    {
        public override long TypeId => FixedHash.Hash64(typeof(SHandshakeArgument).FullName);
#else
    public sealed class SHandshakeArgument : Transaction.Bean
    {
#endif
        public byte[] EncryptParam;
        public int CompressS2c;
        public int CompressC2s;
        public int EncryptType;

        public override void ClearParameters()
        {
            EncryptParam = null;
            CompressS2c = 0;
            CompressC2s = 0;
            EncryptType = 0;
        }

        public override void Decode(ByteBuffer bb)
        {
            EncryptParam = bb.ReadBytes();
            CompressS2c = bb.ReadInt();
            CompressC2s = bb.ReadInt();
            EncryptType = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBytes(EncryptParam);
            bb.WriteInt(CompressS2c);
            bb.WriteInt(CompressC2s);
            bb.WriteInt(EncryptType);
        }
    }

    public sealed class CHandshake : Protocol<CHandshakeArgument>
    {
        public static readonly int ProtocolId_ = FixedHash.Hash32(typeof(CHandshake).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class SHandshake : Protocol<SHandshakeArgument>
    {
        public static readonly int ProtocolId_ = FixedHash.Hash32(typeof(SHandshake).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

#if USE_CONFCS
    public sealed class CHandshakeDone : Protocol<ConfEmptyBean>
#else
    public sealed class CHandshakeDone : Protocol<Transaction.EmptyBean>
#endif
    {
        public static readonly int ProtocolId_ = FixedHash.Hash32(typeof(CHandshakeDone).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

#if USE_CONFCS
    public sealed class SHandshake0Argument : ConfBean
    {
        public override long TypeId => FixedHash.Hash64(typeof(SHandshake0Argument).FullName);
#else
    public sealed class SHandshake0Argument : Transaction.Bean
    {
#endif
        public int EncryptType;
        public List<int> SupportedEncryptList = new List<int>();
        public int CompressS2c;
        public int CompressC2s;
        public List<int> SupportedCompressList = new List<int>();

        public override void ClearParameters()
        {
            EncryptType = 0;
            SupportedEncryptList.Clear();
            CompressS2c = 0;
            CompressC2s = 0;
            SupportedCompressList.Clear();
        }

        public override void Decode(ByteBuffer bb)
        {
            EncryptType = bb.ReadInt();
            for (int count = bb.ReadInt(); count > 0; count--)
                SupportedEncryptList.Add(bb.ReadInt());
            CompressS2c = bb.ReadInt();
            CompressC2s = bb.ReadInt();
            for (int count = bb.ReadInt(); count > 0; count--)
                SupportedCompressList.Add(bb.ReadInt());
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(EncryptType);
            bb.WriteInt(SupportedEncryptList.Count);
            foreach (var e in SupportedEncryptList)
                bb.WriteInt(e);
            bb.WriteInt(CompressS2c);
            bb.WriteInt(CompressC2s);
            bb.WriteInt(SupportedCompressList.Count);
            foreach (var c in SupportedCompressList)
                bb.WriteInt(c);
        }
    }

    public sealed class SHandshake0 : Protocol<SHandshake0Argument>
    {
        public static readonly int ProtocolId_ = FixedHash.Hash32(typeof(SHandshake0).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }
}
