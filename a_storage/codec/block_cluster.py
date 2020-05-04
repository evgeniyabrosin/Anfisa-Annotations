from ._block import _Blocker
#===============================================
class BlockerCluster(_Blocker):
    def __init__(self, master, properties):
        _Blocker.__init__(self, master, properties)

    def getType(self):
        return "cluster"
