package mtr.mappings;

import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

/** Contains matrix stack operations for both Matrix3x2fStack and PoseStack */
public interface MatrixStackWrapper {

    void pushPose();

    void popPose();

    void translate(float x, float y, float z);

    void translate(double x, double y, double z);

    void scale(float x, float y, float z);

    Matrix4f last();

    class Matrix3x2f implements MatrixStackWrapper {

        private final Matrix3x2fStack impl;

        public Matrix3x2f(Matrix3x2fStack impl) {
            this.impl = impl;
        }

        @Override
        public void pushPose() {
            this.impl.pushMatrix();
        }

        @Override
        public void popPose() {
            this.impl.popMatrix();
        }

        @Override
        public void translate(float x, float y, float z) {
            this.impl.translate(x, y);
        }

        @Override
        public void translate(double x, double y, double z) {
            this.impl.translate((float)x, (float)y);
        }

        @Override
        public void scale(float x, float y, float z) {
            this.impl.scale(x, y);
        }

        @Override
        public Matrix4f last() {
            return new Matrix4f(
                    this.impl.m00, this.impl.m01, 0.0f, 0.0f, // Column 0
                    this.impl.m10, this.impl.m11, 0.0f, 0.0f, // Column 1
                    0.0f,       0.0f,       1.0f, 0.0f, // Column 2
                    this.impl.m20, this.impl.m21, 0.0f, 1.0f  // Column 3 (Translation)
            );

//            return new Matrix4f().set((Matrix3x2fc)this.impl);
        }
    }

    class PoseStack implements MatrixStackWrapper {

        private final com.mojang.blaze3d.vertex.PoseStack impl;

        public PoseStack(com.mojang.blaze3d.vertex.PoseStack impl) {
            this.impl = impl;
        }

        @Override
        public void pushPose() {
            this.impl.pushPose();
        }

        @Override
        public void popPose() {
            this.impl.popPose();
        }

        @Override
        public void translate(float x, float y, float z) {
            this.impl.translate(x, y, z);
        }

        @Override
        public void translate(double x, double y, double z) {
            this.impl.translate(x, y, z);
        }

        @Override
        public void scale(float x, float y, float z) {
            this.impl.scale(x, y, z);
        }

        @Override
        public Matrix4f last() {
            return this.impl.last().pose();
        }
    }
}
