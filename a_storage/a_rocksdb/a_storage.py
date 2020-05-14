import os, shutil, gc

from .a_connector import AConnector
#========================================
class AStorage:
    def __init__(self, config, dummy_mode = False):
        self.mConfig = config
        self.mDummyMode = dummy_mode
        self.mConnectors = dict()

    def getDbOptions(self):
        return self.mConfig["db-options"].items()

    def getDefaulColumnAttrs(self, column_type):
        return self.mConfig["col-options"][column_type]

    def getDBFilePath(self, name):
        return self.mConfig["db-dir"] + '/' + name

    def getSchemaFilePath(self, name):
        return self.mConfig["schema-dir"] + '/' + name

    def getSamplesCount(self):
        return self.mConfig["samples-count"]

    def getLoadKeepSchemaSec(self):
        return self.mConfig["load-keep-schema-sec"]

    def getConnector(self, aspect_name):
        return self.mConnectors.get(aspect_name)

    def isDummyMode(self):
        return self.mDummyMode

    def openConnection(self, name, write_mode):
        if name not in self.mConnectors:
            self.mConnectors[name] = (
                AConnector(self, name, write_mode))
        ret = self.mConnectors[name]
        ret._incRefCount()
        assert ret.getWriteMode() == write_mode
        return ret

    def closeConnection(self, connector_h):
        assert connector_h is self.mConnectors[connector_h.getName()]
        if connector_h._decRefCount() <= 0:
            del self.mConnectors[connector_h.getName()]
            connector_h.close()
            del connector_h

    def activate(self):
        for connector_h in self.mConnectors.values():
            connector_h.activate()

    def deactivate(self):
        while len(self.mConnectors) > 0:
            self.closeConnection(list(self.mConnectors.values())[-1])
        gc.collect()

    def dropDB(self, db_name):
        done = False
        schema_fdir = self.getSchemaFilePath(db_name)
        if os.path.exists(schema_fdir):
            shutil.rmtree(schema_fdir)
            done = True
        db_fdir = self.getDBFilePath(db_name)
        if os.path.exists(db_fdir):
            shutil.rmtree(db_fdir)
            done = True
        return done
