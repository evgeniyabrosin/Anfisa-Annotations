import abc
#===============================================
class _Blocker:
    def __init__(self, master, properties):
        self.mMaster = master
        self.mProperties = properties
        self.mDescr = dict()
        self._getProperty("type")
        self.mOnDuty = False

    def _getProperty(self, name, default_value = None):
        if name in self.mDescr:
            return self.mDescr[name]
        if name in self.mProperties:
            self.mDescr[name] = self.mProperties[name]
        else:
            self.mDescr[name] = default_value
        return self.mDescr[name]

    def _updateProperty(self, key, val):
        self.mDescr[key] = val

    def _onDuty(self):
        assert not self.mOnDuty
        unused = set(self.mProperties.keys()) - set(self.mDescr.keys())
        assert len(unused) == 0, (
            "Lost option(s) for blocker: %s"
            % (self.mMaster.getName(), ", ".join(sorted(unused))))
        self.mOnDuty = True

    def getDescr(self):
        return self.mDescr

    @abc.abstractmethod
    def getType(self):
        return
