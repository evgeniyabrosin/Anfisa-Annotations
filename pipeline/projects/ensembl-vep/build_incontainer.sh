#!/bin/bash

## Run the initial procedure to download and cache Ensembl VEP data for the particular release

set -euxo pipefail

source /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env_incontainer.sh

#if [ -d $VEP_CACHE_DIR ]; then
    #echo "Forome VEP setup is already built. Remove $VEP_CACHE_DIR cache directory to generate a new build."
    #sudo rm -rf $VEP_CACHE_DIR
    #exit 1
#fi

echo "Download VEP cache files"

mkdir -p $VEP_CACHE_DIR
chmod a+rwx $VEP_CACHE_DIR

perl INSTALL.pl -a p -g $VEP_PLUGINS -c $(realpath $VEP_CACHE_DIR)
perl INSTALL.pl -a cf -s $VEP_SPECIES -y $VEP_ASSEMBLY -c $(realpath $VEP_CACHE_DIR)

echo "Download Plugins config files"

VEP_RELEASE_SHORT=$(echo ${VEP_RELEASE}|awk -F'[_.]' '{print$2}')

wget https://raw.github.com/Ensembl/VEP_plugins/release/${VEP_RELEASE_SHORT}/ExACpLI_values.txt -o /dev/null -O ${VEP_CACHE_DIR}/Plugins/ExACpLI_values.txt
wget http://hollywood.mit.edu/burgelab/maxent/download/fordownload.tar.gz -o /dev/null && tar xzf fordownload.tar.gz -C ${VEP_CACHE_DIR}/Plugins/ && rm fordownload.tar.gz
wget https://raw.github.com/Ensembl/VEP_plugins/release/${VEP_RELEASE_SHORT}/LoFtool_scores.txt -o /dev/null -O ${VEP_CACHE_DIR}/Plugins/LoFtool_scores.txt

echo "Forome VEP setup is done"
