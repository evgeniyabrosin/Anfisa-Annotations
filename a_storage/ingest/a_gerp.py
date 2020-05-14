import sys, csv, logging
from datetime import datetime

from .a_util import reportTime, detectFileChrom, extendFileList, dumpReader
#========================================
def new_record(chrom, pos, lst):
    record = dict()
    for idx, item in enumerate(lst):
        record[["GerpN", "GerpRS"][idx]] = (
            float(item) if item != "NA" else None)
    return [("chr" + chrom, pos), record]

#========================================
class ReaderGerp:
    def __init__(self, file_list, chrom_loc = "chr"):
        self.mFiles = extendFileList(file_list)
        self.mChromLoc = chrom_loc

    def read(self):
        for chrom_file in self.mFiles:
            chrom = detectFileChrom(self.mChromLoc, chrom_file)
            logging.info("Evaluation of %s in %s"
                % (chrom, chrom_file))
            with open(chrom_file, 'r') as header:
                position = 0
                reader = csv.reader(header, delimiter = '\t')
                start_time = datetime.now()
                for record in reader:
                    position += 1
                    yield new_record(chrom, position, record)
                    if position % 100000 == 0:
                        reportTime("", position, start_time)
                reportTime("Done:", position, start_time)

#========================================
def reader_Gerp(properties, schema_h = None):
    return ReaderGerp(
        properties["file_list"],
        properties.get("chrom_loc", "chr"))


#========================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)
    reader = reader_Gerp({"file_list": sys.argv[1]})
    dumpReader(reader)
