import sys, gzip, logging
from datetime import datetime

from .a_util import reportTime, detectFileChrom, extendFileList, dumpReader
#========================================
VARIANT_TAB = [
    ("REF",                             str,    3),
    ("ALT",                             str,    4),
    ("MutationTaster_score",            str,    53),
    ("MutationTaster_pred",             str,    55),
    ("PrimateAI_pred",                  str,    92),
    ("CADD_raw",                        float,  102),
    ("CADD_phred",                      float,  104),
    ("DANN_score",                      float,  105),
    ("DANN_rankscore",                  float,  106),
    ("Eigen_raw_coding",                float,  114),
    ("Eigen_raw_coding_rankscore",      float,  115),
    ("Eigen_phred_coding",              float,  116),
    ("Eigen_PC_raw_coding",             float,  117),
    ("Eigen_PC_raw_coding_rankscore",   float,  118),
    ("Eigen_PC_phred_coding",           float,  119),
    ("GTEx_V7_gene",                    str,    374),
    ("GTEx_V7_tissue",                  str,    375),
    ("Geuvadis_eQTL_target_gene",       str,    376)
]

#========================================
FACET_TAB = [
    ("refcodon",                    str,    30),
    ("codonpos",                    str,    31),
    ("SIFT4G_converted_rankscore",  float,  41),
    ("MetaLR_score",                float,  72),
    ("MetaLR_rankscore",            float,  73),
    ("MetaLR_pred",                 str,    74),
    ("REVEL_score",                 float,  79),
    ("MutPred_score",               str,    81),
    ("MutPred_rankscore",           float,  82),
    ("MutPred_protID",              str,    83),
    ("MutPred_AAchange",            str,    84),
    ("MutPred_Top5features",        str,    85),
    ("MPC_rankscore",               float,  89),
    ("PrimateAI_score",             float,  90),
    ("PrimateAI_rankscore",         float,  91)
]

#========================================
TRANSCRIPT_TAB = [
    ("Ensembl_geneid",          str,    14),
    ("Ensembl_transcriptid",    str,    15),
    ("Ensembl_proteinid",       str,    16),
    ("Uniprot_acc",             str,    17),
    ("HGVSc_ANNOVAR",           str,    19),
    ("HGVSp_ANNOVAR",           str,    20),
    ("HGVSc_snpEff",            str,    21),
    ("HGVSp_snpEff",            str,    22),
    ("GENCODE_basic",           str,    26),
    ("SIFT_score",              float,  37),
    ("SIFT_pred",               str,    39),
    ("SIFT4G_score",            float,  40),
    ("SIFT4G_pred",             str,    42),
    ("Polyphen2_HDIV_score",    float,  43),
    ("Polyphen2_HDIV_pred",     str,    45),
    ("Polyphen2_HVAR_score",    float,  46),
    ("Polyphen2_HVAR_pred",     str,    48),
    ("MutationAssessor_score",  float,  58),
    ("MutationAssessor_pred",   str,    60),
    ("FATHMM_score",            float,  61),
    ("FATHMM_pred",             str,    63),
    ("MPC_score",               float,  88)
]

#========================================
def iterFields(fields, properties_tab):
    for name, tp, idx in properties_tab:
        val = fields[idx - 1]
        if val == '.':
            yield name, None
        else:
            yield name, tp(val)

def iterDeepFields(fields, properties_tab):
    for name, tp, idx in properties_tab:
        val_seq = []
        for val in fields[idx-1].split(';'):
            if val == '.':
                val_seq.append(None)
            else:
                val_seq.append(tp(val))
        yield name, val_seq

#========================================
class DataCollector:
    def __init__(self):
        self.mCounts = [0, 0, 0]
        self.mCurRecord = None

    def getCounts(self):
        return self.mCounts

    def ingestLine(self, line):
        global VARIANT_TAB, FACET_TAB, TRANSCRIPT_TAB
        if line.endswith('\n'):
            line = line[:-1]
        fields = line.split('\t')
        chrom = "chr" + str(fields[0])
        pos = int(fields[1])
        new_record = False
        if self.mCurRecord is None or (chrom, pos) != self.mCurRecord[0]:
            new_record = True
        new_variant = new_record

        var_data = dict()
        for name, val in iterFields(fields, VARIANT_TAB):
            var_data[name] = val
            if not new_variant and val != self.mCurRecord[1][-1][name]:
                new_variant = True

        facet_data = {name: val
            for name, val in iterFields(fields, FACET_TAB)}

        tr_data_seq = None
        for name, val_seq in iterDeepFields(fields, TRANSCRIPT_TAB):
            if tr_data_seq is None:
                tr_data_seq = [{name: val} for val in val_seq]
            else:
                for idx, val in enumerate(val_seq):
                    tr_data_seq[idx][name] = val
        if tr_data_seq is None:
            tr_data_seq = []
        facet_data["transcripts"] = tr_data_seq
        self.mCounts[2] += len(tr_data_seq)
        self.mCounts[1] += 1

        ret = None
        if new_record:
            self.mCounts[0] += 1
            var_data["facets"] = [facet_data]
            ret, self.mCurRecord = self.mCurRecord, [(chrom, pos), [var_data]]
        elif new_variant:
            self.mCounts[0] += 1
            var_data["facets"] = [facet_data]
            self.mCurRecord[1].append(var_data)
        else:
            self.mCurRecord[1][-1]["facets"].append(facet_data)

        return ret

    def finishUp(self):
        return self.mCurRecord

#========================================
#========================================
class ReaderDBNSFP4:
    def __init__(self, file_list, chrom_loc = "chr"):
        self.mFiles = extendFileList(file_list)
        self.mChromLoc = chrom_loc

    def read(self):
        exceptions = 0
        for chrom_file in self.mFiles:
            chrom = detectFileChrom(self.mChromLoc, chrom_file)
            print("Evaluation of", chrom, "in", chrom_file)
            with gzip.open(chrom_file, 'rt') as text_inp:
                start_time = datetime.now()
                collector = DataCollector()
                for line_no, line in enumerate(text_inp):
                    if line_no == 0:
                        continue
                    try:
                        info = collector.ingestLine(line)
                        if info is not None:
                            yield info
                        if (line_no % 10000) == 0:
                            total_var, _, _ = collector.getCounts()
                            reportTime("", total_var, start_time)
                    except IndexError:
                        exceptions += 1
                info = collector.finishUp()
                if info:
                    yield info
                total_var, total_facets, total_tr = collector.getCounts()
                reportTime("Done (transripts:", total_var, start_time)
                logging.info("transcripts: %d, facets: %d, exceptions: %d"
                    % (total_tr, total_facets, exceptions))

#========================================
def reader_dbNSFP4(properties):
    return ReaderDBNSFP4(
        properties["file_list"],
        properties.get("chrom_loc", "chr"))


#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    reader = reader_dbNSFP4({"file_list": sys.argv[1]})
    dumpReader(reader)
