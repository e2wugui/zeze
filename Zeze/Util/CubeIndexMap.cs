using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text;
using System.Collections.Concurrent;
using System.Collections.Immutable;

namespace Zeze.Util
{
    public class CubeIndex : IComparable<CubeIndex>
    {
        public long X { get; set; }
        public long Y { get; set; }
        public long Z { get; set; }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + X.GetHashCode();
            result = prime * result + Y.GetHashCode();
            result = prime * result + Z.GetHashCode();
            return result;
        }

        public override bool Equals(object obj)
        {
            if (obj == this)
                return true;

            if (obj is CubeIndex other)
            {
                return X == other.X && Y == other.Y && Z == other.Z;
            }
            return false;
        }

        public int CompareTo(CubeIndex other)
        {
            int c = X.CompareTo(other.X);
            if (c != 0)
                return c;
            c = Y.CompareTo(other.Y);
            if (c != 0)
                return c;
            return Z.CompareTo(other.Z);
        }
    }

    public abstract class Cube<TObject>
    {
        public const int StateNormal = 0;
        public const int StateRemoved = -1;

        /// <summary>
        /// 子类实现可以利用这个状态，自定义状态必须大于等于0，负数保留给内部使用。
        /// </summary>
        public int State { get; set; }

        // under lock(cube)
        public abstract void Add(CubeIndex index, TObject obj);

        /// <summary>
        /// 返回 True 表示 Cube 可以删除。这是为了回收内存，如果不需要回收，永远返回false即可。
        /// under lock(cube)
        /// </summary>
        /// <param name="index"></param>
        /// <param name="obj"></param>
        /// <returns></returns>
        public abstract bool Remove(CubeIndex index, TObject obj);
    }

    /// <summary>
    /// 把三维空间划分成一个个相邻的Cube。
    /// 地图中的玩家或者物品Id记录在所在的Cube中。
    /// 用来快速找到某个坐标周围的玩家或物体。
    /// </summary>
    public class CubeIndexMap<TCube, TObject> where TCube : Cube<TObject>, new()
    {
        private ConcurrentDictionary<CubeIndex, TCube> Cubes
            = new ConcurrentDictionary<CubeIndex, TCube>();

        public int CubeSizeX { get; }
        public int CubeSizeY { get; }
        public int CubeSizeZ { get; }

        public CubeIndex ToIndex(double x, double y, double z)
        {
            return new CubeIndex()
            {
                X = (long)(x / CubeSizeX),
                Y = (long)(y / CubeSizeY),
                Z = (long)(z / CubeSizeZ),
            };
        }

        public CubeIndex ToIndex(float x, float y, float z)
        {
            return new CubeIndex()
            {
                X = (long)(x / CubeSizeX),
                Y = (long)(y / CubeSizeY),
                Z = (long)(z / CubeSizeZ),
            };
        }

        public CubeIndex ToIndex(long x, long y, long z)
        {
            return new CubeIndex()
            {
                X = (long)(x / CubeSizeX),
                Y = (long)(y / CubeSizeY),
                Z = (long)(z / CubeSizeZ),
            };
        }
        public CubeIndexMap(int cubeSizeX, int cubeSizeY, int cubeSizeZ)
        {
            if (cubeSizeX <= 0)
                throw new ArgumentException("cubeSizeX <= 0");
            if (cubeSizeY <= 0)
                throw new ArgumentException("cubeSizeY <= 0");
            if (cubeSizeZ <= 0)
                throw new ArgumentException("cubeSizeZ <= 0");

            this.CubeSizeX = cubeSizeX;
            this.CubeSizeY = cubeSizeY;
            this.CubeSizeZ = cubeSizeZ;
        }

        /// <summary>
        /// perfrom action if cube exist.
        /// under lock (cube)
        /// </summary>
        public void TryPerfrom(CubeIndex index, Action<CubeIndex, TCube> action)
        {
            if (Cubes.TryGetValue(index, out var cube))
            {
                lock (cube)
                {
                    if (cube.State != Cube<TObject>.StateRemoved)
                    {
                        action(index, cube);
                    }
                }
            }
        }

        /// <summary>
        /// perfrom action for Cubes.GetOrAdd.
        /// under lock (cube)
        /// </summary>
        public void Perform(CubeIndex index, Action<CubeIndex, TCube> action)
        {
            while (true)
            {
                var cube = Cubes.GetOrAdd(index, (_) => new TCube());
                lock (cube)
                {
                    if (cube.State == Cube<TObject>.StateRemoved)
                        continue;
                    action(index, cube);
                }
            }
        }

        /// <summary>
        /// 角色进入地图时
        /// </summary>
        public void OnEnter(TObject obj, double x, double y, double z)
        {
            Perform(ToIndex(x, y, z), (index, cube) => cube.Add(index, obj));
        }

        public void OnEnter(TObject obj, float x, float y, float z)
        {
            Perform(ToIndex(x, y, z), (index, cube) => cube.Add(index, obj));
        }

        public void OnEnter(TObject obj, long x, long y, long z)
        {
            Perform(ToIndex(x, y, z), (index, cube) => cube.Add(index, obj));
        }

        public void OnEnter(TObject obj, CubeIndex index)
        {
            Perform(index, (index, cube) => cube.Add(index, obj));
        }
        private void RemoveObject(CubeIndex index, TCube cube, TObject obj)
        {
            if (cube.Remove(index, obj))
            {
                cube.State = Cube<TObject>.StateRemoved;
                Cubes.TryRemove(index, out var _);
            }
        }

        private bool OnMove(CubeIndex oIndex, CubeIndex nIndex, TObject obj)
        {
            if (oIndex.Equals(nIndex))
                return false;

            TryPerfrom(oIndex, (index, cube) => RemoveObject(index, cube, obj));
            Perform(nIndex, (index, cube) => cube.Add(index, obj));
            return true;
        }

        /// <summary>
        /// 角色位置变化时，
        /// return true 如果cube发生了变化。
        /// return false 还在原来的cube中。
        /// </summary>
        public bool OnMove(TObject obj,
            double oldx, double oldy, double oldz,
            double newx, double newy, double newz)
        {
            return OnMove(ToIndex(oldx, oldy, oldz), ToIndex(newx, newy, newz), obj);
        }

        public bool OnMove(TObject obj,
            float oldx, float oldy, float oldz,
            float newx, float newy, float newz)
        {
            return OnMove(ToIndex(oldx, oldy, oldz), ToIndex(newx, newy, newz), obj);
        }

        public bool OnMove(TObject obj,
            long oldx, long oldy, long oldz,
            long newx, long newy, long newz)
        {
            return OnMove(ToIndex(oldx, oldy, oldz), ToIndex(newx, newy, newz), obj);
        }

        public bool OnMove(TObject obj,
            CubeIndex oldIndex,
            CubeIndex newIndex)
        {
            return OnMove(oldIndex, newIndex, obj);
        }
        /// <summary>
        /// 角色离开地图时
        /// </summary>
        public void OnLeave(TObject obj, double x, double y, double z)
        {
            TryPerfrom(ToIndex(x, y, z), (index, cube) => RemoveObject(index, cube, obj));
        }

        public void OnLeave(TObject obj, float x, float y, float z)
        {
            TryPerfrom(ToIndex(x, y, z), (index, cube) => RemoveObject(index, cube, obj));
        }

        public void OnLeave(TObject obj, long x, long y, long z)
        {
            TryPerfrom(ToIndex(x, y, z), (index, cube) => RemoveObject(index, cube, obj));
        }

        public void OnLeave(TObject obj, CubeIndex index)
        {
            TryPerfrom(index, (index, cube) => RemoveObject(index, cube, obj));
        }

        public List<TCube> GetCubes(CubeIndex center, int rangeX, int rangeY, int rangeZ)
        {
            List<TCube> result = new List<TCube>();
            for (long i = center.X - rangeX; i <= center.X + rangeX; ++i)
            {
                for (long j = center.Y - rangeY; j <= center.Y + rangeY; ++j)
                {
                    for (long k = center.Z - rangeZ; k <= center.Z + rangeZ; ++k)
                    {
                        var index = new CubeIndex() { X = i, Y = j, Z = k };
                        if (Cubes.TryGetValue(index, out var cube))
                            result.Add(cube);
                    }
                }
            }
            return result;
        }

        /// <summary>
        /// 返回 center 坐标所在的 cube 周围的所有 cube。
        /// 可以遍历返回的Cube的所有角色，进一步进行精确的距离判断。
        /// </summary>
        public List<TCube> GetCubes(
            double centerX, double centerY, double centerZ,
            int rangeX = 4, int rangeY = 4, int rangeZ = 4)
        {
            return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
        }

        public List<TCube> GetCubes(
            float centerX, float centerY, float centerZ,
            int rangeX = 4, int rangeY = 4, int rangeZ = 4)
        {
            return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
        }

        public List<TCube> GetCubes(
            long centerX, long centerY, long centerZ,
            int rangeX = 4, int rangeY = 4, int rangeZ = 4)
        {
            return GetCubes(ToIndex(centerX, centerY, centerZ), rangeX, rangeY, rangeZ);
        }
    }

    /// <summary>
    /// Game Helper
    /// </summary>
    public class GameObjectId : IComparable<GameObjectId>
    {
        public int Type { get; set; }
        public int ConfigId { get; set; }
        public long InstanceId { get; set; }

        public override int GetHashCode()
        {
            const int prime = 31;
            int result = 17;
            result = prime * result + Type.GetHashCode();
            result = prime * result + ConfigId.GetHashCode();
            result = prime * result + InstanceId.GetHashCode();
            return result;
        }

        public override bool Equals(object obj)
        {
            if (obj == null)
                return false;

            if (obj == this)
                return true;

            if (obj is GameObjectId o)
            {
                return Type == o.Type && ConfigId == o.ConfigId && InstanceId == o.InstanceId;
            }
            return false;
        }

        public int CompareTo(GameObjectId x)
        {
            int c = Type.CompareTo(x.Type);
            if (c != 0)
                return c;
            c = ConfigId.CompareTo(x.ConfigId);
            if (c != 0)
                return c;
            return InstanceId.CompareTo(x.InstanceId);
        }
    }

    public class GameCube : Cube<GameObjectId>
    {
        public ImmutableHashSet<GameObjectId> ObjectIds { get; private set; }
            = ImmutableHashSet<GameObjectId>.Empty;

        public override void Add(CubeIndex index, GameObjectId obj)
        {
            // under lock(cube)
            ObjectIds = ObjectIds.Add(obj);
        }

        /// <summary>
        /// 返回 True 表示 Cube 可以删除。这是为了回收内存。
        /// </summary>
        /// <param name="index"></param>
        /// <param name="obj"></param>
        /// <returns></returns>
        public override bool Remove(CubeIndex index, GameObjectId obj)
        {
            // under lock(cube)
            ObjectIds = ObjectIds.Remove(obj);
            return ObjectIds.Count == 0;
        }
    }


    public class GameMap : CubeIndexMap<GameCube, GameObjectId>
    {
        public GameMap(int cubeSizeX = 256, int cubeSizeY = 256, int cubeSizeZ = 256)
            : base(cubeSizeX, cubeSizeY, cubeSizeZ)
        {

        }
    }
}
