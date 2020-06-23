import bz2

#===============================================
sConvertors = dict()

#===============================================
class PosSeqSupport:
    @staticmethod
    def toBytes(pos_seq):
        xseq = [0, 0]
        for idx in range(len(pos_seq) - 1, 0, -1):
            delta = pos_seq[idx] - pos_seq[idx - 1] - 1
            if xseq[-1] == delta and xseq[-2] < 255:
                xseq[-2] += 1
            else:
                xseq += [1, delta]
        if len(xseq) > 2 and xseq[0] == 0:
            assert xseq[1] == 0
            del xseq[:2]
        return b''.join(n.to_bytes(1, 'big') for n in xseq)

    @staticmethod
    def fromBytes(xbytes):
        pseq = [0]
        for idx in range(0, len(xbytes), 2):
            cnt, delta = xbytes[idx], xbytes[idx + 1]
            for _ in range(cnt):
                pseq.append(pseq[-1] - delta - 1)
        return pseq[-1::-1]


sConvertors["PosSeq"] = PosSeqSupport
#===============================================
class StrSupport:
    @staticmethod
    def toBytes(text):
        return bytes(text, encoding = "utf-8")

    @staticmethod
    def fromBytes(xbytes):
        return xbytes.decode(encoding = "utf-8")


sConvertors["str"] = StrSupport
#===============================================
class BZ2Support:
    @staticmethod
    def toBytes(text):
        return bz2.compress(StrSupport.toBytes(text))

    @staticmethod
    def fromBytes(xbytes):
        return StrSupport.fromBytes(bz2.decompress(xbytes))


sConvertors["bz2"] = BZ2Support
#===============================================
class BinSupport:
    @staticmethod
    def toBytes(xbytes):
        return xbytes

    @staticmethod
    def fromBytes(xbytes):
        return xbytes


sConvertors["bin"] = BinSupport
#===============================================
class BytesFieldsSupport:
    def __init__(self, conv_code_seq,
            stat_max_seq = None, stat_sum_seq = None):
        self.mConvSeq = []
        self.mStatMaxSeq = stat_max_seq
        self.mStatSumSeq = stat_sum_seq
        for conv_code in conv_code_seq:
            self.addConv(conv_code)

    def getStatMaxSeq(self):
        return self.mStatMaxSeq

    def getStatSumSeq(self):
        return self.mStatSumSeq

    def addConv(self, conv_code):
        global sConvertors
        self.mConvSeq.append(sConvertors[conv_code])
        if self.mStatMaxSeq is not None:
            if len(self.mStatMaxSeq) < len(self.mConvSeq):
                self.mStatMaxSeq.append(0)
                self.mStatSumSeq.append(0)
            assert len(self.mStatMaxSeq) == len(self.mStatSumSeq)

    def pack(self, seq_data):
        xseq = [conv.toBytes(data)
                for conv, data in zip(self.mConvSeq, seq_data)]
        assert len(xseq) == len(self.mConvSeq)
        len_header = [len(xdata) for xdata in xseq[:-1]]
        if self.mStatMaxSeq is not None:
            for idx, xdata in enumerate(xseq):
                p_size = len(xdata)
                self.mStatSumSeq[idx] += p_size
                if p_size > self.mStatMaxSeq[idx]:
                    self.mStatMaxSeq[idx] = p_size
        return b''.join([ll.to_bytes(4, 'big') for ll in len_header] + xseq)

    def unpack(self, xbytes):
        pos = 0
        len_header = []
        for _ in range(len(self.mConvSeq) - 1):
            len_header.append(int.from_bytes(xbytes[pos: pos + 4], 'big'))
            pos += 4
        ret = []
        for ll, conv in zip(len_header, self.mConvSeq[:-1]):
            ret.append(conv.fromBytes(xbytes[pos: pos + ll]))
            pos += ll
        ret.append(self.mConvSeq[-1].fromBytes(xbytes[pos:]))
        return ret
