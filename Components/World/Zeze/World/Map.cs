
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.World;
using Zeze.Util;
using Zeze.Serialize;
using Zeze.Net;
using System.Collections.Concurrent;

namespace Zeze.World
{
    public class Map : ICommand
    {
        public World World { get; }
        public long MapInstanceId { get; }
        public ConcurrentDictionary<long, Entity> Entities { get; } = new ConcurrentDictionary<long, Entity>();

        public Map(World world, long mapInstanceId)
        {
            World = world;
            MapInstanceId = mapInstanceId;

            World.Register(BCommand.eMoveMmo, this);
            World.Register(BCommand.eAoiEnter, this);
            World.Register(BCommand.eAoiOperate, this);
            World.Register(BCommand.eAoiLeave, this);
        }

        public async Task<BCommand> Query(int commandId, ConfBean param)
        {
            var r = new Query();
            r.Argument.CommandId = commandId;
            var bb = ByteBuffer.Allocate();
            param.Encode(bb);
            r.Argument.Param = new Binary(bb);
            r.Argument.MapInstanceId = MapInstanceId;

            await r.SendAsync(World.Service.GetSocket());
            if (r.ResultCode != ResultCode.Success)
                throw new Exception($"SwitchWorld Error={World.GetErrorCode(r.ResultCode)}");

            return r.Result;
        }

        public bool SendCommand(int commandId, ConfBean param)
        {
            var r = new Command();
            r.Argument.CommandId = commandId;
            var bb = ByteBuffer.Allocate();
            param.Encode(bb);
            r.Argument.Param = new Binary(bb);
            r.Argument.MapInstanceId = MapInstanceId;

            return r.Send(World.Service.GetSocket());
        }

        public Task<long> Handle(BCommand c)
        {
            switch (c.CommandId)
            {
                case BCommand.eMoveMmo:
                    break;

                case BCommand.eAoiEnter:
                    {
                        // enter 最好使用单独的结构，不要和eAoiOperate混起来。
                        var enter = new BAoiOperates();
                        enter.Decode(ByteBuffer.Wrap(c.Param));
                        foreach (var e in enter.Operates)
                        {
                            // GetOrAdd 允许存在。存在时继续更新，一般是覆盖数据，Entity来决定。
                            Entities.GetOrAdd(e.Key, (key) => new Entity(key)).ProcessOperete(e.Value);
                        }
                    }
                    break;

                case BCommand.eAoiLeave:
                    {
                        var leave = new BAoiLeaves();
                        leave.Decode(ByteBuffer.Wrap(c.Param));
                        foreach (var entityId in leave.Keys)
                        {
                            // 实体销毁事件。
                            Entities.TryRemove(entityId, out var entity);
                        }
                    }
                    break;

                case BCommand.eAoiOperate:
                    {
                        var operates = new BAoiOperates();
                        operates.Decode(ByteBuffer.Wrap(c.Param));
                        foreach (var e in operates.Operates)
                        {
                            // eAoiOperate 通知忽略不存在的。
                            if (Entities.TryGetValue(e.Key, out var entity))
                                entity.ProcessOperete(e.Value);
                        }
                    }
                    break;
            }
            return Task.FromResult(0L);
        }

        internal void ProcessEnter(List<BAoiOperates> datas)
        {
            foreach (var data in datas)
            {
                foreach (var e in data.Operates)
                    Entities.GetOrAdd(e.Key, (key) => new Entity(key)).ProcessOperete(e.Value);
            }
        }

    }
}
