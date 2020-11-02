using System;
using System.Collections.Generic;
using System.Globalization;
using System.Net;
using System.Numerics;
using System.Reflection.Metadata;
using System.Security.Cryptography;
using System.Text;
using NLog;
using Org.BouncyCastle.Asn1.Ntt;
using Org.BouncyCastle.Utilities.Net;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using System.Collections.Concurrent;

/// <summary>
/// 使用dh算法交换密匙把连接加密。
/// </summary>

namespace Zeze.Services
{
    /// <summary>
    /// 服务器客户端定义在一起
    /// </summary>
    public class HandshakeOptions
    {
        // for HandshakeServer
        public HashSet<int> DhGroups { get; set; } = new HashSet<int>();
        public byte[] SecureIp { get; set; } = null;
        public bool S2cNeedCompress { get; set; } = true;
        public bool C2sNeedCompress { get; set; } = true;

        // for HandshakeClient
        public byte DhGroup { get; set; } = 1;

        public HandshakeOptions()
        {
            AddDhGroup(1);
            AddDhGroup(2);
            AddDhGroup(5);
        }

        public void AddDhGroup(int group)
        {
            if (Handshake.Helper.isDHGroupSupported(group))
                DhGroups.Add(group);
        }
    }

    public class HandshakeServer : Service
    {
        public HandshakeServer(string name, Application zeze) : base(name, zeze)
        {
            AddFactoryHandle(new Handshake.CHandshake().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new Handshake.CHandshake(),
                HandleRequest = ProcessCHandshake,
                NoProcedure = true,
            });
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            _asocketMap.TryAdd(so.SessionId, so);
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            _asocketMap.TryAdd(so.SessionId, so);
        }

        private int ProcessCHandshake(Protocol _p)
        {
            Handshake.CHandshake p = (Handshake.CHandshake)_p;
            int group = p.Argument.dh_group;
            if (false == Config.HandshakeOptions.DhGroups.Contains(group))
            {
                p.Sender.Close(new Exception("dhGroup Not Supported"));
                return 0;
            }
            Array.Reverse(p.Argument.dh_data);
            BigInteger data = new BigInteger(p.Argument.dh_data);
            BigInteger rand = Handshake.Helper.makeDHRandom();
            byte[] material = Handshake.Helper.computeDHKey(group, data, rand).ToByteArray();
            Array.Reverse(material);
            byte[] key = Config.HandshakeOptions.SecureIp != null ? Config.HandshakeOptions.SecureIp
                : ((IPEndPoint)p.Sender.Socket.LocalEndPoint).Address.GetAddressBytes();
            int half = material.Length / 2;
            byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
            p.Sender.SetInputSecurityCodec(hmacMd5, Config.HandshakeOptions.C2sNeedCompress);
            byte[] response = Handshake.Helper.generateDHResponse(group, rand).ToByteArray();
            Array.Reverse(response);
            new Handshake.SHandshake(response, Config.HandshakeOptions.S2cNeedCompress, Config.HandshakeOptions.C2sNeedCompress).Send(p.Sender);
            hmacMd5 = Digest.HmacMd5(key, material, half, material.Length - half);
            p.Sender.SetOutputSecurityCodec(hmacMd5, Config.HandshakeOptions.S2cNeedCompress);

            OnHandshakeDone(p.Sender);

            return 0;
        }
    }

    public class HandshakeClient : Service
    {
        public HandshakeClient(string name, Application zeze) : base(name, zeze)
        {
            AddFactoryHandle(new Handshake.SHandshake().TypeId, new Service.ProtocolFactoryHandle()
            {
                Factory = () => new Handshake.SHandshake(),
                HandleRequest = ProcessSHandshake,
                NoProcedure = true,
            });
        }

        private ConcurrentDictionary<long, BigInteger> DHContext = new ConcurrentDictionary<long, BigInteger>();

        public void Connect(string hostNameOrAddress, int port, bool autoReconnect = false)
        {
            Config.ServiceConf.Connector c = Config.GetOrAddConnector(hostNameOrAddress, port, autoReconnect);
            c.Socket?.Dispose();
            c.Socket = this.NewClientSocket(hostNameOrAddress, port);
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            _asocketMap.TryAdd(so.SessionId, so);
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            // 重载这个方法，推迟OnHandshakeDone调用
            _asocketMap.TryAdd(so.SessionId, so);
            Config.FindConnectorBySocket(so)?.OnSocketConnected(this);

            BigInteger dhRandom = Handshake.Helper.makeDHRandom();
            if (!DHContext.TryAdd(so.SessionId, dhRandom))
                throw new Exception("handshake duplicate context for same session.");
            byte[] response = Handshake.Helper.generateDHResponse(Config.HandshakeOptions.DhGroup, dhRandom).ToByteArray();
            Array.Reverse(response);
            new Handshake.CHandshake(Config.HandshakeOptions.DhGroup, response).Send(so);
        }

        private int ProcessSHandshake(Protocol _p)
        {
            Handshake.SHandshake p = (Handshake.SHandshake)_p;
            if (DHContext.TryGetValue(p.Sender.SessionId, out var dhRandom))
            {
                Array.Reverse(p.Argument.dh_data);
                byte[] material = Handshake.Helper.computeDHKey(Config.HandshakeOptions.DhGroup, new BigInteger(p.Argument.dh_data), dhRandom).ToByteArray();
                Array.Reverse(material);
                byte[] key = ((IPEndPoint)p.Sender.Socket.RemoteEndPoint).Address.GetAddressBytes();
                int half = material.Length / 2;
                byte[] hmacMd5 = Digest.HmacMd5(key, material, 0, half);
                p.Sender.SetOutputSecurityCodec(hmacMd5, p.Argument.c2sneedcompress);
                hmacMd5 = Digest.HmacMd5(key, material, half, material.Length - half);
                p.Sender.SetInputSecurityCodec(hmacMd5, p.Argument.s2cneedcompress);

                DHContext.TryRemove(p.Sender.SessionId, out var _);
                OnHandshakeDone(p.Sender);
                return 0;
            }
            throw new Exception("handshake lost context.");
        }
    }
}

