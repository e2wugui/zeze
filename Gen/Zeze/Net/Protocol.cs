namespace Zeze.Net
{
    // Gen 工具只需要 confcs 运行时中 Protocol.MakeTypeId 的计算结果（写入生成代码的注释），
    // 这里保留同名同实现的方法，避免引入整个 confcs 网络层依赖。
    // 实现必须和 confcs/Zeze/Net/Protocol.cs 保持一致。
    public static class Protocol
    {
        public static long MakeTypeId(int moduleId, int protocolId)
        {
            return (long)moduleId << 32 | (uint)protocolId;
        }
    }
}
