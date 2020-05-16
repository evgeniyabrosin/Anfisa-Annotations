import logging

from .a_schema import ASchema
from .a_fasta_schema import AFastaSchema
#=====================================
class AArray:
    def __init__(self, storage, name, descr):
        self.mStorage = storage
        self.mName = name
        self.mDescr = descr
        self.mSchemaSeq = []
        self.mFilteringSet = set()
        self.mUseLastPos = False
        db_key_type = None
        for schema_info in self.mDescr:
            schema_name = schema_info["schema"]
            if schema_name == "fasta":
                schema_h = AFastaSchema(self.mStorage, schema_name,
                    schema_info.get("dbname", schema_name))
            else:
                schema_h = ASchema(self.mStorage, schema_name,
                    schema_info.get("dbname", schema_name))
            self.mFilteringSet |= set(schema_h.getFilteringProperties())
            self.mUseLastPos |= schema_h.useLastPos()
            self.mSchemaSeq.append(schema_h)
            if db_key_type is not None:
                assert schema_h.getDBKeyType() == db_key_type, (
                    "Conflict dbkeys in %s: %s/%s" % (self.mName,
                    schema_h.getDBKeyType(), str(db_key_type)))
            db_key_type = schema_h.getDBKeyType()
            logging.info("Activate schema %s in %s"
                % (schema_name, self.mName))

    def request(self, rq_args):
        chrom, str_pos = rq_args["loc"].split(':')
        if not chrom.startswith("chr"):
            chrom = "chr" + chrom
        if '-' in str_pos:
            assert self.mUseLastPos, (
                "Array %s: last position in diapason not supported: %s"
                % (self.mName, rq_args["loc"]))
            vpos, _, vlast = str_pos.partition('-')
            pos, last_pos = sorted([int(vpos), int(vlast)])
        else:
            pos, last_pos = int(str_pos), None
        ret = {"chrom": chrom, "array": self.mName}
        if last_pos is not None:
            ret["start"] = pos
            ret["end"] = last_pos
        else:
            ret["pos"] = pos
        filtering = dict()
        for flt_name in self.mFilteringSet:
            flt_val = rq_args.get(flt_name)
            if flt_val:
                filtering[flt_name] = flt_val
                ret[flt_name] = flt_val
        for schema_h in self.mSchemaSeq:
            rec_data = schema_h.getRecord((chrom, pos), filtering, last_pos)
            if rec_data is not None:
                ret[schema_h.getName()] = rec_data
        return ret
