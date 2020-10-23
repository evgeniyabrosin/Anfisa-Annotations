tar -czvf /home/vulitin/pastorage-fasta.tar.gz /projects/AStorage/rdbs/fasta

scp -i /home/kris/forome/key/foro_id_rsa \
 vulitin@108.26.194.164:/home/vulitin/pastorage-fasta.tar.gz \
 /media/kris/external/forome/pastorage-data/
===============================================================================
tar -czvf /home/vulitin/pastorage-dbSNP.tar.gz /projects/AStorage/rdbs/dbSNP

scp -i /home/kris/forome/key/foro_id_rsa \
 vulitin@108.26.194.164:/home/vulitin/pastorage-dbSNP.tar.gz \
 /media/kris/external/forome/pastorage-data/