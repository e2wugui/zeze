
package Zeze.World.Graphics;

import Zeze.Serialize.Quaternion;
import Zeze.Serialize.Vector3;

// column major matrix
public class Matrix3x3f {

    private float m00;
    private float m01;
    private float m02;

    private float m10;
    private float m11;
    private float m12;

    private float m20;
    private float m21;
    private float m22;


    public Matrix3x3f() {
        m00 = 0f;
        m01 = 0f;
        m02 = 0f;

        m10 = 0f;
        m11 = 0f;
        m12 = 0f;

        m20 = 0f;
        m21 = 0f;
        m22 = 0f;

        // data = new float[9];
    }

    // private void set(int row, int column, float value) {
    //     data[row + column * 3] = value;
    // }

    // private float get(int row, int column) {
    //     return data[row + column * 3];
    // }

    public Matrix3x3f transpose() {
        Matrix3x3f m = new Matrix3x3f();
        m.m00 = m00;
        m.m01 = m10;
        m.m02 = m20;

        m.m10 = m01;
        m.m11 = m11;
        m.m12 = m21;

        m.m20 = m02;
        m.m21 = m12;
        m.m22 = m22;
        // for (int r = 0; r < 3; r ++) {
        //     for (int c = 0; c < 3; c ++) {
        //         m.set(r, c, this.get(c, r));
        //     }
        // }
        return m;
    }

    public Vector3 multiplyVector(Vector3 v) {
        // float x = data[0] * v.x + data[3] * v.y + data[6] * v.z;
        // float y = data[1] * v.x + data[4] * v.y + data[7] * v.z;
        // float z = data[2] * v.x + data[5] * v.y + data[8] * v.z;

        float x = m00 * v.x + m01 * v.y + m02 * v.z;
        float y = m10 * v.x + m11 * v.y + m12 * v.z;
        float z = m20 * v.x + m21 * v.y + m22 * v.z;
        return new Vector3(x, y, z);
    }

    public Matrix3x3f multiply(Matrix3x3f v) {
        Matrix3x3f m = new Matrix3x3f();
        // for (int r = 0; r < 3; r ++) {
        //     for (int c = 0; c < 3; c ++) {
        //         float sum = 0;
        //         for (int i = 0; i < 3; i ++) {
        //             sum += this.get(r, i) * other.get(i, c);
        //         }
        //         m.set(r, c, sum);
        //     }
        // }
        m.m00 = m00 * v.m00 + m01 * v.m10 + m02 * v.m20;
        m.m01 = m00 * v.m01 + m01 * v.m11 + m02 * v.m21;
        m.m02 = m00 * v.m02 + m01 * v.m12 + m02 * v.m22;

        m.m10 = m10 * v.m00 + m11 * v.m10 + m12 * v.m20;
        m.m11 = m10 * v.m01 + m11 * v.m11 + m12 * v.m21;
        m.m12 = m10 * v.m02 + m11 * v.m12 + m12 * v.m22;

        m.m20 = m20 * v.m00 + m21 * v.m10 + m22 * v.m20;
        m.m21 = m20 * v.m01 + m21 * v.m11 + m22 * v.m21;
        m.m22 = m20 * v.m02 + m21 * v.m12 + m22 * v.m22;

        return m;
    }

