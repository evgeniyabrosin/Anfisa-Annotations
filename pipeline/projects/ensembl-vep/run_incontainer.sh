#!/bin/bash 

set -euo pipefail

if [ $# -eq 0 ]; then
    echo "Please supply an input file as an argument"
    exit 1
fi

INPUT_FULL_PATH=$(realpath $1)
DIR_PATH=$(dirname ${INPUT_FULL_PATH})

INPUT_FILE=$(basename ${INPUT_FULL_PATH})
OUTPUT_FILE="$(basename $INPUT_FILE .vcf).vep.json"

OUTPUT_FULL_PATH="${DIR_PATH}/${OUTPUT_FILE}"

VEP_SETUP_DIR=$( cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

#source ${VEP_SETUP_DIR}/env.sh
source /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env_incontainer.sh

if [ ! -d $VEP_CACHE_DIR ]; then
    echo "$VEP_CACHE_DIR does not exist! Run `build.sh` to download the cache files for these parameters."
    exit 1
fi

if test -f "${OUTPUT_FULL_PATH}"; then
    echo "${OUTPUT_FULL_PATH} exists! Remove the file to make a new VEP annotation."
    exit 1
else
    echo "VEP annotation: ${INPUT_FULL_PATH} -> ${OUTPUT_FULL_PATH}"

    vep \
    --buffer_size 50000 \
    --cache --dir /opt/vep/.vep --dir_cache /opt/vep/.vep \
    --fork 4 \
    --force_overwrite \
    --merged \
    --json \
    --port 3337 \
    --input_file /data/${INPUT_FILE} \
    --output_file /data/${OUTPUT_FILE} \
    --plugin ExACpLI,/opt/vep/.vep/Plugins/ExACpLI_values.txt \
    --plugin MaxEntScan,/opt/vep/.vep/Plugins/fordownload \
    --plugin LoFtool,/opt/vep/.vep/Plugins/LoFtool_scores.txt \
    --plugin SpliceRegion \
    --everything
fi
