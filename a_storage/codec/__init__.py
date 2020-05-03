from ._codec import _Codec
from .codec_num import CodecNum
from .codec_str import CodecStr
from .codec_list import CodecList
from .codec_dict import CodecDict
from .codec_agroup import CodecAGroup
from .hg_key import Conv_HG19, Conv_HG38

#===============================================
sCodecs = {
    "num": CodecNum,
    "str": CodecStr,
    "list": CodecList,
    "dict": CodecDict,
    "attr-group": CodecAGroup
}

sKeyCodecs = {
    "hg19": Conv_HG19,
    "hg38": Conv_HG38
}
#===============================================
def createCodec(master, parent, schema_instr, default_name):
    global sCodecs
    return sCodecs[schema_instr["tp"]](
        master, parent, schema_instr, default_name)

def getKeyCodec(name):
    global sKeyCodecs
    return sKeyCodecs[name]


#===============================================
_Codec.sCreateFunc = createCodec
