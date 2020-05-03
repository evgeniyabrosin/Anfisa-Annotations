import logging

from .a_schema import ASchema
#=====================================
class AArray:
    def __init__(self, storage, name, descr):
        self.mStorage = storage
        self.mName = name
        self.mDescr = descr
        self.mSchemaSeq = []
        self.mFilteringSet = set()
        db_key_type = None
        for schema_info in self.mDescr:
            schema_name = schema_info["schema"]
            schema_h = ASchema(self.mStorage, schema_name,
                schema_info.get("dbname", schema_name), write_mode = False)
            self.mFilteringSet |= set(schema_h.getFilteringProperties())
            self.mSchemaSeq.append(schema_h)
            if db_key_type is not None:
                assert schema_h.getDBKeyType() ==  db_key_type, (
                "Conflict dbkeys in %s: %s/%s" %(self.mName,
                schema_h.getDBKeyType(), str(db_key_type)))
            db_key_type = schema_h.getDBKeyType()
            logging.info("Activate schema %s in %s"
                % (schema_name, self.mName))

    def request(self, rq_args):
        chrom, pos = rq_args["loc"].split(':')
        if not chrom.startswith("chr"):
            chrom = "chr" + chrom
        pos = int(pos)
        ret = {"chrom": chrom, "pos": pos, "array": self.mName}
        filtering = dict()
        for flt_name in self.mFilteringSet:
            flt_val = rq_args.get(flt_name)
            if flt_val:
                filtering[flt_name] = flt_val
        for schema_h in self.mSchemaSeq:
            rec_data = schema_h.getRecord((chrom, pos), filtering)
            if rec_data is not None:
                ret[schema_h.getName()] = rec_data
        return ret
