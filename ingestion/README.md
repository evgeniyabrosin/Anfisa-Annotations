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

================================
Modes currently available: 

hg19 hg38 gerp gnomad pharmgkb gtex spliceai dbnsfp4 clinvar 

**Note**: In newer version of projects Anfisa/Anfisa-Annotations 
(v.0.6.x) the following modes are not in use. 
Use a_storage/ingestion to ingest data into AStorage/RocksDB complex:

hg19 hg38 gerp gnomad spliceai dbnsfp4

=======
Mode hg19:

The mode creates the table in MySQL with information on HG19 genome system.
To run this mode one needs to download file with description of HG19 system:

?internet reference?

and setup path location of this file as config option "hg19.fasta_file".
Database for this table is configured by config option "hg19.database",
recommended is "util".

=======
Mode hg38:

The mode is completely analogous to mode "hg19" but builds GH38 table with
information on HG38 system.
Internet reference to download fasta_file:

?internet reference?

=======
Mode gerp:

Mode creates table with content of Gerp project:

?internet reference?

Data of project is split onto files one per chromosome, name of file has
format containing name of chromosome. For example: "chr11.maf.rates" refers
to chromosome 11.

Config option "gerp.file_list" should be set to list of filenames, or
pattern(s) of filenames with '*' symbol, for example:

 >   "gerp.file_list":   ["?/chr*.maf.rates"])

It is correct to run mode for only portion of cromosomes, but take care
that on the end of ingestion all chromosomes should be loaded.

It is incorrect to perform process for the same chromosome twice: there
shluld be errors on evaluation. In case ofproblems remove the whole table
from MySQL instance and run mode again from the beginning.

Database for this table is configured by config option "hg19.database",
recommended is "conservation".


