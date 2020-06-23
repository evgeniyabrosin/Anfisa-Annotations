#  Copyright (c) 2019. Partners HealthCare and other members of
#  Forome Association
#
#  Developed by Sergey Trifonov based on contributions by Joel Krier,
#  Michael Bouzinier, Shamil Sunyaev and other members of Division of
#  Genetics, Brigham and Women's Hospital
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
import json
from pyliftover import LiftOver
#===============================================
class ACollect:
    def __init__(self, arrays):
        self.mSchemaSeq_hg19 = []
        self.mSchemaSeq_hg38 = []
        for array_h in arrays.values():
            if array_h.getDBKeyType() == "hg19":
                for schema_h in array_h.iterSchemaSeq():
                    self.mSchemaSeq_hg19.append(schema_h)
            elif array_h.getDBKeyType() == "hg38":
                for schema_h in array_h.iterSchemaSeq():
                    self.mSchemaSeq_hg38.append(schema_h)
        self.mSchemaNames_hg19 = {schema_h.getName()
            for schema_h in self.mSchemaSeq_hg19}
        self.mSchemaNames_hg38 = {schema_h.getName()
            for schema_h in self.mSchemaSeq_hg38}
        self.mAllSchemaNames = self.mSchemaNames_hg19 | self.mSchemaNames_hg38
        self.mLO_19_38 = LiftOver('hg19', 'hg38')
        self.mLO_38_19 = LiftOver('hg38', 'hg19')

    def _conv19_38(self, chrom, pos):
        coord = self.mLO_19_38.convert_coordinate(chrom, pos - 1)
        if not coord:
            return None
        return coord[0][1] + 1

    def _conv38_19(self, chrom, pos):
        coord = self.mLO_38_19.convert_coordinate(chrom, pos - 1)
        if not coord:
            return None
        return coord[0][1] + 1

    def request(self, rq_args, rq_descr):
        if "@request" in rq_args:
            rq_info = rq_args["@request"]
            variants = rq_info["variants"]
            fasta = rq_info.get("fasta", "hg38")
            arrays = rq_info.get("arrays", self.mAllSchemaNames)
        else:
            variants = json.loads(rq_args["variants"])
            fasta = rq_args.get("fasta", "hg38")
            if "arrays" in rq_args:
                arrays = json.loads(rq_args["arrays"])
            else:
                arrays = self.mAllSchemaNames
        result = []
        for var_info in variants:
            chrom, pos = var_info["chrom"], var_info["pos"]
            ret = {"chrom": chrom, "pos": pos}
            result.append(ret)
            filtering = dict()
            for key in ("alt", "ref"):
                val = var_info.get(key)
                if val:
                    ret[key] = val
                    filtering[key] = val
            if len(filtering) == 0:
                filtering = None
            if len(arrays & self.mSchemaNames_hg19) > 0:
                if fasta == "hg38":
                    _pos = self._conv38_19(chrom, pos)
                    ret["hg19"] = _pos
                else:
                    _pos = pos
                if _pos is not None:
                    for schema_h in self.mSchemaSeq_hg19:
                        if schema_h.getName() not in arrays:
                            continue
                        rec_data = schema_h.getRecord((chrom, _pos), filtering)
                        if rec_data is not None:
                            ret[schema_h.getName()] = rec_data
            if len(arrays & self.mSchemaNames_hg38) > 0:
                if fasta == "hg19":
                    _pos = self._conv19_38(chrom, pos)
                    ret["hg38"] = _pos
                else:
                    _pos = pos
                if _pos is not None:
                    for schema_h in self.mSchemaSeq_hg38:
                        if schema_h.getName() not in arrays:
                            continue
                        rec_data = schema_h.getRecord((chrom, _pos), filtering)
                        if rec_data is not None:
                            ret[schema_h.getName()] = rec_data
        return result