namespace Zeze.Services.Handshake
{
    public static class Helper
    {
        private static readonly BigInteger dh_g = new BigInteger(2);
        private static readonly BigInteger[] dh_group = new BigInteger[] {
            BigInteger.Zero,
            BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A63A3620FFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group1, rfc2049 768
			BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381FFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group2, rfc2049 1024
			BigInteger.Zero,
            BigInteger.Zero,
            BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group5 rfc3526 1536
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
                    NumberStyles.AllowHexSpecifier),// dh_group14, rfc3526 2048
			BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group15,rfc3526 3072
			BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C934063199FFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group16,rfc3526 4096
			BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C93402849236C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AACC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E6DCC4024FFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier),// dh_group17,rfc3526 6144
			BigInteger.Parse(
                    "0FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E208E24FA074E5AB3143DB5BFCE0FD108E4B82D120A92108011A723C12A787E6D788719A10BDBA5B2699C327186AF4E23C1A946834B6150BDA2583E9CA2AD44CE8DBBBC2DB04DE8EF92E8EFC141FBECAA6287C59474E6BC05D99B2964FA090C3A2233BA186515BE7ED1F612970CEE2D7AFB81BDD762170481CD0069127D5B05AA993B4EA988D8FDDC186FFB7DC90A6C08F4DF435C93402849236C3FAB4D27C7026C1D4DCB2602646DEC9751E763DBA37BDF8FF9406AD9E530EE5DB382F413001AEB06A53ED9027D831179727B0865A8918DA3EDBEBCF9B14ED44CE6CBACED4BB1BDB7F1447E6CC254B332051512BD7AF426FB8F401378CD2BF5983CA01C64B92ECF032EA15D1721D03F482D7CE6E74FEF6D55E702F46980C82B5A84031900B1C9E59E7C97FBEC7E8F323A97A7E36CC88BE0F1D45B7FF585AC54BD407B22B4154AACC8F6D7EBF48E1D814CC5ED20F8037E0A79715EEF29BE32806A1D58BB7C5DA76F550AA3D8A1FBFF0EB19CCB1A313D55CDA56C9EC2EF29632387FE8D76E3C0468043E8F663F4860EE12BF2D5B0B7474D6E694F91E6DBE115974A3926F12FEE5E438777CB6A932DF8CD8BEC4D073B931BA3BC832B68D9DD300741FA7BF8AFC47ED2576F6936BA424663AAB639C5AE4F5683423B4742BF1C978238F16CBE39D652DE3FDB8BEFC848AD922222E04A4037C0713EB57A81A23F0C73473FC646CEA306B4BCBC8862F8385DDFA9D4B7FA2C087E879683303ED5BDD3A062B3CF5B3A278A66D2A13F83F44F82DDF310EE074AB6A364597E899A0255DC164F31CC50846851DF9AB48195DED7EA1B1D510BD7EE74D73FAF36BC31ECFA268359046F4EB879F924009438B481C6CD7889A002ED5EE382BC9190DA6FC026E479558E4475677E9AA9E3050E2765694DFC81F56E880B96E7160C980DD98EDD3DFFFFFFFFFFFFFFFFF",
                    NumberStyles.AllowHexSpecifier) // dh_group18,rfc3526 8192
        };
        public static byte[] makeRandValues(int bytes)
        {
            byte[] v = new byte[bytes];
            using (RandomNumberGenerator random = RandomNumberGenerator.Create())
                random.GetNonZeroBytes(v);
            return v;
        }
        public static bool isDHGroupSupported(int group)
        {
            return group >= 0 && group < dh_group.Length && !dh_group[group].Equals(BigInteger.Zero);
        }
        public static BigInteger makeDHRandom()
        {
            byte[] r = makeRandValues(17);
            r[16] = 0;
            return new BigInteger(r);
        }
        public static BigInteger generateDHResponse(int group, BigInteger rand)
        {
            return BigInteger.ModPow(dh_g, rand, dh_group[group]);
        }
        public static BigInteger computeDHKey(int group, BigInteger response, BigInteger rand)
        {
            return BigInteger.ModPow(response, rand, dh_group[group]);
        }
    }

    public class CHandshakeArgument : Zeze.Transaction.Bean
    {
        public byte dh_group;
        public byte[] dh_data;

        public override void Decode(ByteBuffer bb)
        {
            dh_group = bb.ReadByte();
            dh_data = bb.ReadBytes();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteByte(dh_group);
            bb.WriteBytes(dh_data);
        }

        protected override void InitChildrenTableKey(TableKey root)
        {
        }
    }

    public class SHandshakeArgument : Zeze.Transaction.Bean
    {
        public byte[] dh_data;
        public bool s2cneedcompress;
        public bool c2sneedcompress;

        public override void Decode(ByteBuffer bb)
        {
            dh_data = bb.ReadBytes();
            s2cneedcompress = bb.ReadBool();
            c2sneedcompress = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBytes(dh_data);
            bb.WriteBool(s2cneedcompress);
            bb.WriteBool(c2sneedcompress);
        }

        protected override void InitChildrenTableKey(TableKey root)
        {
        }
    }

    public class CHandshake : Zeze.Net.Protocol<CHandshakeArgument>
    {
        public override int ModuleId => 0;
        public override int ProtocolId => 1;

        public CHandshake()
        {

        }

        public CHandshake(byte dh_group, byte[] dh_data)
        {
            Argument.dh_group = dh_group;
            Argument.dh_data = dh_data;
        }
    }

    public class SHandshake : Zeze.Net.Protocol<SHandshakeArgument>
    {
        public override int ModuleId => 0;
        public override int ProtocolId => 2;

        public SHandshake()
        {
        }

        public SHandshake(byte[] dh_data, bool s2cneedcompress, bool c2sneedcompress)
        {
            Argument.dh_data = dh_data;
            Argument.s2cneedcompress = s2cneedcompress;
            Argument.c2sneedcompress = c2sneedcompress;
        }
    }
}
