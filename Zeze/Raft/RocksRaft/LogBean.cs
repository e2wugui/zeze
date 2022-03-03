using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class LogBean : Log
	{
		public Dictionary<int, Log> Variables { get; } = new Dictionary<int, Log>();

		// LogBean仅在_final_commit的Collect过程中创建，不会参与Savepoint。
        internal override Log BeginSavepoint()
        {
            throw new NotImplementedException();
        }

        internal override void EndSavepoint(Savepoint currentsp)
        {
            throw new NotImplementedException();
        }

        public override void Decode(ByteBuffer bb)
		{
			for (int i = bb.ReadInt(); i > 0; --i)
			{
				var typeId = bb.ReadInt4();
				var log = Log.Create(typeId);

				var varId = bb.ReadInt();
				log.VariableId = varId;
				log.Decode(bb);

				Variables.Add(varId, log);
			}
		}

		public override void Encode(ByteBuffer bb)
		{
			bb.WriteInt(Variables.Count);
			foreach (var log in Variables.Values)
			{
				bb.WriteInt4(log.TypeId);
				bb.WriteInt(log.VariableId);
				log.Encode(bb);
			}
		}

		// 仅发生在事务执行期间。Decode-Apply不会执行到这里。
		public override void Collect(Changes changes, Bean recent, Log vlog)
        {
			if (Variables.TryAdd(vlog.VariableId, vlog))
            {
				// 向上传递
				changes.Collect(recent, this);
			}
		}

        public override string ToString()
        {
			var sb = new StringBuilder();
			ByteBuffer.BuildString(sb, Variables, Zeze.Util.ComparerInt.Instance);
            return sb.ToString();
        }
	}
}
