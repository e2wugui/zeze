package Zeze.Services.GlobalCacheManager;

import Zeze.Util.BitConverter;

public class GlobalTableKey implements Comparable<GlobalTableKey>, Zeze.Serialize.Serializable {
    public String TableName;
    public byte[] Key;

    public GlobalTableKey() {
    }

    public GlobalTableKey(String tableName, Zeze.Serialize.ByteBuffer key) {
        this(tableName, key.Copy());
    }

    public GlobalTableKey(String tableName, byte[] key) {
        TableName = tableName;
        Key = key;
    }

    @Override
    public int compareTo(GlobalTableKey other) {
        int c = this.TableName.compareTo(other.TableName);
        if (c != 0)
            return c;

        return Zeze.Serialize.ByteBuffer.Compare(Key, other.Key);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj instanceof GlobalTableKey) {
            var another = (GlobalTableKey)obj;
            return TableName.equals(another.TableName) && Zeze.Serialize.ByteBuffer.Equals(Key, another.Key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + Zeze.Serialize.ByteBuffer.calc_hashnr(TableName);
        result = prime * result + Zeze.Serialize.ByteBuffer.calc_hashnr(Key, 0, Key.length);
        return result;
    }

    @Override
    public String toString() {
        return TableName + ":" + BitConverter.toString(Key, 0, Key.length);
    }

    public void Decode(Zeze.Serialize.ByteBuffer bb) {
        TableName = bb.ReadString();
        Key = bb.ReadBytes();
    }

    public void Encode(Zeze.Serialize.ByteBuffer bb) {
        bb.WriteString(TableName);
        bb.WriteBytes(Key);
    }
}
