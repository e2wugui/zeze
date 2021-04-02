using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Transaction;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Serialize;
using System.Runtime.InteropServices;
using Zeze.Net;

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
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.App.Instance.demo_Module1.Table1.Remove(1);
                return Procedure.Success;
            }, "TestChangeListener.Remove").Call());

            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
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
            }, "TestChangeListener.Prepare").Call());
        }

        [TestMethod]
        public void TestAllType()
        {
            Prepare();
            AddListener();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
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
            }, "TestChangeListener.Modify").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                value.Set10.Add(127); value.Set10.Remove(124);
                value.Map11.Add(5, new demo.Module2.Value()); value.Map11.Add(6, new demo.Module2.Value());
                value.Map11.Remove(1); value.Map11.Remove(2);
                value.Map15.Add(5, 5); value.Map15.Add(6, 6);
                value.Map15.Remove(1); value.Map15.Remove(2);
                return Procedure.Success;
            }, "TestChangeListener.ModifyCollections").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                List<int> except = new List<int>
                {
                    1,
                    2
                };
                value.Set10.ExceptWith(except);
                return Procedure.Success;
            }, "TestChangeListener.ModifySetExcept").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                List<int> intersect = new List<int>
                {
                    123,
                    126
                };
                value.Set10.IntersectWith(intersect);
                return Procedure.Success;
            }, "TestChangeListener.ModifySetIntersect").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                List<int> SymmetricExcept = new List<int>
                {
                    123,
                    140
                };
                value.Set10.SymmetricExceptWith(SymmetricExcept);
                return Procedure.Success;
            }, "TestChangeListener.ModifySetSymmetricExcept").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.GetOrAdd(1);
                List<int> Union = new List<int>
                {
                    123,
                    140
                };
                value.Set10.UnionWith(Union);
                return Procedure.Success;
            }, "TestChangeListener.ModifySetUnion").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.App.Instance.demo_Module1.Table1.Put(1, new demo.Module1.Value());
                return Procedure.Success;
            }, "TestChangeListener.PutRecord").Call());
            Verify();

            Init();
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.App.Instance.demo_Module1.Table1.Remove(1);
                return Procedure.Success;
            }, "TestChangeListener.RemoveRecord").Call());
            Verify();
        }

        private demo.Module1.Value localValue;

        private void Init()
        {
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.Get(1);
                localValue = value?.Copy();
                return Procedure.Success;
            }, "TestChangeListener.CopyLocal").Call());


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
            Assert.IsTrue(Procedure.Success == demo.App.Instance.Zeze.NewProcedure(() =>
            {
                demo.Module1.Value value = demo.App.Instance.demo_Module1.Table1.Get(1);
                localValue = value?.Copy();
                return Procedure.Success;
            }, "TestChangeListener.CopyLocal").Call());

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
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_int1, _CLInt1);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_long2, _ClLong2);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_string3, _CLString3);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_bool4, _CLBool4);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_short5, _CLShort5);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_float6, _CLFloat6);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_double7, _CLDouble7);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_bytes8, _CLBytes8);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_list9, _CLList9);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_set10, _CLSet10);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_map11, _CLMap11);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_bean12, _CLBean12);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_byte13, _CLByte13);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_dynamic14, _ClDynamic14);
            demo.App.Instance.demo_Module1.Table1.ChangeListenerMap.AddListener(demo.Module1.Table1.VAR_map15, _CLMap15);
        }

        private readonly CLInt1 _CLInt1 = new CLInt1();
        private readonly ClLong2 _ClLong2 = new ClLong2();
        private readonly CLString3 _CLString3 = new CLString3();
        private readonly CLBool4 _CLBool4 = new CLBool4();
        private readonly CLShort5 _CLShort5 = new CLShort5();
        private readonly CLFloat6 _CLFloat6 = new CLFloat6();
        private readonly CLDouble7 _CLDouble7 = new CLDouble7();
        private readonly CLBytes8 _CLBytes8 = new CLBytes8();
        private readonly CLList9 _CLList9 = new CLList9();
        private readonly CLSet10 _CLSet10 = new CLSet10();
        private readonly CLMap11 _CLMap11 = new CLMap11();
        private readonly CLBean12 _CLBean12 = new CLBean12();
        private readonly CLByte13 _CLByte13 = new CLByte13();
        private readonly ClDynamic14 _ClDynamic14 = new ClDynamic14();
        private readonly CLMap15 _CLMap15 = new CLMap15();

        class CLMap15 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = new Dictionary<long, long>();
                foreach (var e in ((demo.Module1.Value)value).Map15)
                    newValue.Add(e.Key, e.Value);
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                ChangeNoteMap1<long, long> notemap1 = (ChangeNoteMap1<long, long>)note;

                foreach (var a in notemap1.Replaced)
                    newValue[a.Key] = a.Value;
                foreach (var r in notemap1.Removed)
                    newValue.Remove(r);
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class ClDynamic14 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Dynamic14;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Dynamic14;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLByte13 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Byte13;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Byte13;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = 255;
            }
        }

        class CLBean12 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bean12.Copy();
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Bean12.Copy();
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLMap11 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = new Dictionary<long, demo.Module2.Value>();
                foreach (var e in ((demo.Module1.Value)value).Map11)
                    newValue.Add(e.Key, e.Value.Copy());
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                ChangeNoteMap2<long, demo.Module2.Value> notemap2 = (ChangeNoteMap2<long, demo.Module2.Value>)note;
                notemap2.MergeChangedToReplaced(((demo.Module1.Value)value).Map11);

                foreach (var a in notemap2.Replaced)
                    newValue[a.Key] = a.Value;
                foreach (var r in notemap2.Removed)
                    newValue.Remove(r);
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLSet10 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = new HashSet<int>();
                foreach (var i in ((demo.Module1.Value)value).Set10)
                    newValue.Add(i);
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                ChangeNoteSet<int> noteset = (ChangeNoteSet<int>)note;
                foreach (var a in noteset.Added)
                    newValue.Add(a);
                foreach (var r in noteset.Removed)
                    newValue.Remove(r);
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLList9 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = new List<demo.Bean1>();
                foreach (var e in ((demo.Module1.Value)value).List9)
                    newValue.Add(e.Copy());
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = new List<demo.Bean1>();
                foreach (var e in ((demo.Module1.Value)value).List9)
                    newValue.Add(e.Copy());
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLBytes8 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bytes8;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Bytes8;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class CLDouble7 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Double7;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Double7;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = 0;
            }
        }

        class CLFloat6 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Float6;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Float6;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = 0;
            }
        }

        class CLShort5 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Short5;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Short5;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = -1;
            }
        }

        class CLBool4 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Bool4;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Bool4;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = false;
            }

        }

        class CLString3 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).String3;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).String3;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = null;
            }
        }

        class ClLong2 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Long2;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Long2;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = -1;
            }
        }


        class CLInt1 : ChangeListener
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

            void ChangeListener.OnChanged(object key, Bean value)
            {
                newValue = ((demo.Module1.Value)value).Int1;
            }

            void ChangeListener.OnChanged(object key, Bean value, ChangeNote note)
            {
                newValue = ((demo.Module1.Value)value).Int1;
            }

            void ChangeListener.OnRemoved(object key)
            {
                newValue = -1;
            }
        }
    }
}
