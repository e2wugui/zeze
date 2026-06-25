namespace Zeze.Serialize
{
    public interface Serializable
    {
        void Decode(ByteBuffer bb);
        void Encode(ByteBuffer bb);
    }
}
