from .a_blk_segment import ABlockerIO_Segment
from .a_blk_cluster import ABlockerIO_PageCluster
from .a_blk_frames import ABlockerIO_FrameIndex
#===============================================
sBlockerTypes = {
    "segment":      ABlockerIO_Segment,
    "page-cluster": ABlockerIO_PageCluster,
    "frame":        ABlockerIO_FrameIndex
}

def createBlocker(schema, properties, key_codec_type):
    global sBlockerTypes
    return sBlockerTypes[properties["block-type"]](
        schema, properties, key_codec_type)
