
| DATABASE NAME           |    pharmgkb     |
|-------------------------|-----------------|

| FILE NAME                |   TABLE NAME   |
|-----------------------   |-----------------|
| clinical_ann.tsv         |    CA           |

|COLUMN                    |  COLUMN NAME     |   TYPE |
|-------------------------|-------------------|--------|
Genotype-Phenotype ID     |  GPID            |    INT(10)
Genotype                  |  GTYPE           |    TEXT
Clinical Phenotype        |  CPTYPE          |     TEXT
***
|FILE NAME               |        TABLE NAME |
|---------------------   |-------------------|
|clinical_ann_metadata.tsv |    CAmeta   |

|COLUMN                    | COLUMN NAME     |    TYPE                                |
|--------------------------|-----------------|----------------------------------------|
Clinical Annotation Id    |  CAID           |     INT(10)
Location                  |    LOC          |       TEXT
Gene                      |   GEN           |      TEXT
Level of Evidence         |   LOE           |       VARCHAR(2)
Clinical Annotation Types |  CAT            |      VARCHAR(50)
Genotype-Phenotypes IDs   |  -----numbers correspond with the clinical_ann.tsv file.
Annotation Text           |  AT             |     TEXT|
Variant Annotations IDs   |  VAIDS          |     TEXT|
Variant Annotations       |  VA              |    TEXT|
PMIDs                     |  PMIDS           |    TEXT |
Evidence Count            |  EC              |    INT(3)  |
Related Chemicals         |  RC              |    TEXT    |
Related Diseases          |  RD              |    TEXT     |
Race                      |  RACE            |    VARCHAR(51)
Chromosome                |  CHR             |    VARCHAR(5)

***
|FILE NAME               |     TABLE NAME             |
|------------------------|----------------------------|
clinical_ann_metadata.tsv |   CAmeta2CA|

|COLUMN                   |   COLUMN NAME    |     TYPE    |
|-------------------------|------------------|-------------|
Clinical Annotation Id    |  CAID_CAmeta      |   INT(10)   |
Genotype-Phenotype ID     |  GPID_CA           |  INT(10)|

***
|FILE NAME               |     TABLE NAME            |
|------------------------|---------------------------|
var_drug_ann.tsv         |    VDA                    |

|COLUMN                  |    COLUMN NAME      |   TYPE   |
|------------------------|---------------------|----------|
Annotation ID            |	 AID            |    INT(10)
Variant                  |	 VAR            |    TEXT
Gene                     |	 GENE           |    TEXT
Chemical                 |	 CHEM           |    TEXT
PMID                     |	 PMID           |    INT(10)
Phenotype Category       |	 PCAT           |    VARCHAR(46)
Significance             |	 SIGN           |    VARCHAR(10)
Notes                    |	 NOTES           |   TEXT
Sentence                 |	 SENT            |   TEXT
StudyParameters          ||	 corresponds with the Study Parameters ID in the study_parameters.tsv file 
Alleles                  |	 AL             |    TEXT
Chromosome               |	 CHROM          |    VARCHAR(5)
***
|FILE NAME               |     TABLE NAME          |
|------------------------|-------------------------|
var_fa_ann.tsv           |    VFA

|COLUMN                   |   COLUMN NAME       |  TYPE       |
|-------------------------|---------------------|-------------|
Annotation ID            |	 AID            |    INT(10)
Variant                  |	 VAR            |    TEXT
Gene                     |	 GENE           |    TEXT
Chemical                 |	 CHEM           |    TEXT
PMID                     |	 PMID           |    INT(10)
Phenotype Category       |	 PCAT           |    VARCHAR(26)
Significance             |	 SIGN           |    VARCHAR(10)
Notes                    |	 NOTES          |    TEXT
Sentence                 |	 SENT            |   TEXT
StudyParameters         || 	 corresponds with the Study Parameters ID in the study_parameters.tsv file 
Alleles                  |	 AL            |     TEXT
Chromosome               |	 CHROM         |     VARCHAR(5)
***

|FILE NAME               |     TABLE NAME    |
|------------------------|-------------------|
var_pheno_ann.tsv        |    VPA           |

|COLUMN                  |    COLUMN NAME      |   TYPE     |
|------------------------|---------------------|------------|
Annotation ID           | 	 AID              |  INT(10)
Variant                 | 	 VAR            |    TEXT
Gene                    | 	 GENE            |   TEXT
Chemical                | 	 CHEM             |  TEXT
PMID                    | 	 PMID             |  INT(10)
Phenotype Category      | 	 PCAT           |    VARCHAR(37)
Significance            | 	 SIGN            |   VARCHAR(10)
Notes                   | 	 NOTES           |   TEXT
Sentence                | 	 SENT             |  TEXT
StudyParameters         || 	 corresponds with the Study Parameters ID in the study_parameters.tsv file 
Alleles                 | 	 AL              |   TEXT
Chromosome              | 	 CHROM           |   VARCHAR(5)
***

