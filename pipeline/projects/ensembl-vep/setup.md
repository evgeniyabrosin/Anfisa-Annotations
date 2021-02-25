## Guidelines for working with Ensembl VEP locally using its docker image.

1. Define the following parameters for the run:

		export VEP_RELEASE=release_103         # Ensemble VEP release
		export VEP_CACHE_DIR=$HOME/vep-cache   # The local directory to store cached files
		export VEP_ASSEMBLY=GRCh38             # The homo-sapiens species genome assembly

2. Launch build script - this should be done only once for a particular release, when it is required to download the new cache and fasta files. They will be saved into a local directory, defined by `$VEP_CACHE_DIR` variable.

		bash build.sh

3. Now start a new ensembl-vep container instance:

		docker run --rm -it -v $(realpath $VEP_CACHE_DIR):/opt/vep/.vep ensemblorg/ensembl-vep:$VEP_RELEASE bash

4. Run VEP with the required options, for example:

		./vep --cache --offline --format vcf --vcf --force_overwrite \
		--dir_cache /opt/vep/.vep/ \
		--dir_plugins /opt/vep/.vep/Plugins/ \
		--input_file /opt/vep/.vep/input/my_input.vcf \
		--output_file /opt/vep/.vep/output/my_output.vcf
