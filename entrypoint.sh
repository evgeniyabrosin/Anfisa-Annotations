#!/bin/bash

if [ "$1" = "astorage" ] ;
then
    source /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env.sh
    exec /data/project/AStorage/venv/bin/uwsgi --ini /data/project/AStorage/uwsgi.ini --virtualenv /data/project/AStorage/venv/
    #exec uwsgi --ini /data/project/AStorage/uwsgi.ini
fi
if [ "$1" = "configurevep" ] ;
then
    exec /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/build_incontainer.sh
fi
if [ "$1" = "annotate" ] ;
then
    export DATASET_FOLDER_NAME=$3
    export INVENTORY_FILE_NAME=$4
    java -cp /data/project/AStorage/Anfisa-Annotations/annotation-service/annotation.jar \
    org.forome.annotation.annotator.main.AnnotatorMainFork -config $2 \
    -inventory /data/forannotation/$DATASET_FOLDER_NAME/$INVENTORY_FILE_NAME
fi
