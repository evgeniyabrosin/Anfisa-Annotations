import logging, time, json
from datetime import datetime
from threading import Thread
from os.path import abspath, dirname
from argparse import ArgumentParser
from random import Random

from forome_tools.json_conf import loadJSonConfig
from forome_tools.rest import RestAgent
#=====================================
logging.root.setLevel(logging.INFO)

parser = ArgumentParser()
parser.add_argument("-c", "--config",
    help = "AStrorage configuration file",
    default = "astorage.cfg")
parser.add_argument("-m", "--mode", default = "simple",
    help = 'Modes: simple/bulk')
parser.add_argument("-s", "--schema", help = "Schema name")
parser.add_argument("-a", "--array", help = "Array name")
parser.add_argument("-u", "--url", help = "Service url",
    default = "http://localhost:8290/")
parser.add_argument("--threads", type = int, default = 5,
    help = "Count of threads")
parser.add_argument("--portions", type = int, default = 10,
    help = "Max portion of records")
parser.add_argument("--tasks", type = int, default = 1000,
    help = "Count of tasks per thread")
args = parser.parse_args()
config = loadJSonConfig(args.config,
    home_path = dirname(abspath(__file__)))

date_started = datetime.now()
logging.info("Started at " + str(date_started))
#=====================================
class SchemaSmpH:
    def __init__(self, config, array_name, schema_name, db_name):
        self.mArrayName = array_name
        self.mSchemaName = schema_name
        self.mDbName = db_name
        self.mSamples = []
        with open(config["schema-dir"] + "/" + self.mDbName
                + "/" + self.mSchemaName + ".1.samples",
                "r", encoding = "utf-8") as inp:
            cur_key, cur_no = None, None
            for line in inp:
                rec = json.loads(line)
                if cur_key is not None:
                    self.mSamples.append([cur_key, rec, cur_no])
                    cur_key = None
                else:
                    cur_key, cur_no = rec["key"], rec["no"]
            assert cur_key is None
        logging.info("Schema samples loaded for %s/%s: %d samples"
            % (self.mArrayName, self.mSchemaName, len(self.mSamples)))

    def __len__(self):
        return len(self.mSamples)

    def getKey(self, idx):
        return self.mSamples[idx][0]

    def formRequest(self, idx):
        chrom, pos = self.mSamples[idx][0]
        return 'get?array=%s&loc=%s:%d' % (self.mArrayName, chrom, pos)

    def testIt(self, idx, test_rec):
        it_no = self.mSamples[idx][2]
        if test_rec.get(self.mSchemaName) is None:
            logging.error("At smp %s[%s]: None" % (self.mSchemaName, it_no))
            return False
        presentations = [json.dumps(rec,
            sort_keys = True, ensure_ascii = False)
            for rec in (self.mSamples[idx][1], test_rec[self.mSchemaName])]
        if presentations[0] == presentations[1]:
            return True
        logging.error("At smp %s[%s]: Diff %s"
            % (self.mSchemaName, it_no, presentations[1]))
        return False


#=====================================
sSmpSeq = []

for array_name, array_info in config["service"]["arrays"].items():
    if args.array and array_name != args.array:
        continue
    if array_name == "fasta":
        continue
    for s_info in array_info:
        schema_name = s_info["schema"]
        if args.schema and schema_name != args.schema:
            continue
        sSmpSeq.append(SchemaSmpH(config, array_name, schema_name,
            s_info.get("dbname", schema_name)))

rest_agent = RestAgent(args.url, "AStorage")

#=====================================
class TestRunner(Thread):
    def __init__(self, run_id, smp_seq, rest_agent,
            max_portions, task_count):
        Thread.__init__(self)
        self.mRunId = run_id
        self.mSmpSeq = smp_seq
        self.mRestAgent = rest_agent
        self.mRH = Random(179 + run_id)
        self.mMaxPortions = max_portions
        self.mTaskCount = task_count
        self.mCountAll = 0
        self.mCountBad = 0
        self.start()

    def tryOne(self):
        schema_idx = self.mRH.randint(0, len(self.mSmpSeq) - 1)
        smp_h = self.mSmpSeq[schema_idx]
        smp_from = self.mRH.randint(0, len(smp_h))
        if smp_from + 1 < len(smp_h):
            smp_to = self.mRH.randint(smp_from + 1,
                min(smp_from + self.mMaxPortions, len(smp_h)))
        else:
            smp_to = len(smp_h)
        is_ok = True
        for smp_idx in range(smp_from, smp_to):
            test_rec = self.mRestAgent.call(
                None, "GET", smp_h.formRequest(smp_idx))
            is_ok &= smp_h.testIt(smp_idx, test_rec)
        return is_ok

    def run(self):
        for _ in range(self.mTaskCount):
            is_ok = self.tryOne()
            self.mCountAll += 1
            if not is_ok:
                self.mCountBad += 1
            if self.mCountAll % 100 == 0:
                logging.info("Runner %d: %d bad: %d"
                    % (self.mRunId, self.mCountAll, self.mCountBad))
        logging.info("Done Runner %d: %d bad: %d"
            % (self.mRunId, self.mCountAll, self.mCountBad))


runners = [TestRunner(idx, sSmpSeq, rest_agent, args.portions, args.tasks)
    for idx in range(args.threads)]

while True:
    time.sleep(.01)
    if not any(runner.is_alive() for runner in runners):
        break

n_tasks = sum(runner.mCountAll for runner in runners)
logging.info("Total tasks: %d in %d threads"
    % (n_tasks, args.threads))

date_done = datetime.now()
logging.info("Done at " + str(date_started)
    + " for " + str(date_done - date_started))
