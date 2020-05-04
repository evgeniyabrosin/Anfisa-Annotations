from ._block import _Blocker
#===============================================
class BlockerSegment(_Blocker):
    def __init__(self, master, properties):
        _Blocker.__init__(self, master, properties)

    def getType(self):
        return "segment"
