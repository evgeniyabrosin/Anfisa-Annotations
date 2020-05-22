from ._codec_data import _CodecData
from .codec_num import CodecNum
from .codec_str import CodecStr
from .codec_list import CodecList
from .codec_dict import CodecDict
from .codec_agroup import CodecAGroup
from ._block_agent import BlockerIdle
from .block_segment import BlockerSegment
from .block_cluster import BlockerCluster
from .block_frames import BlockerFrameIndex
from .hg_key import Conv_HG19, Conv_HG38, Conv_ID

#===============================================
sDataCodecs = {
    "num": CodecNum,
    "str": CodecStr,
    "list": CodecList,
    "dict": CodecDict,
    "attr-group": CodecAGroup
}

sKeyCodecs = {
    "hg19": Conv_HG19,
    "hg38": Conv_HG38,
    "id":   Conv_ID
}

sBlockCodecs = {
    "cluster":      BlockerCluster,
    "segment":      BlockerSegment,
    "frame-idx":    BlockerFrameIndex,
    "idle":         BlockerIdle
}
#===============================================
def createDataCodec(master, parent, schema_instr, default_name):
    global sDataCodecs
    return sDataCodecs[schema_instr["tp"]](
        master, parent, schema_instr, default_name)

def getKeyCodec(name):
    global sKeyCodecs
    return sKeyCodecs[name]

def createBlockCodec(master, blocker_type):
    global sBlockCodecs
    return sBlockCodecs[blocker_type](master)


#===============================================
_CodecData.sCreateFunc = createDataCodec
