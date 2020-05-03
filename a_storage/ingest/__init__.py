from .a_gerp import processGERP
from .s_gerp import SCHEMA_GERP
from .s_splice_ai import SCHEMA_SPLICE_AI
from .a_spliceai import processSpliceAI
from .a_dbnsfp4 import processDBNSFP4
from .s_dbnsfp import SCHEMA_DBNSFP_4
from .a_gnomad211 import processGNOMAD211
from .s_gnomad import SCHEMA_GNOMAD_2_1
#========================================
sIngestModes = {
    "Gerp":     [SCHEMA_GERP,       processGERP],
    "gnomAD":   [SCHEMA_GNOMAD_2_1, processGNOMAD211],
    "dbNSFP":   [SCHEMA_DBNSFP_4,   processDBNSFP4],
    "SpliceAI": [SCHEMA_SPLICE_AI,  processSpliceAI]
}

#========================================
def getIngestModeData(mode):
    global sIngestModes
    return sIngestModes[mode]
