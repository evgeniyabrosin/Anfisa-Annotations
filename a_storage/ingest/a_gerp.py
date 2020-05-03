import csv, json, logging
from datetime import datetime

from .a_util import reportTime, detectFileChrom, extendFileList
#========================================
def new_record(chrom, pos, lst):
    record = dict()
    for idx, item in enumerate(lst):
        record[["GerpN", "GerpRS"][idx]] = (
            float(item) if item != "NA" else None)
    return [("chr" + chrom, pos), record]

#========================================
def processGERP(file_list, chrom_loc = "chr"):
    for chrom_file in extendFileList(file_list):
        chrom = detectFileChrom(chrom_loc, chrom_file)
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
if __name__ == '__main__':
    for key, record in processGERP(file_list =
            "/home/trifon/work/MD/data_ex/Gerp/chr1.maf.rates"):
        print(json.dumps({"key": list(key)},
            ensure_ascii = False, sort_keys = True))
        print(json.dumps(record,
            ensure_ascii = False, sort_keys = True))
