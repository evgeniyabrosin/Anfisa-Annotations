VEP_SPECIES=homo_sapiens_merged
VEP_ASSEMBLY=GRCh37
VEP_PLUGINS="dbNSFP,CADD,G2P,ExACpLI,MaxEntScan,LoFtool,SpliceRegion"
VEP_RELEASE=release_103
VEP_IMAGE=ensemblorg/ensembl-vep:${VEP_RELEASE}
VEP_DIR=/data/vep
VEP_CACHE_DIR=${VEP_DIR}/vep-cache-${VEP_ASSEMBLY}
