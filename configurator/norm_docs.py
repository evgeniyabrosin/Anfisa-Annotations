import glob
import sys
import os
from typing import Dict


def execute(cmd):
    print(cmd)
    os.system(cmd)


def read_mapping(path):
    with open (path) as lines:
        mapping = {
            x[3]:x[0]
            for x in [line.split() for line in lines]
        }
    return mapping


def rename_files(mapping:Dict, path):
    commands = []
    for sample in mapping:
        files = glob.glob(os.path.join(path, "**"), recursive=True)
        commands += [
            "mv {f} {f1}".format(f=f, f1=f.replace(sample, mapping[sample]))
            for f in files if sample in f
        ]
    for cmd in commands:
        execute(cmd)


def rename_in_files(mapping:Dict, path):
    substitutions = [
        "-e 's/{sample}/{name}/g'".format(sample=sample, name = mapping[sample])
        for sample in mapping
    ]
    sed = "sed -i '' " + " ".join(substitutions)
    cmd = "find {path} -type f -exec {sed}".format(path=path, sed=sed)
    cmd += " {} \; -print"
    execute(cmd)


if __name__ == '__main__':
    mapping = read_mapping(sys.argv[2])
    rename_files(mapping, sys.argv[1])
    rename_in_files(mapping, sys.argv[1])