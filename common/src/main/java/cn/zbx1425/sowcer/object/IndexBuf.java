package cn.zbx1425.sowcer.object;

import com.mojang.blaze3d.vertex.VertexFormat;

public class IndexBuf extends VertBuf {

    public int faceCount;
    // public final int indexType;
    public final VertexFormat.IndexType indexType;

    public int vertexCount;

    public IndexBuf(int faceCount, /*int*/ VertexFormat.IndexType indexType) {
        this.faceCount = faceCount;
        this.indexType = indexType;
        this.vertexCount = faceCount * 3;
    }

    public void setFaceCount(int faceCount) {
        this.faceCount = faceCount;
        this.vertexCount = faceCount * 3;
    }

}
