from .a_blk_cluster import ABlockerCluster
from .a_blk_segment import ABlockerSegment
from .a_blk_frames import ABlockerFrameIndex
#===============================================
sBlockerTypes = {
    "cluster":      ABlockerCluster,
    "segment":      ABlockerSegment,
    "frame":        ABlockerFrameIndex
}

def createBlocker(schema, properties, key_codec_type):
    global sBlockerTypes
    return sBlockerTypes[properties["block-type"]](
        schema, properties, key_codec_type)
