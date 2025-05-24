using System;
using System.Threading;
using System.Threading.Tasks;
using demo.Module1;
using demo.Module1.Module11;
using Zeze;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services;

public class ModuleModule1 : AbstractModuleModule1
{
    protected override Task<long> ProcessProtocol1(Protocol p)
    {
        return Task.FromResult<long>(0);
    }

    protected override Task<long> ProcessProtocol3(Protocol p)
    {
        throw new NotImplementedException();
    }

    protected override Task<long> ProcessRpc2Request(Protocol p)
    {
        throw new NotImplementedException();
    }
}

public class ModuleModule11 : AbstractModuleModule11
{
}

public class TestClient : HandshakeClient
{
    private readonly ModuleModule1 module1 = new ModuleModule1();
    private readonly ModuleModule11 module11 = new ModuleModule11();

    public TestClient(string name, Config config) : base(name, config)
    {
        module1.Register(this);
        module11.Register(this);
    }

    public override void OnSocketConnected(AsyncSocket so)
    {
        Console.WriteLine("OnSocketConnected");
        base.OnSocketConnected(so);
    }

    public override void OnHandshakeDone(AsyncSocket sender)
    {
        Console.WriteLine("OnHandshakeDone");
        base.OnHandshakeDone(sender);

        // 模拟发送Auth协议
        var bb = ByteBuffer.Allocate();
        bb.WriteInt4(10000); // moduleId
        bb.WriteInt4(-997899722); // protocolId
        bb.BeginWriteWithSize4(out int saveSize); // pSize
        bb.WriteUInt(FamilyClass.Request); // rpc request
        bb.WriteLong(1); // rpc sessionId
        bb.WriteTag(0, 1, ByteBuffer.BYTES); // tag: lastVarId=0, varId=1, type=string
        bb.WriteString("ZezeTest"); // account
        bb.WriteByte(0); // bean end
        bb.EndWriteWithSize4(saveSize);
        bool r = sender.Send(bb.Bytes, bb.ReadIndex, bb.Size);
        Console.WriteLine($"Send Auth: {r}");
    }

    public override void OnSocketConnectError(AsyncSocket so, Exception e)
    {
        Console.WriteLine("OnSocketConnectError");
        base.OnSocketConnectError(so, e);
    }

    public override void OnSocketClose(AsyncSocket so, Exception e)
    {
        Console.WriteLine("OnSocketClose");
        base.OnSocketClose(so, e);
    }

    public override void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
    {
        Console.WriteLine($"DispatchUnknownProtocol: moduleId={moduleId}, protocolId={protocolId}, data=[{data.Size}]");
        if (moduleId == 10000 && protocolId == -997899722) // Auth reply
        {
            // 模拟发送SaveAccountSdkInfo协议
            var bb = ByteBuffer.Allocate();
            bb.WriteInt4(1); // moduleId
            bb.WriteInt4(2095710350); // protocolId
            bb.BeginWriteWithSize4(out int saveSize); // pSize
            bb.WriteUInt(FamilyClass.Protocol); // protocol
            bb.WriteByte(0); // bean end
            bb.EndWriteWithSize4(saveSize);
            bool r = so.Send(bb.Bytes, bb.ReadIndex, bb.Size);
            Console.WriteLine($"Send SaveAccountSdkInfo: {r}");
            // 模拟发送Login协议
            bb.Reset();
            bb.WriteInt4(11013); // moduleId
            bb.WriteInt4(-789575265); // protocolId
            bb.BeginWriteWithSize4(out saveSize); // pSize
            bb.WriteUInt(FamilyClass.Request); // rpc request
            bb.WriteLong(2); // rpc sessionId
            bb.WriteTag(0, 1, ByteBuffer.INTEGER); // tag: lastVarId=0, varId=1, type=long
            bb.WriteLong(321); // RoleId
            bb.WriteTag(1, 2, ByteBuffer.BYTES); // tag: lastVarId=1, varId=2, type=string
            bb.WriteString("vwvtuber"); // OnlineSetName
            bb.WriteByte(0); // bean end
            bb.EndWriteWithSize4(saveSize);
            r = so.Send(bb.Bytes, bb.ReadIndex, bb.Size);
            Console.WriteLine($"Send Login: {r}");
        }
    }

    public static void Main(string[] args)
    {
        var client = new TestClient("TestClient", new Config());
        client.Connect("127.0.0.1", 11000);
        Thread.Sleep(999_999_999);
    }
}