|FILE NAME              |      TABLE NAME         |
|-----------------------|-------------------------|
study_parameters.tsv     |    SPA                 |

|COLUMN                    |       COLUMN NAME    |     TYPE  |
|--------------------------|----------------------|-----------|
Study Parameters ID      	|     SPID           |     INT(10)
Study Type               	|     ST            |      VARCHAR(56)
Study Cases              	|     SC            |      VARCHAR(6)
Study Controls             	|     SCT            |     VARCHAR(6)
Characteristics          	|     CH              |    TEXT
Characteristics Type     	|     CHT            |     VARCHAR(12)
Frequency In Cases       	|     FIC           |      VARCHAR(9)
Allele Of Frequency In Cases |    AFCS          | 	 VARCHAR(57)
Frequency In Controls    	  |   FICT           |     VARCHAR(10)
Allele Of Frequency In Controls	| AFCT         |       VARCHAR(57)
P Value Operator         	   |  PVO            |     VARCHAR(59)
P Value                  	|     PV             |     VARCHAR(10)
Ratio Stat Type          	|     RST            |     VARCHAR(7)
Ratio Stat               	|     RS             |     VARCHAR(8)
Confidence Interval Start	|     CSTART          |    VARCHAR(10)
Confidence Interval Stop 	 |    CSTOP           |    VARCHAR(13)
Race(s)                  	|     RACE            |    VARCHAR(61)
***
|FILE NAME                 |   TABLE NAME |
|--------------------------|--------------|
var_drug_ann.tsv          |   VDA2SPA

|COLUMN                  |    COLUMN NAME       |   TYPE|
|------------------------|----------------------|-------|
Annotation Id             |  AID_VDA            |  INT(10)
StudyParameters:         |   SPID_SPA           |  INT(10)
***
|FILE NAME              |      TABLE NAME     |
|-----------------------|----------------------|
var_fa_ann.tsv          |     VFA2SPA|

|COLUMN                |      COLUMN NAME    |      TYPE|
|----------------------|---------------------|----------|
Annotation Id         |      AID_VFA          |    INT(10)
StudyParameters:       |     SPID_SPA         |    INT(10)
***
|FILE NAME              |      TABLE NAME |
|-----------------------|-----------------|
var_pheno_ann.tsv       |     VPA2SPA

|COLUMN                  |    COLUMN NAME      |    TYPE|
|------------------------|---------------------|--------|
Annotation Id           |    AID_VPA            |  INT(10)
StudyParameters:         |   SPID_SPA           |  INT(10)
***
|TABLE NAME | 
|-----------|
|CHEMICALS  |


| Field     | Type        | Null | Key | Default | Extra |
|-----------|-------------|------|-----|---------|-------|
| Variant   | varchar(20) | YES  | MUL | NULL    |       |
| AssocKind | varchar(10) | NO   | PRI | NULL    |       |
| AssocID   | int(10)     | NO   | PRI | NULL    |       |
| ChTitle   | varchar(80) | YES  |     | NULL    |       |
| ChID      | varchar(20) | NO   | PRI | NULL    |       |
***
|TABLE NAME | 
|-----------|
|PMIDS      |


| Field     | Type        | Null | Key | Default | Extra |
|-----------|-------------|------|-----|---------|-------|
| Variant   | varchar(20) | YES  | MUL | NULL    |       |
| AssocKind | varchar(10) | NO   | PRI | NULL    |       |
| AssocID   | int(10)     | NO   | PRI | NULL    |       |
| PMID      | int(10)     | NO   | PRI | NULL    |       |
***
|TABLE NAME | 
|-----------|
|DISEASES   |


| Field     | Type        | Null | Key | Default | Extra |
|-----------|-------------|------|-----|---------|-------|
| Variant   | varchar(20) | YES  | MUL | NULL    |       |
| AssocKind | varchar(10) | NO   | PRI | NULL    |       |
| AssocID   | int(10)     | NO   | PRI | NULL    |       |
| DisTitle  | varchar(80) | YES  |     | NULL    |       |
| DisID     | varchar(20) | NO   | PRI | NULL    |       |
***
|TABLE NAME | 
|-----------|
|NOTES      |


| Field     | Type        | Null | Key | Default | Extra |
|-----------|-------------|------|-----|---------|-------|
| Variant   | varchar(20) | YES  | MUL | NULL    |       |
| AssocKind | varchar(10) | NO   | PRI | NULL    |       |
| AssocID   | int(10)     | NO   | PRI | NULL    |       |
| Note      | text        | YES  |     | NULL    |       |



***
***

