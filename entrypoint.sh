#!/bin/bash

if [ "$1" = "astorage" ] ;
then
    source /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env.sh
    exec uwsgi --ini /data/project/AStorage/uwsgi.ini
fi