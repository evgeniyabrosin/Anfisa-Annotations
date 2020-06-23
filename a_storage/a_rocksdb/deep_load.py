import os, json
from glob import glob
from .deep_comp import DeepCompReader
from codec.block_support import BytesFieldsSupport
from ingest.in_util import TimeReport

#========================================
class DeepCompLoader:
    def __init__(self, schema_h, dir_data, dir_support):
        self.mSchemaH = schema_h
        self.mDirData = dir_data
        self.mDirSupport = dir_support
        assert not self.mSchemaH.isOptionRequired("ext-dict"), (
            "Deep compilation regime does not support option dict for strings")
        self.mParts = sorted(os.path.basename(os.path.dirname(fname))
            for fname in glob(dir_data + "/*/data.bin"))

    def doLoad(self):
        block_io = self.mSchemaH.getBlockIO()
        col_name = block_io.getColumnH().getName()
        for part_name in self.mParts:
            time_rep = TimeReport("Deep part " + part_name)
            reader = DeepCompReader(
                self.mDirData + "/" + part_name + "/data.bin")
            if reader.isLegacyMode():
                bytes_supp = BytesFieldsSupport(
                    ["bin"] * len(reader.getColumnNames()))
            else:
                bytes_supp = None
                assert reader.getColumnNames() == [col_name]
            cnt = 0
            while True:
                xkey, data_seq = reader.readOne()
                if xkey is None:
                    break
                if bytes_supp is not None:
                    xdata = bytes_supp.pack(data_seq)
                else:
                    xdata = data_seq[0]
                block_io._putData(
                    block_io.decodeXKey(xkey), xdata, use_encode = False)
                cnt += 1
                if cnt % 10000 == 0:
                    time_rep.portion(cnt)
            time_rep.done(cnt)

    def finishUp(self):
        for part_name in self.mParts:
            inp_names = glob(
                self.mDirSupport + '/' + part_name + "/*.0.samples")
            assert len(inp_names) == 1
            with open(inp_names[0], "r", encoding = "utf-8") as inp:
                while True:
                    line_rep = inp.readline()
                    if not line_rep:
                        break
                    rep_obj = json.loads(line_rep)
                    record = json.loads(inp.readline())
                    self.mSchemaH._addDirectSample(
                        tuple(rep_obj["key"]), record)

    def _finishUp(self):
        counts = []
        for ext in ("0.samples", "1.samples"):
            with open(self.mSchemaH.getStorage().getSchemaFilePath(
                    self.mSchemaH, ext), "w", encoding = "utf-8") as output:
                cnt = 0
                for part_name in self.mParts:
                    inp_names = glob(
                        self.mDirSupport + '/' + part_name + "/*." + ext)
                    assert len(inp_names) == 1
                    with open(inp_names[0], "r", encoding = "utf-8") as inp:
                        while True:
                            line_rep = inp.readline()
                            if not line_rep:
                                break
                            rep_obj = json.loads(line_rep)
                            rec_line = inp.readline().rstrip()
                            rep_obj["no"] = cnt + 1
                            cnt += 1
                            print(json.dumps(rep_obj, ensure_ascii = False),
                                file = output)
                            print(rec_line, file = output)
            counts.append(cnt)
        assert counts[0] == counts[1]
        self.mSchemaH.checkSamples()
