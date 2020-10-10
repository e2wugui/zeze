
namespace Zeze.Transaction.Collections
{
    public abstract class PCollection : Bean // 简单起见就继承了，实际上容器可以不是Bean，只不过用到了一些Bean的属性。
    {
        public long LogKey { get; }

        protected PCollection(long logKey) : base((int)(logKey & Bean.MaxVariableId))
        {
            LogKey = logKey;
        }

        public override void Decode(global::Zeze.Serialize.ByteBuffer bb)
        {
            throw new System.NotImplementedException();
        }

        public override void Encode(global::Zeze.Serialize.ByteBuffer bb)
        {
            throw new System.NotImplementedException();
        }
    }
}
