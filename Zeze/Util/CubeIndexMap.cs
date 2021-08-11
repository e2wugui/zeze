using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    /// <summary>
    /// 把三维空间划分成一个个相邻的Cube。
    /// 地图中的玩家或者物品Id记录在所在的Cube中。
    /// 用来快速找到某个坐标周围的玩家或物体。
    /// </summary>
    public class CubeIndexMap
    {
        public class Index : IComparable<Index>
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

                if (obj is Index other)
                {
                    return X == other.X && Y == other.Y && Z == other.Z;
                }
                return false;
            }

            public int CompareTo(Index other)
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

        public class ObjectId : IComparable<ObjectId>
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

                if (obj is ObjectId o)
                {
                    return Type == o.Type && ConfigId == o.ConfigId && InstanceId == o.InstanceId;
                }
                return false;
            }

            public int CompareTo(ObjectId x)
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

        public class Cube
        {
            public const int StateRemoved = -1;

            public int State { get; set; }

            public HashSet<ObjectId> ObjectIds { get; } = new HashSet<ObjectId>();

            public virtual void Add(Index index, ObjectId obj)
            {
                ObjectIds.Add(obj);
            }

            public virtual void Remove(Index index, ObjectId obj)
            {
                ObjectIds.Remove(obj);
            }
        }

        private ConcurrentDictionary<Index, Cube> Cubes = new ConcurrentDictionary<Index, Cube>();

        public int CubeSizeX { get; }
        public int CubeSizeY { get; }
        public int CubeSizeZ { get; }

        public Index ToIndex(double x, double y, double z)
        {
            return new Index()
            {
                X = (long)(x / CubeSizeX),
                Y = (long)(y / CubeSizeY),
                Z = (long)(z / CubeSizeZ),
            };
        }

        public CubeIndexMap(int cubeSizeX = 256, int cubeSizeY = 256, int cubeSizeZ = 256)
        {
            if (cubeSizeX <= 0)
                throw new ArgumentException();
            if (cubeSizeY <= 0)
                throw new ArgumentException();
            if (cubeSizeZ <= 0)
                throw new ArgumentException();

            this.CubeSizeX = cubeSizeX;
            this.CubeSizeY = cubeSizeY;
            this.CubeSizeZ = cubeSizeZ;
        }

        /// <summary>
        /// 可以重载，提供Cube的子类进行扩展。
        /// </summary>
        /// <param name="index"></param>
        /// <returns></returns>
        public virtual Cube CreateCubeFactory(Index index)
        {
            return new Cube();
        }

        /// <summary>
        /// perfrom action if cube exist.
        /// under lock (cube)
        /// </summary>
        public void TryPerfrom(Index index, Action<Index, Cube> action)
        {
            if (Cubes.TryGetValue(index, out var cube))
            {
                lock (cube)
                {
                    if (cube.State != Cube.StateRemoved)
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
        public void Perform(Index index, Action<Index, Cube> action)
        {
            while (true)
            {
                var cube = Cubes.GetOrAdd(index, CreateCubeFactory);
                lock (cube)
                {
                    if (cube.State == Cube.StateRemoved)
                        continue;
                    action(index, cube);
                }
            }
        }

        /// <summary>
        /// 角色进入地图时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="x"></param>
        /// <param name="y"></param>
        /// <param name="z"></param>
        public void OnEnter(ObjectId objId, double x, double y, double z)
        {
            Perform(ToIndex(x, y, z), (index, cube) => cube.Add(index, objId));
        }

        /// <summary>
        /// 角色位置变化时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="oldx"></param>
        /// <param name="oldy"></param>
        /// <param name="oldz"></param>
        /// <param name="newx"></param>
        /// <param name="newy"></param>
        /// <param name="newz"></param>
        public void OnMove(ObjectId objId,
            double oldx, double oldy, double oldz,
            double newx, double newy, double newz)
        {
            var oIndex = ToIndex(oldx, oldy, oldz);
            var nIndex = ToIndex(newx, newy, newz);

            if (oIndex.Equals(nIndex))
                return;

            TryPerfrom(oIndex, (index, cube) => RemoveObject(index, cube, objId));
            Perform(nIndex, (index, cube) => cube.Add(index, objId));
        }

        private void RemoveObject(Index index, Cube cube, ObjectId obj)
        {
            cube.Remove(index, obj);
            if (cube.ObjectIds.Count == 0)
            {
                cube.State = Cube.StateRemoved;
                Cubes.TryRemove(index, out var _);
            }
        }

        /// <summary>
        /// 角色离开地图时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="x"></param>
        /// <param name="y"></param>
        /// <param name="z"></param>
        public void OnLeave(ObjectId objId, double x, double y, double z)
        {
            TryPerfrom(ToIndex(x, y, z), (index, cube) => RemoveObject(index, cube, objId));
        }

        /// <summary>
        /// 返回 center 坐标所在的 cube 周围的所有 cube。
        /// 可以遍历返回的Cube的所有角色，进一步进行精确的距离判断。
        /// </summary>
        /// <param name="centerX"></param>
        /// <param name="centerY"></param>
        /// <param name="centerZ"></param>
        /// <param name="range">周围cube数</param>
        /// <returns></returns>
        public List<Cube> GetCubes(
            double centerX, double centerY, double centerZ,
            int rangeX = 4, int rangeY = 4, int rangeZ = 4)
        {
            var center = ToIndex(centerX, centerY, centerZ);

            List<Cube> result = new List<Cube>();
            for (long i = center.X - rangeX; i <= center.X + rangeX; ++i)
            {
                for (long j = center.Y - rangeY; j <= center.Y + rangeY; ++j)
                {
                    for (long k = center.Z - rangeZ; k <= center.Z + rangeZ; ++k)
                    {
                        var index = new Index() { X = i, Y = j, Z = k };
                        if (Cubes.TryGetValue(index, out var cube))
                            result.Add(cube);
                    }
                }
            }
            return result;
        }
    }
}
