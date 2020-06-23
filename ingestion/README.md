# ingestion subproject

The annotation process provided by Anfisa-Annotation project requires
data from various sources in form of tables in instance of MySQL.

This subproject provides ingestion of these sources into mySQL.
It is written on Python3 and can be used in the following way.

1. Clone or download copy of repository Anfisa-Annotation and
go to ingestion directory

2. Make a copy the file config_proto.js with name config.js:

> cp config_proto.js config.js

Then edit this local file config.js: replace '?'signs with meaningful
values. The file should be formatted in proper JSON form.

3. If the config file is set up properly, one can perform one by one
the modes of ingestion process:

> python3 ingest.py -m _mode_ config.js

Attention: each mode is a long process, so it is recommended to
start it in in a safe way (immune to hangups):

> nohup python3 ingest.py -m _mode_ config.js &> log.txt &

## Modes currently available:

**hg19 hg38 gerp gnomad pharmgkb gtex spliceai dbnsfp4 clinvar**

**Note**: In newer version of projects Anfisa/Anfisa-Annotations
(v.0.6.x) the following modes are not in use.
Use a_storage/ingestion to ingest data into AStorage/RocksDB complex:

hg19 hg38 gerp gnomad spliceai dbnsfp4

Mode hg19
---------
GRCh37 (HG19)

* Project URL: [https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.13/](https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.13/)
* File URL:
    [http://ftp.ncbi.nlm.nih.gov/genomes/refseq/vertebrate_mammalian/Homo_sapiens/all_assembly_versions/GCF_000001405.25_GRCh37.p13/GCF_000001405.25_GRCh37.p13_genomic.fna.gz](http://ftp.ncbi.nlm.nih.gov/genomes/refseq/vertebrate_mammalian/Homo_sapiens/all_assembly_versions/GCF_000001405.25_GRCh37.p13/GCF_000001405.25_GRCh37.p13_genomic.fna.gz)

The mode creates the table in MySQL with information on HG19 genome system.
To run this mode one needs to download file with description of HG19 system
and setup path location of this file as config option "hg19.fasta_file".
Database for this table is configured by config option "hg19.database",
recommended is "util".

Mode hg38
----------
GRCh38

* Version: patch 13
* Project URL: [https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.39](https://www.ncbi.nlm.nih.gov/assembly/GCF_000001405.39)
* File URL: [ftp://ftp.ncbi.nlm.nih.gov/genomes/refseq/vertebrate_mammalian/Homo_sapiens/all_assembly_versions/GCF_000001405.39_GRCh38.p13](ftp://ftp.ncbi.nlm.nih.gov/genomes/refseq/vertebrate_mammalian/Homo_sapiens/all_assembly_versions/GCF_000001405.39_GRCh38.p13)

The mode is completely analogous to mode "hg19" but builds GH38 table with
information on HG38 system.

Mode gerp
---------
GERP Scores

* Project URL: [http://mendel.stanford.edu/SidowLab/downloads/gerp/](http://mendel.stanford.edu/SidowLab/downloads/gerp/)
* Download URL: [http://mendel.stanford.edu/SidowLab/downloads/gerp/hg19.GERP_scores.tar.gz](http://mendel.stanford.edu/SidowLab/downloads/gerp/hg19.GERP_scores.tar.gz)

Data of project is split onto files one per chromosome, name of file has
format containing name of chromosome. For example: "chr11.maf.rates" refers
to chromosome 11.

Config option "gerp.file_list" should be set to list of filenames, or
pattern(s) of filenames with '*' symbol, for example:

 >   "gerp.file_list":   ["?/chr*.maf.rates"])

It is correct to run mode for only portion of cromosomes, but take care
that on the end of ingestion all chromosomes should be loaded.

It is incorrect to perform process for the same chromosome twice: there
shluld be errors on evaluation. In case of problems remove the whole table
from MySQL instance and run mode again from the beginning.

Mode gnomad
------------
Genome Aggregation Database (gnomAD)

* Version: 2.1.1
* URL: [https://gnomad.broadinstitute.org/](https://gnomad.broadinstitute.org/)
* Download URL: [https://gnomad.broadinstitute.org/downloads](https://gnomad.broadinstitute.org/downloads)

Mode pharmgkb
--------------
PharmGKB (Pharmacogenomics)

* Project URL: [https://www.pharmgkb.org/](https://www.pharmgkb.org/)
* Download URL: [https://www.pharmgkb.org/downloads](https://www.pharmgkb.org/downloads)
* File: Variant Annotations Help File (annotations.zip)

Mode gtex
---------
GTEx

* Project URL: [https://www.gtexportal.org/home/](https://www.gtexportal.org/home/)
* Download URL: [https://storage.googleapis.com/gtex_analysis_v8/rna_seq_data/GTEx_Analysis_2017-06-05_v8_RNASeQCv1.1.9_gene_median_tpm.gct.gz](https://storage.googleapis.com/gtex_analysis_v8/rna_seq_data/GTEx_Analysis_2017-06-05_v8_RNASeQCv1.1.9_gene_median_tpm.gct.gz)

Mode spliceai
-----------
SpliceAI

* GitHub URL: [https://github.com/Illumina/SpliceAI](https://github.com/Illumina/SpliceAI)
* Version: v1pre3
* Download URL (requires free registration):
    https://basespace.illumina.com/analyses/194103939/files/236418325?projectId=66029966
* Files:

    spliceai_scores.masked.snv.hg38.vcf.gz
    spliceai_scores.masked.indel.hg38.vcf.gz

Mode dbnsfp4
------------
dbNSFP

* Project URL: [https://sites.google.com/site/jpopgen/dbNSFP](https://sites.google.com/site/jpopgen/dbNSFP)
* File URL: [ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFP4.0a.zip](ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFP4.0a.zip)
*   https://drive.google.com/file/d/1BNLEdIc4CjCeOa7V7Z8n8P8RHqUaF5GZ/view?usp=sharing

Mode clinvar
-----------
ClinVar

* Project URL: [https://www.ncbi.nlm.nih.gov/clinvar/](https://www.ncbi.nlm.nih.gov/clinvar/)
* CSV File URL (contains data): [https://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz ](https://ftp.ncbi.nlm.nih.gov/pub/clinvar/tab_delimited/variant_summary.txt.gz )
* XML File URL (contains data and metadata): [https://ftp.ncbi.nlm.nih.gov/pub/clinvar/xml/](https://ftp.ncbi.nlm.nih.gov/pub/clinvar/xml/)

