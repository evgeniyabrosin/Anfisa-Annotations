from .a_gerp import reader_Gerp
from .s_gerp import SCHEMA_GERP
from .s_splice_ai import SCHEMA_SPLICE_AI
from .a_spliceai import reader_SpliceAI
from .a_dbnsfp4 import reader_dbNSFP4
from .s_dbnsfp import SCHEMA_DBNSFP_4
from .a_gnomad211 import reader_GNOMAD211
from .s_gnomad import SCHEMA_GNOMAD_2_1
#========================================
sIngestModes = {
    "Gerp":     [SCHEMA_GERP,       reader_Gerp],
    "gnomAD":   [SCHEMA_GNOMAD_2_1, reader_GNOMAD211],
    "dbNSFP":   [SCHEMA_DBNSFP_4,   reader_dbNSFP4],
    "SpliceAI": [SCHEMA_SPLICE_AI,  reader_SpliceAI]
}

#========================================
def getIngestModeSetup(mode):
    global sIngestModes
    return sIngestModes[mode]
