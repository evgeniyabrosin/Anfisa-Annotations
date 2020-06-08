import logging

from codec.hg_key import ALL_CHROM_SET
from .a_schema import ASchema
#=====================================
class ASegmentedSchema:
    def __init__(self, storage, name, segments):
        self.mStorage = storage
        self.mName = name
        self.mSegSchemas = [ASchema(self.mStorage, self.mName, dbname)
            for dbname in segments]
        self.mChrom2Schema = dict()
        self.mStorage.regActivator(self)

    def activate(self):
        for schema_h in self.mSegSchemas:
            for chrom in ALL_CHROM_SET:
                if schema_h.locateChrom(chrom):
                    assert chrom not in self.mChrom2Schema
                    self.mChrom2Schema[chrom] = schema_h
        logging.info("Segmented schema %s stats with %d chromosomes support"
            % (self.mName, len(self.mChrom2Schema)))

    def flush(self):
        for schema_h in self.mSegSchemas:
            schema_h.flush()

    def keepSchema(self):
        pass

    def keepSamples(self):
        pass

    def close(self):
        for schema_h in self.mSegSchemas:
            schema_h.close()
        del self.mChrom2Schema

    def getStorage(self):
        return self.mStorage

    def getName(self):
        return self.mName

    def isWriteMode(self):
        return False

    def getProperty(self, name):
        return None

    def getTotal(self):
        return sum(schema_h.getTotal() for schema_h in self.mSegSchemas)

    def getIO(self):
        return None

    def getSchemaDescr(self):
        return None

    def useLastPos(self):
        return self.mSegSchemas[0].useLastPos()

    def getDBKeyType(self):
        return self.mSegSchemas[0].getDBKeyType()

    def getFilteringProperties(self):
        return self.mSegSchemas[0].getFilteringProperties()

    def isOptionRequired(self, opt):
        return any(schema_h.isOptionRequired(opt)
            for schema_h in self.mSegSchemas)

    def checkSamples(self, output_stream = None):
        for schema_h in self.mSegSchemas:
            if output_stream is not None:
                print("Check samples for sub-schema %s"
                    % schema_h.getIO().getDbName(), file = output_stream)
            schema_h.checkSamples(output_stream)

    def getRecord(self, key, filtering = None, last_pos = None):
        schema_h = self.mChrom2Schema.get(key[0])
        if schema_h is None:
            return None
        return schema_h.getRecord(key, filtering, last_pos)
