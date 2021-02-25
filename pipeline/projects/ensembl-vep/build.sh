#!/bin/bash

## Run the initial procedure to download and cache Ensembl VEP data for the particular release

set -euxo pipefail

VEP_RELEASE=${VEP_RELEASE:-release_103}
VEP_IMAGE=ensemblorg/ensembl-vep:$VEP_RELEASE
VEP_CACHE_DIR=${VEP_CACHE_DIR:-$HOME/vep-cache}
VEP_SPECIES=homo_sapiens
VEP_ASSEMBLY=${VEP_ASSEMBLY:-GRCh38}
VEP_PLUGINS="dbNSFP,CADD,G2P,ExACpLI,MaxEntScan,LoFtool,SpliceRegion"

docker pull $VEP_IMAGE

mkdir -p $VEP_CACHE_DIR
chmod a+rwx $VEP_CACHE_DIR

docker run --rm -it --init -v $(realpath $VEP_CACHE_DIR):/opt/vep/.vep $VEP_IMAGE perl INSTALL.pl -a p -g $VEP_PLUGINS
docker run --rm -it --init -v $(realpath $VEP_CACHE_DIR):/opt/vep/.vep $VEP_IMAGE perl INSTALL.pl -a cf -s $VEP_SPECIES -y $VEP_ASSEMBLY
