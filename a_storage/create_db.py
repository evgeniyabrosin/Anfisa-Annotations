import sys, codecs, logging
from argparse import ArgumentParser

from utils.json_conf import loadJSonConfig
from a_rocksdb.a_storage import AStorage
from a_rocksdb.a_schema import ASchema
from ingest import getIngestModeData
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
    parser.add_argument("-d", "--dbname", help = "db name, "
        "by default equals to mode name")
    parser.add_argument("-m", "--mode",
        help = "Mode")
    args = parser.parse_args()

    db_config = loadJSonConfig(args.config)
    a_storage = AStorage(db_config)

    db_name = args.dbname
    if not db_name:
        db_name = args.mode
    schema_cfg, process_func = getIngestModeData(args.mode)

    a_schema = ASchema(a_storage, args.mode, db_name,
        schema_cfg, write_mode = True)

    a_storage.activate()

    for key, record in process_func(**db_config["create"][args.mode]):
        a_schema.putRecord(key, record)

    a_schema.close()
    a_storage.deactivate()
