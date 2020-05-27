import os, shutil, gc, json, logging

from .a_connector import AConnector
#========================================
class AStorage:
    sDefaultColumnOptions = {
        "base": {
            "-compress": "bz2",
        },
        "str": {
            "-compress": "bz2",
        },
        "sgidx": {
        },
        "fasta": {
            "-compress": "bz2",
        }
    }

    def __init__(self, config,
            dummy_mode = False, deep_comp_mode = False):
        self.mConfig = config
        self.mDummyMode = dummy_mode
        self.mDeepCompMode = deep_comp_mode
        self.mConnectors = dict()
        self.mActivators = []

    def getDbOptions(self):
        return self.mConfig["db-options"].items()

    def getDefaulColumnOptions(self, column_type):
        return self.sDefaultColumnOptions[column_type]

    def getDBFilePath(self, name):
        return self.mConfig["db-dir"] + '/' + name

    def getSupportFilePath(self, name):
        return self.mConfig["schema-dir"] + '/' + name

    def getSamplesCount(self):
        return self.mConfig["samples-count"]

    def getLoadKeepSchemaSec(self):
        return self.mConfig["load-keep-schema-sec"]

    def getConnector(self, aspect_name):
        return self.mConnectors.get(aspect_name)

    def isDummyMode(self):
        return self.mDummyMode

    def isDeepCompMode(self):
        return self.mDeepCompMode

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

    def regActivator(self, activator_h):
        self.mActivators.append(activator_h)

    def activate(self):
        for connector_h in self.mConnectors.values():
            connector_h.activate()
        for activator_h in self.mActivators:
            activator_h.activate()
        logging.info("Storage activated")

    def deactivate(self):
        while len(self.mConnectors) > 0:
            self.closeConnection(list(self.mConnectors.values())[-1])
        gc.collect()
        logging.info("Storage terminated")

    def newDB(self, db_name):
        if self.isDummyMode():
            return
        self.dropDB(db_name)
        os.mkdir(self.getDBFilePath(db_name))
        os.mkdir(self.getSupportFilePath(db_name))

    def dropDB(self, db_name):
        if self.isDummyMode():
            return False
        done = False
        schema_fdir = self.getSupportFilePath(db_name)
        if os.path.exists(schema_fdir):
            shutil.rmtree(schema_fdir)
            done = True
        db_fdir = self.getDBFilePath(db_name)
        if os.path.exists(db_fdir):
            shutil.rmtree(db_fdir)
            done = True
        return done

    def preLoadSchemaData(self, db_name, schema_name,
            schema_descr, update_mode = False):
        write_mode = (schema_descr is not None)
        supp_fpath = self.getSupportFilePath(db_name)
        if not (write_mode and not update_mode):
            assert os.path.exists(supp_fpath), (
                "No DB for reading (or update):" + supp_fpath)
        schema_fname = supp_fpath + "/" + schema_name + ".json"
        if not write_mode or update_mode:
            assert os.path.exists(schema_fname), (
                "Attempt to read from uninstalled database schema:"
                + schema_fname)
            with open(schema_fname, "r", encoding = "utf-8") as inp:
                return json.loads(inp.read())
        if not self.isDummyMode():
            if os.path.exists(schema_fname):
                os.remove(schema_fname)
        assert schema_descr is not None
        return schema_descr

    def getSchemaFilePath(self, schema_h, file_ext):
        return (self.getSupportFilePath(schema_h.getIO().getDbName())
            + ("/%s.%s" % (schema_h.getName(), file_ext)))

    def saveSchemaData(self, schema_h):
        schema_fname = self.getSchemaFilePath(schema_h, "json")
        with open(schema_fname + ".new", "w", encoding = "utf-8") as outp:
            outp.write(json.dumps(schema_h.getSchemaDescr(), sort_keys = True,
                indent = 4, ensure_ascii = False))
        if os.path.exists(schema_fname):
            os.rename(schema_fname, schema_fname + '~')
        os.rename(schema_fname + ".new", schema_fname)
