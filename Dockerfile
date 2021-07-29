FROM ensemblorg/ensembl-vep:release_103
USER root
COPY --chown=vep:vep . /data/project/AStorage/Anfisa-Annotations
RUN apt update && apt install -y git curl libcurl4-openssl-dev wget software-properties-common && \
add-apt-repository -y ppa:deadsnakes/ppa && \
apt update && apt install -y python3.8 python3.8-dev python3.8-distutils librocksdb-dev openjdk-8-jdk && \
ln -sf /usr/bin/python3.8 /usr/bin/python3 && chmod +x /data/project/AStorage/Anfisa-Annotations/entrypoint.sh && ln -sf /data/project/AStorage/Anfisa-Annotations/entrypoint.sh /usr/bin/entrypoint.sh



USER vep:vep
ENV PATH=$PATH:/opt/vep/.local/bin
RUN cd /data/project/AStorage/Anfisa-Annotations/annotation-service/ && \
./gradlew clean --refresh-dependencies && \
./gradlew build -Pfilename=annotation.jar --refresh-dependencies && \
mv build/libs/annotation.jar ./
RUN bash -c 'export PATH=$PATH:/opt/vep/.local/bin && wget https://bootstrap.pypa.io/get-pip.py && python3 get-pip.py && cd /data/project/AStorage/Anfisa-Annotations && pip install -r requirements.txt && cd a_storage/plainrocks && pip3 install Cython && pip3 install . && pip3 install -e git+https://github.com/ForomePlatform/forome_misc_tools.git#egg=forome-tools && mv /data/project/AStorage/Anfisa-Annotations/docker/uwsgi.ini /data/project/AStorage/uwsgi.ini && cp /data/project/AStorage/Anfisa-Annotations/docker/astorage.cfg.template /data/project/AStorage/astorage.cfg'
ENTRYPOINT [ "entrypoint.sh" ]