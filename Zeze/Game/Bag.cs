
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Zeze.Arch;
using Zeze.Builtin.Game.Bag;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Game
{
    public class Bag
    {
        // ������ƷId��ѯ��Ʒ�ѵ����������������������ȫ��max==1�������ܶѵ���
        public static volatile Func<int, int> FuncItemPileMax;

        // ��Ʒ�������ʱ���Զ�ע�᣻
        // ע���Bean.ClassName�ᱻ�־û�����������
        // Module.Start��ʱ���Զ�װ��ע���ClassName��
        private readonly static Zeze.Collections.BeanFactory BeanFactory = new();

        public static long GetSpecialTypeIdFromBean(Bean bean)
        {
            return bean.TypeId;
        }

        public static Bean CreateBeanFromSpecialTypeId(long typeId)
        {
            return BeanFactory.Create(typeId);
        }

        private Module module;

        Bag(Module module, string bagName)
        {
            this.module = module;
            this.name = bagName;
        }

        async Task<Bag> OpenAsync()
        {
            if (null == bean) // ���ϸ���̱߳����������ظ�GetOrAdd��
                bean = await module._tBag.GetOrAddAsync(name);
            return this;
        }

        private readonly string name;
        private BBag bean;

        public string Name => name;
        public BBag Bean => bean;
        public int Capacity { get { return bean.Capacity; } set { bean.Capacity = value; } }

        /// <summary>
        /// ɾ��number������ָ��id��Ʒ��
        /// warning: �������false����ʾ��Ʒ��������ʱӦ�ûع����񣬷���Ჿ��ɾ����
        /// �����߼�����ɾ����Ʒ����Ϊ��ʹ�ã����������ʹ��ʧ�ܣ��ع������ǱȽϺ���ġ�
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        /// <returns></returns>
        public bool Remove(int id, int number)
        {
            if (number <= 0)
                throw new ArgumentException();

            foreach (var item in bean.Items)
            {
                if (item.Value.Id == id)
                {
                    if (item.Value.Number > number)
                    {
                        item.Value.Number -= number;
                        return true;
                    }
                    number -= item.Value.Number;
                    bean.Items.Remove(item.Key);
                    if (number <= 0)
                        return true;
                }
            }
            return false;
        }

        /// <summary>
        /// ����ɾ�� positionHint ָ���ĸ��ӵ���Ʒ��
        /// ��Ϸ��ĳ���������Ҽ�ʹ����Ʒʱ�����û��ָ�����ӵ���Ϣ���ͻ�����ɾ��ǰ����ӵ���Ʒ��������һ��㲻���Ѻá�
        /// </summary>
        /// <param name="positionHint"></param>
        /// <param name="id"></param>
        /// <param name="number"></param>
        /// <returns></returns>
        public bool Remove(int positionHint, int id, int number)
        {
            if (number <= 0)
                throw new ArgumentException();

            if (bean.Items.TryGetValue(positionHint, out var bItem))
            {
                if (id != bItem.Id)
                    return Remove(id, number);

                if (bItem.Number > number)
                {
                    bItem.Number -= number;
                    return true;
                }
                number -= bItem.Number;
                bean.Items.Remove(positionHint);
                if (number <= 0)
                    return true;
            }
            return Remove(id, number);
        }

        /// <summary>
        /// �������Ʒ��ֻ��id��number
        /// </summary>
        /// <param name="id"></param>
        /// <param name="number"></param>
        public async Task<int> AddAsync(int id, int number)
        {
            return await AddAsync(-1, new BItem() { Id = id, Number = number });
        }

        /// <summary>
        /// ������Ʒ�����ȶѵ������еĸ������棻Ȼ������ܶ࣬�Զ���֡�
        /// ʧ�ܴ�����������������ʧ��ʱ�ع�������ô���е���Ӷ��ᱻ�ع���
        ///           ���û�лع�����ô�ͻ���ɲ�����ӡ���ʱ����ʣ��number���߼�������Ҫ��ʣ����������Ʒת������ϵͳ�������ʼ��У���
        ///           ���������ع�ȫ����ӣ������ֲ��ع���������Ӧ��ʹ��Ƕ������
        ///           ��Ƕ�������г�����ӣ�ʧ�ܵĻ��ع�Ƕ������Ȼ�������������Ʒת������ϵͳ��
        /// </summary>
        /// <param name="item"></param>
        public async Task<int> AddAsync(int positionHint, BItem itemAdd)
        {
            if (itemAdd.Number <= 0)
                throw new ArgumentException();

            await module.Register(itemAdd.Item.Bean.GetType());
            int pileMax = GetItemPileMax(itemAdd.Id);

            // ���ȼӵ���ʾ����
            if (positionHint >= 0 && positionHint < bean.Capacity)
            {
                if (bean.Items.TryGetValue(positionHint, out var bItemHint))
                {
                    if (bItemHint.Id == itemAdd.Id)
                    {
                        int numberNew = bItemHint.Number + itemAdd.Number;
                        if (numberNew <= pileMax)
                        {
                            bItemHint.Number = numberNew;
                            return 0; // all pile done
                        }
                        bItemHint.Number = pileMax;
                        itemAdd.Number = numberNew - pileMax;
                        // continue to add
                    }
                    // continue to add
                }
                else
                {
                    bean.Items.Add(positionHint, itemAdd); // in managed
                    if (itemAdd.Number <= pileMax)
                    {
                        return 0;
                    }

                    int remain = itemAdd.Number - pileMax;
                    itemAdd.Number = pileMax;
                    itemAdd = itemAdd.Copy(); // current itemAdd has in mananged.
                    itemAdd.Number = remain;
                    // ready to continue add
                }
            }

            foreach (var item in bean.Items)
            {
                if (item.Value.Id == itemAdd.Id)
                {
                    int numberNew = item.Value.Number + itemAdd.Number;
                    if (numberNew > pileMax)
                    {
                        item.Value.Number = pileMax;
                        itemAdd.Number = numberNew - pileMax;
                        continue;
                    }
                    item.Value.Number = numberNew;
                    return 0; // all pile done
                }
            }
            while (itemAdd.Number > pileMax)
            {
                int pos = GetEmptyPosition();
                if (pos == -1)
                    return itemAdd.Number;

                BItem itemNew = itemAdd.Copy();
                itemNew.Number = pileMax;
                itemAdd.Number -= pileMax;
                bean.Items.Add(pos, itemNew);
            }
            if (itemAdd.Number > 0)
            {
                int pos = GetEmptyPosition();
                if (pos == -1)
                    return itemAdd.Number;
                bean.Items.Add(pos, itemAdd);
            }
            return 0;
        }

        private int GetEmptyPosition()
        {
            for (int pos = 0; pos < bean.Capacity; ++pos)
            {
                if (false == bean.Items.TryGetValue(pos, out var _))
                    return pos;
            }
            return -1;
        }

        private int GetItemPileMax(int itemId)
        {
            var tmp = FuncItemPileMax;
            if (tmp == null)
                return 1;
            return tmp(itemId);
        }

        /// <summary>
        /// �ƶ���Ʒ����һ�������ƶ�����һ�����ӡ�ʵ�ֹ��ܣ��ƶ������������ӣ���֡�
        /// </summary>
        /// <param name="from"></param>
        /// <param name="to"></param>
        /// <param name="number">-1 ��ʾ�����ƶ����е�</param>
        public int Move(int from, int to, int number)
        {
            // validate parameter
            if (from < 0 || from >= bean.Capacity)
                return Module.ResultCodeFromInvalid;

            if (to < 0 || to >= bean.Capacity)
                return Module.ResultCodeToInvalid;

            BItem itemFrom;
            if (false == bean.Items.TryGetValue(from, out itemFrom))
                return Module.ResultCodeFromNotExist;

            if (number < 0 || number > itemFrom.Number)
                number = itemFrom.Number; // move all

            int pileMax = GetItemPileMax(itemFrom.Id);
            if (bean.Items.TryGetValue(to, out var itemTo))
            {
                if (itemFrom.Id != itemTo.Id)
                {
                    if (number < itemFrom.Number)
                        // ��ͼ��֣�����Ŀ���Ѿ����ڲ�ͬ��Ʒ
                        return Module.ResultCodeTrySplitButTargetExistDifferenceItem;

                    // ����
                    BItem.Swap(itemFrom, itemTo);
                    return 0;
                }
                // ���ӣ����֣�
                int numberToWill = itemTo.Number + number;
                if (numberToWill > pileMax)
                {
                    itemTo.Number = pileMax;
                    itemFrom.Number = itemFrom.Number - number + (numberToWill - pileMax);
                }
                else
                {
                    if (itemFrom.Number == number)
                        bean.Items.Remove(from);
                    else
                        itemFrom.Number -= number;
                    itemTo.Number = numberToWill;
                }
                return 0;
            }
            // �ƶ������֣�
            BItem itemNew = itemFrom.Copy(); // �ȸ���һ�������ó�Ŀ��������
            itemNew.Number = number;
            if (itemFrom.Number == number)
            {
                // �ƶ�
                bean.Items.Remove(from);
                bean.Items.Add(to, itemNew);
                return 0;
            }
            // ���
            itemFrom.Number -= number;
            bean.Items.Add(to, itemNew);
            return 0;
        }

        public int Destroy(int from)
        {
            bean.Items.Remove(from);
            return 0;
        }

        public void Sort()
        {
            Sort((x, y) => x.Value.Id.CompareTo(y.Value.Id)); // sort by item.Id
        }

        public void Sort(Comparison<KeyValuePair<int, BItem>> comparison)
        {
            KeyValuePair<int, BItem>[] sort = bean.Items.ToArray();
            Array.Sort(sort, comparison);
            for (int i = 0; i < sort.Length; ++i)
            {
                BItem copy = sort[i].Value.Copy();
                sort[i] = KeyValuePair.Create(i, copy); // old item IsManaged. need Copy a new one.
            }
            bean.Items.Clear();
            bean.Items.AddRange(sort); // use AddRange for performence
        }


        public class Module : AbstractBag
        {
            private ConcurrentDictionary<string, Bag> Bags = new();
            public ProviderApp ProviderApp { get; }
            public Application Zeze { get; }

            public tBag getTable()
            {
                return _tBag;
            }

            public Module(ProviderApp pa)
            {
                ProviderApp = pa;
                Zeze = ProviderApp.Zeze;
                RegisterProtocols(ProviderApp.ProviderService);
                RegisterZezeTables(Zeze);
            }

            public Module(Application zeze)
            {
                Zeze = zeze;
                RegisterZezeTables(Zeze);
            }

            public override void UnRegister()
            {
                if (null != ProviderApp)
                    UnRegisterProtocols(ProviderApp.ProviderService);
                if (null != Zeze)
                    UnRegisterZezeTables(Zeze);
            }

            // ��Ҫ��������ʹ�á�
            // ʹ���겻Ҫ���档
            public async Task<Bag> OpenAsync(string bagName)
            {
                return await Bags.GetOrAdd(bagName, (key) => new Bag(this, bagName)).OpenAsync();
            }

            public async Task Register(Type type)
            {
                BeanFactory.GetType().GetMethod("Register").MakeGenericMethod(type).Invoke(BeanFactory, null);
                (await _tItemClasses.GetOrAddAsync(1)).ItemClasses.Add(type.FullName);
            }

            public void Start(Zeze.Application zeze)
            {
                ProviderApp.BuiltinModules.Add(FullName, this);
                if (0L != zeze.NewProcedure(async () =>
                {
                    var classes = await _tItemClasses.GetOrAddAsync(1);
                    foreach (var cls in classes.ItemClasses)
                    {
                        BeanFactory.GetType().GetMethod("Register")
                            .MakeGenericMethod(Util.Reflect.GetType(cls)).Invoke(BeanFactory, null);
                    }
                    return 0L;
                }, "").CallSynchronously())
                {
                    throw new Exception("Load Item Classes Failed.");
                }
            }

            public void Stop()
            {

            }

            protected override async Task<long> ProcessDestroyRequest(Protocol p)
            {
                var r = p as Destroy;
                var session = ProviderUserSession.Get(r);
                var moduleCode = (await OpenAsync(r.Argument.BagName)).Destroy(r.Argument.Position);
                if (0 != moduleCode)
                {
                    return ErrorCode(moduleCode);
                }
                session.SendResponseWhileCommit(r);
                return 0;
            }

            protected override async Task<long> ProcessMoveRequest(Protocol p)
            {
                var r = p as Move;
                var session = ProviderUserSession.Get(r);
                // throw exception if not login
                var moduleCode = (await OpenAsync(r.Argument.BagName)).Move(
                    r.Argument.PositionFrom, r.Argument.PositionTo, r.Argument.Number);
                if (moduleCode != 0)
                {
                    return ErrorCode(moduleCode);
                }
                session.SendResponseWhileCommit(r);
                return 0;
            }
        }
    }
}
