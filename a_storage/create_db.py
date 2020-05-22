import sys, codecs, logging
from argparse import ArgumentParser

from utils.json_conf import loadJSonConfig
from a_rocksdb.a_storage import AStorage
from a_rocksdb.a_schema import ASchema
from a_rocksdb.a_fasta_schema import AFastaSchema
from a_rocksdb.deep_load import DeepCompLoader
from ingest import getIngestModeSetup
from ingest.a_fasta import getFastaSetup
#=====================================
try:
    sys.stderr = codecs.getwriter('utf8')(sys.stderr.detach())
    sys.stdout = codecs.getwriter('utf8')(sys.stdout.detach())
except Exception:
    pass

#=====================================
if __name__ == '__main__':
    logging.root.setLevel(logging.INFO)

    parser = ArgumentParser()
    parser.add_argument("-c", "--config",
        help = "AStrorage configuration file",
        default = "astorage.cfg")
    parser.add_argument("--metavar", action = "append", nargs = 2,
        help='Meta variables for configuration interpretation')
    parser.add_argument("-d", "--dbname", help = "db name, "
        "by default equals to mode name")
    parser.add_argument("--update",
        action = "store_true", help = "Update mode")
    parser.add_argument("--dummy",
        action = "store_true", help = "Dummy DB mode")
    parser.add_argument("--deepcomp",
        action = "store_true", help = "Deep compilation mode")
    parser.add_argument("--deepstorage",
        help = "Deep compiled storage directory to load")
    parser.add_argument("--deepschema",
        help = "Deep compiled schema directory to load")
    parser.add_argument("-m", "--mode",
        help = "Mode")
    args = parser.parse_args()

    db_config = loadJSonConfig(args.config, args.metavar)
    a_storage = AStorage(db_config,
        dummy_mode = args.dummy, deep_comp_mode = args.deepcomp)

    db_name = args.dbname
    if not db_name:
        db_name = args.mode

    reader_options = db_config["create"][args.mode]

    if args.mode == "fasta":
        schema_cfg, readers = getFastaSetup(reader_options)
        a_schema = AFastaSchema(a_storage, args.mode, db_name, schema_cfg)
        a_storage.activate()
        for reader in readers:
            a_schema.loadReader(reader)
        a_schema.close()
        a_storage.deactivate()
    else:
        schema_cfg, reader_func = getIngestModeSetup(args.mode)
        assert schema_cfg is not None
        a_schema = ASchema(a_storage, args.mode, db_name, schema_cfg,
            update_mode = args.update)
        reader_data, deep_loader = None, None
        if args.deepstorage:
            reader_func(None, a_schema)
            deep_loader = DeepCompLoader(a_schema,
                args.deepstorage, args.deepschema)
        else:
            reader_data = reader_func(reader_options, a_schema)
        a_storage.activate()

        if reader_data is not None:
            for key, record in reader_data.read():
                a_schema.putRecord(key, record)
        else:
            deep_loader.doLoad()
            deep_loader.finishUp()
        a_schema.close()
        a_storage.deactivate()

