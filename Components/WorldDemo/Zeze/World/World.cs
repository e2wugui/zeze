
using Zeze.Serialize;
using Zeze.Builtin.World.Static;
using Zeze.Util;
using Zeze.Net;
using Zeze.Builtin.World;
using System.Collections.Concurrent;

namespace Zeze.World
{
    public class World : AbstractWorld, ICommand
    {
        public static BeanFactory<Util.ConfBean> BeanFactory { get; } = new();
        public Application Zeze { get; }
        public Service Service { get; }

        public WorldStatic WorldStatic { get; }
        public Map Map { get; private set; }

        private ConcurrentDictionary<int, ICommand> Commands = new ConcurrentDictionary<int, ICommand>();

        public static long GetSpecialTypeIdFromBean(ConfBean bean)
        {
            return bean.TypeId;
        }

        public static ConfBean CreateBeanFromSpecialTypeId(long typeId)
        {
            return BeanFactory.Create(typeId);
        }

        protected override Task<long> ProcessCommand(Zeze.Net.Protocol _p)
        {
            var p = _p as Command;
            if (!Commands.TryGetValue(p.Argument.CommandId, out var command))
                return Task.FromResult<long>(GetErrorCode(eCommandHandlerMissing));
            return command.Handle(p.Argument);
        }

        /// <summary>
        /// Application仅用来传递Config；
        /// Service网络管理服务，用来发送协议；
        /// </summary>
        /// <param name="zeze"></param>
        /// <param name="service"></param>
        public World(Application zeze, Service service)
        {
            Zeze = zeze;
            Service = service;

            WorldStatic = new WorldStatic(service); // 目前仅在里面注册协议。

            RegisterProtocols(service);
            Register(BCommand.eEnterWorld, this); // 这条命令自己处理。
        }

        public void Register(int commandId, ICommand command)
        {
            if (false == Commands.TryAdd(commandId, command))
                throw new Exception($"Duplicate Command={commandId}");
        }

        /// <summary>
        /// 切换地图.
        /// 
        /// 一般mapId等参数应该是服务器决定，客户端不能随意指定。
        /// 除非允许客户端随意传送。
        /// 这里先定义成这样。
        /// 如果不允许随意传送，服务器自行忽略参数。
        /// </summary>
        /// <param name="mapId"></param>
        /// <param name="position"></param>
        /// <param name="direcet"></param>
        /// <returns></returns>
        /// <exception cref="Exception"></exception>
        public async Task<long> SwitchWorld(int mapId, Vector3 position, Vector3 direcet)
        {
            var r = new SwitchWorld();
            r.Argument.MapId = mapId;

            await r.SendAsync(Service.GetSocket());
            if (r.ResultCode != ResultCode.Success)
                throw new Exception($"SwitchWorld Error={GetErrorCode(r.ResultCode)}");

            return r.Result.MapInstanceId;
        }

        public Task<long> Handle(BCommand c)
        {
            switch (c.CommandId)
            {
                case BCommand.eEnterWorld:
                    var enter = new BEnterWorld();
                    var bb = ByteBuffer.Wrap(c.Param);
                    enter.Decode(bb);

                    if (null != Map)
                        Console.WriteLine("Trigger Unload Map");

                    Map = new Map(this, enter.MapInstanceId);
                    Map.ProcessEnter(enter.PriorityData);

                    Console.WriteLine("Trigger Load Map");

                    var confirm = new BEnterConfirm();
                    confirm.MapInstanceId = enter.MapInstanceId;

                    Map.SendCommand(BCommand.eEnterConfirm, confirm);

                    break;
            }
            return Task.FromResult(0L);
        }
    }
}
