import logging, sys
from argparse import ArgumentParser

from forome_tools.json_conf import loadJSonConfig
from a_rocksdb.a_storage import AStorage
from a_rocksdb.a_schema import ASchema
from codec.block_cluster import bytes2pseq
#=====================================
logging.root.setLevel(logging.INFO)

parser = ArgumentParser()
parser.add_argument("-c", "--config",
    help = "AStrorage configuration file",
    default = "astorage.cfg")
parser.add_argument("-m", "--mode", default = "pos",
    help = 'Modes: pos/scan/samples')
parser.add_argument("-s", "--schema", help = "Schema name")
parser.add_argument("-d", "--dbname", help = "db name, "
    "by default equals to  name")
parser.add_argument("--count", type = int, default = 30,
    help = "Count of records in scan regime")
parser.add_argument("-l", "--loc",
    help = "Record location")
args = parser.parse_args()

#=====================================
def prepareIterator(db_conn, col_handler, args):
    x_iter = db_conn.mDB.iterator(db_conn.mRdOpts, col_handler)

    if args.loc:
        chrom, _, pos = args.loc.partition(':')
        key_start = (chrom, int(pos))
        xkey_start = key_codec.encode(key_start)
        x_iter.seek(xkey_start)
    else:
        key_start = None
        x_iter.seek_to_first()
    return x_iter, key_start

#=====================================
config = loadJSonConfig(args.config)
storage = AStorage(config)
schema_h = ASchema(storage, args.schema,
    args.dbname if args.dbname else args.schema)

storage.activate()

db_conn = schema_h.getIO().mDbConnector
columns = schema_h.getIO().getAllColumnSeq()

col_handlers = [db_conn.mColHandlers[db_conn.mColIndex[column_h.getName()]]
    for column_h in columns]

key_codec = schema_h.getIO().mKeyCodec


if args.mode == "pos":
    x_iter, key_start = prepareIterator(db_conn, col_handlers[0], args)

    xkey = x_iter.key()
    key = key_codec.decode(xkey)
    pseq = bytes2pseq(key[1], x_iter.value())

    print("Start=", key_start, "Key=", key, "pseq=", pseq)
    for col_idx, col_h in enumerate(columns):
        if col_idx == 0:
            continue
        blob = db_conn.mDB.get(db_conn.mRdOpts, col_handlers[col_idx], xkey)
        if not blob.status.ok():
            print ("col=", col_h.getName(), "Blob None!")
            continue
        if blob.data is None:
            print ("col=", col_h.getName, "Data None!")
            continue
        val_seq = col_h.decode(blob.data).split('\0')

        print("col=", col_h.getName(), "len=", len(val_seq))
        for idx, v in enumerate(val_seq):
            print("col=", col_h.getName(), "idx=", idx, "val=", v)
elif args.mode == "scan":
    x_iter, key_start = prepareIterator(db_conn, col_handlers[0], args)

    cnt = 0
    while x_iter.valid() and cnt < args.count:
        cnt += 1
        xkey = x_iter.key()
        key = key_codec.decode(xkey)
        pseq = bytes2pseq(key[1], x_iter.value())

        vals = []
        for col_h in col_handlers[1:]:
            blob = db_conn.mDB.get(db_conn.mRdOpts, col_h, xkey)
            if blob.status.ok():
                vals.append(len(blob.data))
            else:
                vals.append(None)
        print(cnt, key, pseq, vals)
        x_iter.next()
elif args.mode == "samples":
    schema_h.checkSamples(sys.stdout)
else:
    assert False, "Bad evaluation mode: " + args.mode

storage.deactivate()
