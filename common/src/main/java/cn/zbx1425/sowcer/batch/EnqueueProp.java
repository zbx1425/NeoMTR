package cn.zbx1425.sowcer.batch;

import cn.zbx1425.sowcer.vertex.HackGlState;

/** Additional property affecting rendering process. Set when enqueue. Does not affect batching. */
public class EnqueueProp {

    public static EnqueueProp DEFAULT = new EnqueueProp(null);

    /** The vertex attribute values to use for those specified with VertAttrSrc ENQUEUE. */
    // public VertAttrState attrState;
    public HackGlState hackGlState;

    public EnqueueProp(/*VertAttrState attrState*/ HackGlState hackGlState) {
        // this.attrState = attrState;
        this.hackGlState = hackGlState;
    }
}
