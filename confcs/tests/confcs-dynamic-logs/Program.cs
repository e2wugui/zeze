using System;
using System.Collections.Generic;
using System.IO;
using ConfCsDynamicLogs;
using ConfCsDynamicLogs.Demo;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Transaction.Collections;
using Zeze.Util;

FollowerApplyTables.RegisterLog();
var table = new Orders();
Apply(0);
Check(table.Value is DerivedOrder, "put derived order");
Apply(1);
Check(table.Value.Amounts.Count == 0 && table.Value.Quantities.Count == 1
    && table.Value.Quantities[0] is Quantity { Value: 42 }, "dynamic list add");
Apply(2);
Check(table.Value.DirectQuantity is Quantity { Value: 84 }, "dynamic field replacement");
Apply(3);
Check(table.Value.QuantityMap[3] is Quantity { Value: 21 }
    && table.Value.AmountSorted[5] is Amount { Value: 63 }, "dynamic map replacements");
Apply(4);
Check(table.Value.Quantities[0] is Quantity { Value: 43 }, "nested dynamic list edit");
Apply(5);
Check(table.Value.DirectQuantity == null, "empty dynamic field replacement");
var originalDetail = table.Value.Detail;
Apply(9);
Check(ReferenceEquals(table.Value.Detail, originalDetail) && originalDetail.Revision == 1
    && originalDetail.Payload is Quantity { Value: 64 }, "dynamic replacement inside LogOne child edit");
Apply(10);
Check(ReferenceEquals(table.Value.Detail, originalDetail)
    && originalDetail.Payload is Quantity { Value: 65 }, "dynamic value edit inside LogOne child edit");
Apply(6);
Check(!ReferenceEquals(table.Value.Detail, originalDetail) && table.Value.Detail.Revision == 1
    && table.Value.Detail.Payload is Quantity { Value: 64 }, "committed nested replacement");
Apply(7);
Check(table.Value.Detail.Payload is Quantity { Value: 65 }, "committed nested edit");
Apply(8);
Check(table.Value.Detail.Revision == 2 && table.Value.Detail.Payload is Quantity { Value: 90 },
    "committed whole LogOne replacement");
var mixed = ReadRoot(9);
mixed.Variables.Add(11, new Log<int> { VariableId = 11, Value = 12 });
table.Value.FollowerApply(mixed);
Check(table.Value.LocalRevision == 12 && table.Value.Detail.Payload is Quantity { Value: 64 },
    "derived and inherited fields in the same edit");

CheckWrappedEdits((wrapper, log) =>
{
    var list = new List<WrappedQuantity> { wrapper };
    var delta = new LogList2<WrappedQuantity>();
    delta.Changed.Add(log, new LogList2<WrappedQuantity>.OutInt { Value = 0 });
    CollApply.ApplyList2(list, delta);
    return list[0];
});
CheckWrappedEdits((wrapper, log) =>
{
    var map = new Dictionary<int, WrappedQuantity> { [1] = wrapper };
    var delta = new LogMap2<int, WrappedQuantity>();
    delta.ChangedWithKey.Add(1, log);
    CollApply.ApplyMap2(map, delta);
    return map[1];
});
CheckWrappedEdits((wrapper, log) =>
{
    var map = new SortedDictionary<int, WrappedQuantity> { [1] = wrapper };
    var delta = new LogSortedMap2<int, WrappedQuantity>();
    delta.ChangedWithKey.Add(1, log);
    CollApply.ApplyMap2(map, delta);
    return map[1];
});
var taggedPayload = ByteBuffer.Allocate();
taggedPayload.WriteBool(false);
taggedPayload.WriteBool(true);
taggedPayload.WriteByte(0);
var scalarChild = new LogBean();
scalarChild.Variables.Add(2, new Log<int> { VariableId = 2, Value = 3 });
scalarChild.Encode(taggedPayload);
var taggedLog = new LogOne<Detail>();
taggedLog.Decode(ByteBuffer.Wrap(taggedPayload.Copy()));
var encoded = ByteBuffer.Allocate();
taggedLog.Encode(encoded);
Check(encoded.Copy().AsSpan().SequenceEqual(taggedPayload.Copy()), "C# LogOne child tag round trip");
Console.WriteLine("Java Changes.Record -> C# Decode -> FollowerApply passed");

void CheckWrappedEdits(Func<WrappedQuantity, LogBean, WrappedQuantity> apply)
{
    var wrapper = new WrappedQuantity();
    var replacement = (LogBean)((LogOne<Detail>)ReadRoot(9).Variables[10]).LogBean.Variables[1];
    Check(ReferenceEquals(apply(wrapper, replacement), wrapper) && wrapper.Bean is Quantity { Value: 64 },
        "wrapped dynamic replacement");
    var edit = (LogBean)((LogOne<Detail>)ReadRoot(10).Variables[10]).LogBean.Variables[1];
    Check(ReferenceEquals(apply(wrapper, edit), wrapper) && wrapper.Bean is Quantity { Value: 65 },
        "wrapped dynamic edit");
}

LogBean ReadRoot(int index)
{
    var bb = ByteBuffer.Wrap(File.ReadAllBytes(Path.Combine(args[0], index + ".bin")));
    Check(bb.ReadString() == "orders", "change table");
    table.DecodeKey(bb.ReadByteBuffer());
    Check(bb.ReadUInt() == ChangesRecord.Edit && bb.ReadUInt() == 1, "single root edit");
    var root = new LogBean();
    root.Decode(bb);
    Check(bb.Size == 0, "unread root edit bytes");
    return root;
}

void Apply(int index)
{
    var bb = ByteBuffer.Wrap(File.ReadAllBytes(Path.Combine(args[0], index + ".bin")));
    ChangesRecord.FollowerApply(bb, name => name == "orders" ? table : throw new Exception(name));
    Check(bb.Size == 0, "unread bytes in change " + index);
}

static void Check(bool condition, string message)
{
    if (!condition)
        throw new Exception(message);
}

sealed class Orders : ChangesTable
{
    public DerivedOrder Value { get; private set; }

    public object DecodeKey(ByteBuffer bb)
    {
        var key = bb.ReadLong();
        if (key != 1 || bb.Size != 0)
            throw new Exception("invalid key");
        return key;
    }

    public ConfBean NewValueBean() => new DerivedOrder();
    public ConfBean Get(object key) => Value;
    public void Put(object key, ConfBean value) => Value = (DerivedOrder)value;
    public void Remove(object key) => Value = null;
}

sealed class WrappedQuantity : ConfDynamicBean
{
    public WrappedQuantity() : base(1, _ => 7, _ => new Quantity())
    {
    }
}
