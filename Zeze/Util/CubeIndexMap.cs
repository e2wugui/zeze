using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text;
using System.Collections.Concurrent;

namespace Zeze.Util
{
    /// <summary>
    /// 把三维空间划分成一个个相邻的Cube。地图中的玩家或者物品Id记录在所在的Cube中。
    /// 用来快速找到某个坐标周围的玩家或物体。
    /// </summary>
    public class CubeIndexMap
    {
        public class ObjectId : Comparer<ObjectId>
        {
            public int Type { get; set; }
            public int ConfigId { get; set; }
            public long InstanceId { get; set; }

            public override int Compare(ObjectId x, ObjectId y)
            {
                int c = Type.CompareTo(x.Type);
                if (c != 0)
                    return c;
                c = ConfigId.CompareTo(x.ConfigId);
                if (c != 0)
                    return c;
                return InstanceId.CompareTo(x.InstanceId);
            }

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
        }

        public class Cube
        {
            public HashSet<ObjectId> ObjectIds { get; } = new HashSet<ObjectId>();
        }

        private ConcurrentDictionary<long, ConcurrentDictionary<long, ConcurrentDictionary<long, Cube>>> IndexX
            = new ConcurrentDictionary<long, ConcurrentDictionary<long, ConcurrentDictionary<long, Cube>>>();

        private int CubeSizeX;
        private int CubeSizeY;
        private int CubeSizeZ;

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
        /// 角色进入地图时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="x"></param>
        /// <param name="y"></param>
        /// <param name="z"></param>
        public void OnEnter(ObjectId objId, double dx, double dy, double dz)
        {
            long x = (long)(dx / CubeSizeX);
            long y = (long)(dy / CubeSizeY);
            long z = (long)(dz / CubeSizeZ);

            GetOrAdd(x, y, z).ObjectIds.Add(objId);
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
            long ox = (long)(oldx / CubeSizeX);
            long oy = (long)(oldy / CubeSizeY);
            long oz = (long)(oldz / CubeSizeZ);

            long nx = (long)(newx / CubeSizeX);
            long ny = (long)(newy / CubeSizeY);
            long nz = (long)(newz / CubeSizeZ);

            if (ox == nx && oy == ny && oz == nz)
                return;

            Get(ox, oy, oz)?.ObjectIds.Remove(objId);
            GetOrAdd(nx, ny, nz).ObjectIds.Add(objId);
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
            long ix = (long)(x / CubeSizeX);
            long iy = (long)(y / CubeSizeY);
            long iz = (long)(z / CubeSizeZ);

            Get(ix, iy, iz)?.ObjectIds.Remove(objId);
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
            long ix = (long)(centerX / CubeSizeX);
            long iy = (long)(centerY / CubeSizeY);
            long iz = (long)(centerZ / CubeSizeZ);

            List<Cube> result = new List<Cube>();
            for (long i = ix - rangeX; i <= ix + rangeX; ++i)
            {
                for (long j = iy - rangeY; j <= iy + rangeY; ++j)
                {
                    for (long k = iz - rangeZ; k <= iz + rangeZ; ++k)
                    {
                        Cube cube = Get(i, j, k);
                        if (cube != null)
                            result.Add(cube);
                    }
                }
            }
            return result;
        }

        private Cube GetOrAdd(long ix, long iy, long iz)
        {
            var IndexY = IndexX.GetOrAdd(ix, (_) => new ConcurrentDictionary<long, ConcurrentDictionary<long, Cube>>());
            var IndexZ = IndexY.GetOrAdd(iy, (_) => new ConcurrentDictionary<long, Cube>());
            var cube   = IndexZ.GetOrAdd(iz, (_) => new Cube());
            return cube;
        }


        private Cube Get(long x, long y, long z)
        {
            if (!IndexX.TryGetValue(x, out var IndexY))
                return null;
            if (!IndexY.TryGetValue(y, out var IndexZ))
                return null;
            if (!IndexZ.TryGetValue(z, out var cube))
                return null;
            return cube;
        }
    }
}
