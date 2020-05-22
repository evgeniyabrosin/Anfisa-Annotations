import abc
#===============================================
class _BlockAgent:
    def __init__(self, master_io):
        self.mIO = master_io

    def getIO(self):
        return self.mIO

    def isWriteMode(self):
        return self.mIO.isWriteMode()

    def getAllColumnSeq(self):
        return self.mIO.getMainColumnSeq()

    def _getProperty(self, key, default_value = None):
        return self.mIO._getProperty(key, default_value)

    def properAccess(self):
        return True

    @abc.abstractmethod
    def getType(self):
        return None

    @abc.abstractmethod
    def createWriteBlock(self, encode_env, key, codec):
        return None

    @abc.abstractmethod
    def createReadBlock(self, decode_env_class, key, codec, last_pos = None):
        return None

    def updateWStat(self):
        pass

    def close(self):
        self.updateWStat()

    def normalizeSample(self, key, record):
        return record

#===============================================
class BlockerIdle(_BlockAgent):
    def __init__(self, master_io):
        _BlockAgent.__init__(self, master_io)

    def getType(self):
        return "idle"

    def properAccess(self):
        return False

    def createWriteBlock(self, encode_env, key, codec):
        assert False

    def createReadBlock(self, decode_env_class, key, codec, last_pos = None):
        assert False