    public void setFromToRotation(Vector3 from, Vector3 to) {

        float c = Vector3.dot(from, to);
        if (c > 1.0 - 1e-6f) {
            m00 = m11 = m22 = 1f;
        } else if (c < -1.0 + 1e-6f) {
            var left = Vector3.cross(from, new Vector3(1, 0, 0));

            if (Vector3.dot(left, left) < 1e-6f) {

                left = Vector3.cross(from, new Vector3(0, 1, 0));
            }
            float magnitude = left.magnitude();
            left = new Vector3(left.x / magnitude, left.y / magnitude, left.z / magnitude);

            Vector3 up = Vector3.cross(left, from);


            // Matrix3x3f source = new Matrix3x3f();
            // Matrix3x3f target = new Matrix3x3f();

            // source.set(0, 0, from.x);
            // source.set(1, 0, from.y);
            // source.set(2, 0, from.z);

            // source.set(0, 1, up.x);
            // source.set(1, 1, up.y);
            // source.set(2, 1, up.z);

            // source.set(0, 2, left.x);
            // source.set(1, 2, left.y);
            // source.set(2, 2, left.z);


            // target.set(0, 0, -from.x);
            // target.set(1, 0, -from.y);
            // target.set(2, 0, -from.z);

            // target.set(0, 1, up.x);
            // target.set(1, 1, up.y);
            // target.set(2, 1, up.z);

            // target.set(0, 2, -left.x);
            // target.set(1, 2, -left.y);
            // target.set(2, 2, -left.z);

            // Matrix3x3f result = source.multiply(target.transpose());
            // for (int i = 0; i < 9; i ++) {
            //     this.data[i] = result.data[i];
            // }

            /*
            Here we choose two matrix source, target, and we rotate source matrix to target matrix.
            source.forward = left, source.up = up, source.right = from;
            target.forward = left, target.up = up, target.right = -from;
            source * anyVectorV = result * (target * anyVectorV)
            result = source * inverse(target) = source * transpose(target)
            The inverse of a rotation matrix is its transpose, which is also a rotation matrix.
            */

            float fx = from.x;
            float fy = from.y;
            float fz = from.z;
            float ux = up.x;
            float uy = up.y;
            float uz = up.z;
            float lx = left.x;
            float ly = left.y;
            float lz = left.z;
            /*
            S =
            fx ux lx
            fy uy ly
            fz uz lz

            T =
            -fx ux -lx
            -fy uy -ly
            -fz uz -lz

            inverse(T) = transpose(T) =
            -fx -fy -fz
            ux  uy  uz
            -lx -ly -lz

            S.forward = left; S.up = up; S.right = from;
            T.forward = -left; T.up = up; T.right = -from;

            S * anyVectorX = finalResult * (T * anyVectorX) ->  finalResult = S * inverse(T)
            */
            m00 = -fx * fx + ux * ux - lx * lx;
            m01 = -fx * fy + ux * uy - lx * ly;
            m02 = -fx * fz + ux * uz - lx * lz;

            // float m10 = -fy * fx + uy * ux - ly * lx;
            m10 = m01;
            m11 = -fy * fy + uy * uy - ly * ly;
            m12 = -fy * fz + uy * uz - ly * lz;


            // float m20 = -fz * fx + uz * ux - lz * lx;
            m20 = m02;
            // float m21 = -fz * fy + uz * uy - lz * ly;
            m21 = m12;
            m22 = -fz * fz + uz * uz - lz * lz;




            // set(0, 0, m00);
            // set(0, 1, m01);
            // set(0, 2, m02);

            // set(1, 0, m01);
            // set(1, 1, m11);
            // set(1, 2, m12);

            // set(2, 0, m02);
            // set(2, 1, m12);
            // set(2, 2, m22);

        } else {
            // https://en.wikipedia.org/wiki/Rotation_matrix
            // https://cs.brown.edu/research/pubs/pdfs/1999/Moller-1999-EBA.pdf

            Vector3 v = Vector3.cross(from, to);
            float h = (1 - c) / Vector3.dot(v, v);
            // System.out.println("h : " + h + ", c : " + c);
            m00 = c + h * v.x * v.x;
            m01 = h * v.x * v.y - v.z;
            m02 = h * v.x * v.z + v.y;

            m10 = h * v.x * v.y + v.z;
            m11 = c + h * v.y * v.y;
            m12 = h * v.y * v.z - v.x;

            m20 = h * v.x * v.z - v.y;
            m21 = h * v.y * v.z + v.x;
            m22 = c + h * v.z * v.z;

        }
    }

    @Override
    public String toString() {
		return m00 + " " + m01 + " " + m02 + "\n" + m10 + " " + m11 + " " + m12 + "\n" + m20 + " " + m21 + " " + m22;
    }

    public Quaternion rotation() {
        // https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/

        float qx = 0f;
        float qy = 0f;
        float qz = 0f;
        float qw = 0f;

        float tr = m00 + m11 + m22;
        if (tr > 0) {
            float S = (float)Math.sqrt(tr + 1.0) * 2;
            qw = 0.25f * S;
            qx = (m21 - m12) / S;
            qy = (m02 - m20) / S;
            qz = (m10 - m01) / S;
        } else if (m00 > m11 && m00 > m22) {
            float S = (float)Math.sqrt(1.0 + m00 - m11 - m22) * 2;
            qw = (m21 - m12) / S;
            qx = 0.25f * S;
            qy = (m01 + m10) / S;
            qz = (m02 + m20) / S;
        } else if (m11 > m22) {
            float S = (float)Math.sqrt(1.0 + m11 - m00 - m22) * 2;
            qw = (m02 - m20) / S;
            qx = (m01 + m10) / S;
            qy = 0.25f * S;
            qz = (m12 + m21) / S;
        } else {
            float S = (float)Math.sqrt(1.0 + m22 - m00 - m11) * 2;
            qw = (m10 - m01) / S;
            qx = (m02 + m20) / S;
            qy = (m12 + m21) / S;
            qz = 0.25f * S;
        }
        return new Quaternion(qx, qy, qz, qw).normalized();
    }
}