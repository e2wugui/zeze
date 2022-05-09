using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Transaction;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using System.Runtime.InteropServices;
using Zeze.Net;
using Zeze.Transaction.Collections;

namespace UnitTest.Zeze.Trans
{
    [TestClass]
    public class TestChangeListener
    {
        [TestInitialize]
        public void TestInit()
        {
            demo.App.Instance.Start();
        }

        [TestCleanup]
        public void TestCleanup()
        {
            demo.App.Instance.Stop();
        }

        private void Prepare()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    await demo.App.Instance.demo_Module1.Table1.RemoveAsync(1);
                    return Procedure.Success;
                },
                "TestChangeListener.Remove").CallSynchronously());

            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    value.Int1 = 123;
                    value.Long2 = 123;
                    value.String3 = "123";
                    value.Bool4 = true;
                    value.Short5 = 123;
                    value.Float6 = 123.0f;
                    value.Double7 = 123.0;
                    value.Bytes8 = Binary.Empty;
                    value.List9.Add(new demo.Bean1() { V1 = 1 }); value.List9.Add(new demo.Bean1() { V1 = 2 });
                    value.Set10.Add(123); value.Set10.Add(124);
                    value.Map11.Add(1, new demo.Module2.Value()); value.Map11.Add(2, new demo.Module2.Value());
                    value.Bean12.Int1 = 123;
                    value.Byte13 = 12;
                    value.Dynamic14_demo_Module1_Simple = new demo.Module1.Simple() { Int1 = 123 };
                    value.Map15.Add(1, 1); value.Map15.Add(2, 2);
                    return Procedure.Success;
                },
                "TestChangeListener.Prepare").CallSynchronously());
        }

        [TestMethod]
        public void TestAllType()
        {
            Prepare();
            AddListener();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    value.Int1 = 124;
                    value.Long2 = 124;
                    value.String3 = "124";
                    value.Bool4 = false;
                    value.Short5 = 124;
                    value.Float6 = 124.0f;
                    value.Double7 = 124.0;
                    value.Bytes8 = new Binary(new byte[4]);
                    value.List9.Add(new demo.Bean1() { V1 = 2 }); value.List9.Add(new demo.Bean1() { V1 = 3 });
                    value.Set10.Add(125); value.Set10.Add(126);
                    value.Map11.Add(3, new demo.Module2.Value()); value.Map11.Add(4, new demo.Module2.Value());
                    value.Bean12.Int1 = 124;
                    value.Byte13 = 13;
                    value.Dynamic14_demo_Module1_Simple = new demo.Module1.Simple() { Int1 = 124 };
                    value.Map15.Add(3, 3); value.Map15.Add(4, 4);
                    return Procedure.Success;
                },
                "TestChangeListener.Modify").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    value.Set10.Add(127); value.Set10.Remove(124);
                    value.Map11.Add(5, new demo.Module2.Value()); value.Map11.Add(6, new demo.Module2.Value());
                    value.Map11.Remove(1); value.Map11.Remove(2);
                    value.Map15.Add(5, 5); value.Map15.Add(6, 6);
                    value.Map15.Remove(1); value.Map15.Remove(2);
                    return Procedure.Success;
                },
                "TestChangeListener.ModifyCollections").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    List<int> except = new List<int>
                    {
                        1,
                        2
                    };
                    value.Set10.ExceptWith(except);
                    return Procedure.Success;
                },
                "TestChangeListener.ModifySetExcept").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    List<int> intersect = new List<int>
                    {
                        123,
                        126
                    };
                    value.Set10.IntersectWith(intersect);
                    return Procedure.Success;
                },
                "TestChangeListener.ModifySetIntersect").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    List<int> SymmetricExcept = new List<int>
                    {
                        123,
                        140
                    };
                    value.Set10.SymmetricExceptWith(SymmetricExcept);
                    return Procedure.Success;
                },
                "TestChangeListener.ModifySetSymmetricExcept").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetOrAddAsync(1);
                    List<int> Union = new List<int>
                    {
                        123,
                        140
                    };
                    value.Set10.UnionWith(Union);
                    return Procedure.Success;
                },
                "TestChangeListener.ModifySetUnion").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    await demo.App.Instance.demo_Module1.Table1.PutAsync(1, new demo.Module1.Value());
                    return Procedure.Success;
                },
                "TestChangeListener.PutRecord").CallSynchronously());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    await demo.App.Instance.demo_Module1.Table1.RemoveAsync(1);
                    return Procedure.Success;
                },
                "TestChangeListener.RemoveRecord").CallSynchronously());
            Verify();
        }

        private demo.Module1.Value localValue;

        private void Init()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);
                    localValue = value?.Copy();
                    return Procedure.Success;
                },
                "TestChangeListener.CopyLocal").CallSynchronously());


            _CLInt1.Init(localValue);
            _ClLong2.Init(localValue);
            _CLString3.Init(localValue);
            _CLBool4.Init(localValue);
            _CLShort5.Init(localValue);
            _CLFloat6.Init(localValue);
            _CLDouble7.Init(localValue);
            _CLBytes8.Init(localValue);
            _CLList9.Init(localValue);
            _CLSet10.Init(localValue);
            _CLMap11.Init(localValue);
            _CLBean12.Init(localValue);
            _CLByte13.Init(localValue);
            _ClDynamic14.Init(localValue);
            _CLMap15.Init(localValue);
        }

        private void Verify()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(
                async () =>
                {
                    demo.Module1.Value value = await demo.App.Instance.demo_Module1.Table1.GetAsync(1);
                    localValue = value?.Copy();
                    return Procedure.Success;
                },
                "TestChangeListener.CopyLocal").CallSynchronously());

            _CLInt1.Verify(localValue);
            _ClLong2.Verify(localValue);
            _CLString3.Verify(localValue);
            _CLBool4.Verify(localValue);
            _CLShort5.Verify(localValue);
            _CLFloat6.Verify(localValue);
            _CLDouble7.Verify(localValue);
            _CLBytes8.Verify(localValue);
            _CLList9.Verify(localValue);
            _CLSet10.Verify(localValue);
            _CLMap11.Verify(localValue);
            _CLBean12.Verify(localValue);
            _CLByte13.Verify(localValue);
            _ClDynamic14.Verify(localValue);
            _CLMap15.Verify(localValue);
        }

        private void AddListener()
        {
            var l = new VarListeners();

            l.Vars.Add(demo.Module1.Table1.VAR_int1, _CLInt1);
            l.Vars.Add(demo.Module1.Table1.VAR_long2, _ClLong2);
            l.Vars.Add(demo.Module1.Table1.VAR_string3, _CLString3);
            l.Vars.Add(demo.Module1.Table1.VAR_bool4, _CLBool4);
            l.Vars.Add(demo.Module1.Table1.VAR_short5, _CLShort5);
            l.Vars.Add(demo.Module1.Table1.VAR_float6, _CLFloat6);
            l.Vars.Add(demo.Module1.Table1.VAR_double7, _CLDouble7);
            l.Vars.Add(demo.Module1.Table1.VAR_bytes8, _CLBytes8);
            l.Vars.Add(demo.Module1.Table1.VAR_list9, _CLList9);
            l.Vars.Add(demo.Module1.Table1.VAR_set10, _CLSet10);
            l.Vars.Add(demo.Module1.Table1.VAR_map11, _CLMap11);
            l.Vars.Add(demo.Module1.Table1.VAR_bean12, _CLBean12);
            l.Vars.Add(demo.Module1.Table1.VAR_byte13, _CLByte13);
            l.Vars.Add(demo.Module1.Table1.VAR_dynamic14, _ClDynamic14);
            l.Vars.Add(demo.Module1.Table1.VAR_map15, _CLMap15);

            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(l);
        }

        private readonly CLInt1 _CLInt1 = new ();
        private readonly ClLong2 _ClLong2 = new ();
        private readonly CLString3 _CLString3 = new ();
        private readonly CLBool4 _CLBool4 = new ();
        private readonly CLShort5 _CLShort5 = new ();
        private readonly CLFloat6 _CLFloat6 = new ();
        private readonly CLDouble7 _CLDouble7 = new ();
        private readonly CLBytes8 _CLBytes8 = new ();
        private readonly CLList9 _CLList9 = new ();
        private readonly CLSet10 _CLSet10 = new ();
        private readonly CLMap11 _CLMap11 = new ();
        private readonly CLBean12 _CLBean12 = new ();
        private readonly CLByte13 _CLByte13 = new ();
        private readonly ClDynamic14 _ClDynamic14 = new ();
        private readonly CLMap15 _CLMap15 = new ();

        public interface VarListener
        {
            public void OnRemoved(object key);
            public void OnChanged(object key, Bean value);
            public void OnChanged(object key, Log change);
        }
        public class VarListeners : ChangeListener
        {
            public Dictionary<int, VarListener> Vars { get; } = new();

            public void OnChanged(object key, Changes.Record r)
            {
                // 这是为了兼容旧的测试代码，拼凑出来的类。
                switch (r.State)
                {
                    case Changes.Record.Remove:
                        foreach (var v in Vars)
                        {
                            v.Value.OnRemoved(key);
                        }
                        break;
                    case Changes.Record.Put:
                        foreach (var v in Vars)
                        {
                            v.Value.OnChanged(key, r.PutValue);
                        }
                        break;

                    case Changes.Record.Edit:
                        {
                            var logbean = r.GetLogBean();
                            foreach (var v in Vars)
                            {
                                if (logbean.Variables.TryGetValue(v.Key, out var log))
                                    v.Value.OnChanged(key, log);
                            }
                        }
                        break;
                }

            }
        }

        class CLMap15 : VarListener
        {
            private Dictionary<long, long> newValue;

            public void Init(demo.Module1.Value current)
            {
                if (null != current)
                {
                    newValue = new Dictionary<long, long>();
                    foreach (var e in current.Map15)
                        newValue.Add(e.Key, e.Value);
                }
                else
                    newValue = null;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Dictionary<long, long> newValueCopy = new Dictionary<long, long>();
                foreach (var e in ((demo.Module1.Value)current).Map15)
                    newValueCopy.Add(e.Key, e.Value);
                Assert.AreEqual(newValue.Count, newValueCopy.Count);
                foreach (var e in newValue)
                {
                    Assert.IsTrue(newValueCopy.TryGetValue(e.Key, out var exist));
                    Assert.IsTrue(e.Value == exist);
                }
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = new Dictionary<long, long>();
                foreach (var e in ((demo.Module1.Value)value).Map15)
                    newValue.Add(e.Key, e.Value);
            }
            public void OnChanged(object key, Log note)
            {
                var notemap1 = (LogMap1<long, long>)note;

                foreach (var a in notemap1.Replaced)
                    newValue[a.Key] = a.Value;
                foreach (var r in notemap1.Removed)
                    newValue.Remove(r);
            }
        }

        class ClDynamic14 : VarListener
        {
            private Bean newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = current?.Dynamic14.CopyBean();
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Assert.IsTrue(newValue.TypeId == current.Dynamic14.TypeId);
                if (newValue.TypeId == demo.Module1.Simple.TYPEID)
                {
                    demo.Module1.Simple newSimple = newValue as demo.Module1.Simple;
                    demo.Module1.Simple currentSimple = current.Dynamic14.Bean as demo.Module1.Simple;
                    Assert.IsTrue(null != newSimple);
                    Assert.IsTrue(null != currentSimple);
                    Assert.IsTrue(newSimple.Int1 == currentSimple.Int1);
                }
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Dynamic14;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Dynamic14;
            }
        }

        class CLByte13 : VarListener
        {
            private byte newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Byte13 : (byte)255;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(255 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Byte13);
            }

            public void OnRemoved(object key)
            {
                newValue = 255;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Byte13;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Byte13;
            }
        }

        class CLBean12 : VarListener
        {
            private demo.Module1.Simple newValue;

            public void Init(demo.Module1.Value current)
            {
                if (null != current)
                {
                    newValue = current.Bean12.Copy();
                }
                else
                    newValue = null;
            }
            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Assert.IsTrue(newValue.Int1 == current.Bean12.Int1);
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bean12.Copy();
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Bean12.Copy();
            }
        }

        class CLMap11 : VarListener
        {
            private Dictionary<long, demo.Module2.Value> newValue;

            public void Init(demo.Module1.Value current)
            {
                if (null != current)
                {
                    newValue = new Dictionary<long, demo.Module2.Value>();
                    foreach (var e in current.Map11)
                        newValue.Add(e.Key, e.Value.Copy());
                }
                else
                    newValue = null;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Dictionary<long, demo.Module2.Value> newValueCopy = new Dictionary<long, demo.Module2.Value>();
                foreach (var e in ((demo.Module1.Value)current).Map11)
                    newValueCopy.Add(e.Key, e.Value.Copy());
                Assert.AreEqual(newValue.Count, newValueCopy.Count);
                foreach (var e in newValue)
                {
                    Assert.IsTrue(newValueCopy.TryGetValue(e.Key, out var exist));
                    Assert.IsTrue(e.Value.S == exist.S);
                }
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = new Dictionary<long, demo.Module2.Value>();
                foreach (var e in ((demo.Module1.Value)value).Map11)
                    newValue.Add(e.Key, e.Value.Copy());
            }
            public void OnChanged(object key, Log note)
            {
                var notemap2 = (LogMap2<long, demo.Module2.Value>)note;
                notemap2.MergeChangedToReplaced();

                foreach (var a in notemap2.Replaced)
                    newValue[a.Key] = a.Value;
                foreach (var r in notemap2.Removed)
                    newValue.Remove(r);
            }
        }

        class CLSet10 : VarListener
        {
            private HashSet<int> newValue;

            public void Init(demo.Module1.Value current)
            {
                if (null != current)
                {
                    newValue = new HashSet<int>();
                    foreach (var i in current.Set10)
                        newValue.Add(i);
                }
                else
                    newValue = null;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                HashSet<int> newValueCopy = new HashSet<int>();
                foreach (var i in ((demo.Module1.Value)current).Set10)
                    newValueCopy.Add(i);
                Assert.AreEqual(newValue.Count, newValueCopy.Count);
                foreach (var i in newValue)
                {
                    Assert.IsTrue(newValueCopy.Contains(i));
                }
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = new HashSet<int>();
                foreach (var i in ((demo.Module1.Value)value).Set10)
                    newValue.Add(i);
            }
            public void OnChanged(object key, Log note)
            {
                var noteset = (LogSet1<int>)note;
                foreach (var a in noteset.Added)
                    newValue.Add(a);
                foreach (var r in noteset.Removed)
                    newValue.Remove(r);
            }
        }

        class CLList9 : VarListener
        {
            private List<demo.Bean1> newValue;

            public void Init(demo.Module1.Value current)
            {
                if (null != current)
                {
                    newValue = new List<demo.Bean1>();
                    foreach (var e in current.List9)
                        newValue.Add(e.Copy());
                }
                else
                    newValue =  null;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Assert.AreEqual(newValue.Count, current.List9.Count);
                for (int i = 0; i < newValue.Count; ++i)
                {
                    Assert.IsTrue(newValue[i].V1 == current.List9[i].V1);
                }
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = new List<demo.Bean1>();
                foreach (var e in ((demo.Module1.Value)value).List9)
                    newValue.Add(e.Copy());
            }
            public void OnChanged(object key, Log note)
            {
                newValue = new List<demo.Bean1>();
                foreach (var e in ((demo.Module1.Value)note.Belong).List9)
                    newValue.Add(e.Copy());
            }
        }

        class CLBytes8 : VarListener
        {
            private Binary newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = current?.Bytes8;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Bytes8);
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bytes8;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Bytes8;
            }
        }

        class CLDouble7 : VarListener
        {
            private double newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Double7 : 0;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(0 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Double7);
            }

            public void OnRemoved(object key)
            {
                newValue = 0;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Double7;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Double7;
            }
        }

        class CLFloat6 : VarListener
        {
            private float newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Float6 : 0;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(0 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Float6);
            }

            public void OnRemoved(object key)
            {
                newValue = 0;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Float6;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Float6;
            }
        }

        class CLShort5 : VarListener
        {
            private short newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Short5 : (short)-1;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(-1 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Short5);
            }

            public void OnRemoved(object key)
            {
                newValue = -1;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Short5;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Short5;
            }
        }

        class CLBool4 : VarListener
        {
            private bool newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) && current.Bool4;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(false == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Bool4);
            }

            public void OnRemoved(object key)
            {
                newValue = false;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bool4;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Bool4;
            }

        }

        class CLString3 : VarListener
        {
            private string newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = current?.String3;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(null == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.String3);
            }

            public void OnRemoved(object key)
            {
                newValue = null;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).String3;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).String3;
            }
        }

        class ClLong2 : VarListener
        {
            private long newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Long2 : -1;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(-1 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Long2);
            }

            public void OnRemoved(object key)
            {
                newValue = -1;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Long2;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Long2;
            }
        }


        class CLInt1 : VarListener
        {
            private int newValue;

            public void Init(demo.Module1.Value current)
            {
                newValue = (null != current) ? current.Int1 : -1;
            }

            public void Verify(demo.Module1.Value current)
            {
                if (null == current)
                {
                    Assert.IsTrue(-1 == newValue);
                    return;
                }
                Assert.AreEqual(newValue, current.Int1);
            }

            public void OnRemoved(object key)
            {
                newValue = -1;
            }
            public void OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Int1;
            }
            public void OnChanged(object key, Log note)
            {
                newValue = ((demo.Module1.Value)note.Belong).Int1;
            }
        }
    }
}
