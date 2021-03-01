#!/bin/bash 

set -euxo pipefail

input_full_path=$(realpath $1)
dir_path=$(dirname ${input_full_path})

in_name=$(basename ${input_full_path})
#out_name="$(basename ${dir_path}).vep.json"
out_name="$(basename $in_name .vcf).vep.json"

out_full_path="${dir_path}/${out_name}"

vep_setup_dir=$( cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd )

source ${vep_setup_dir}/env.sh

if test -f "${out_full_path}"; then
    echo "${out_full_path} exists! Remove file to make new VEP annotation"
else
    echo "VEP annotation: ${input_full_path} -> ${out_full_path}"

    docker run --user $(id -u):$(id -g) \
	    -e 'HOME=/opt/vep' \
	    --rm \
	    -it \
	    --init \
            -v $(realpath $VEP_CACHE_DIR):/opt/vep/.vep \
            -v ${dir_path}:/data \
            ${VEP_IMAGE} vep \
            --buffer_size 50000 \
            --cache --dir /opt/vep/.vep --dir_cache /opt/vep/.vep \
            --fork 4 \
            --uniprot --hgvs --symbol --numbers --domains --regulatory --canonical --protein \
            --biotype --tsl --appris --gene_phenotype --variant_class \
            --force_overwrite \
            --merged \
            --json \
            --port 3337 \
            --input_file /data/${in_name} \
            --output_file /data/${out_name} \
            --plugin ExACpLI,/opt/vep/.vep/Plugins/ExACpLI_values.txt \
            --plugin MaxEntScan,/opt/vep/.vep/Plugins/fordownload \
            --plugin LoFtool,/opt/vep/.vep/Plugins/LoFtool_scores.txt \
            --plugin SpliceRegion \
            --everything
fi
