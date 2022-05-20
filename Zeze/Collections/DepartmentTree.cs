
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Builtin.Collections.DepartmentTree;
using Zeze.Transaction;

namespace Zeze.Collections
{
    public abstract class DepartmentTree
    {
		internal static readonly BeanFactory BeanFactory = new BeanFactory();

		public static long GetSpecialTypeIdFromBean(Bean bean)
		{
			return bean.TypeId;
		}

		public static Bean CreateBeanFromSpecialTypeId(long typeId)
		{
			return BeanFactory.Create(typeId);
		}


		public class Module : AbstractDepartmentTree
		{
			private readonly ConcurrentDictionary<string, DepartmentTree> Trees = new();
			public Zeze.Application Zeze { get; }
			public LinkedMap.Module Members { get; }

			public Module(Zeze.Application zeze)
			{
				Zeze = zeze;
				RegisterZezeTables(zeze);
				Members = new LinkedMap.Module(zeze);
			}

			public override void UnRegister()
			{
				Members.UnRegister();
				UnRegisterZezeTables(Zeze);
			}

			public DepartmentTree<TManager, TMember> Open<TManager, TMember>(string name)
				where TManager : Bean, new() where TMember : Bean, new()
			{
				return (DepartmentTree<TManager, TMember>)Trees.GetOrAdd(name, k => new DepartmentTree<TManager, TMember>(this, k));
			}
		}
	}

	// M is Manager Bean
	public class DepartmentTree<TManager, TMember> : DepartmentTree where TManager : Bean, new() where TMember : Bean, new()
	{
		private readonly Module module;
		private readonly string name;

		internal DepartmentTree(Module module, string name)
		{
			this.module = module;
			this.name = name;
			BeanFactory.Register<TManager>();
		}

		public string Name => name;

		public async Task<BDepartmentRoot> GetRootAsync()
		{
			return await module._tDepartment.GetAsync(name);
		}

		public async Task<BDepartmentTreeNode> GetNodeAsync(long departmentId)
		{
			return await module._tDepartmentTree.GetAsync(new BDepartmentKey(name, departmentId));
		}

		public LinkedMap<TMember> GetMembers(long departmentId)
		{
			return module.Members.Open<TMember>($"{departmentId}#{name}");
		}

		public async Task<BDepartmentRoot> SelectDirtyRootAsync()
		{
			return await module._tDepartment.SelectDirtyAsync(name);
		}

		public async Task<BDepartmentTreeNode> SelectDirtyNodeAsync(long departmentId)
		{
			return await module._tDepartmentTree.SelectDirtyAsync(new BDepartmentKey(name, departmentId));
		}

		public async Task<BDepartmentRoot> CreateAsync(string root)
        {
			var droot = await module._tDepartment.GetOrAddAsync(name);
			droot.Root = root;
			return droot;
        }

		public async Task<bool> ChangeRootAsync(string oldroot, string newroot)
		{
			var droot = await module._tDepartment.GetOrAddAsync(name);
			if (!droot.Root.Equals(oldroot))
				return false;
			droot.Root = newroot;
			return true;
		}

		// 其他功能不实现了，参考ZezeJava。
	}
}
