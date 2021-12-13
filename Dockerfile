FROM ensemblorg/ensembl-vep:release_105.0
USER root
COPY --chown=vep:vep . /data/project/AStorage/Anfisa-Annotations

RUN apt update && apt install -y sudo git curl libcurl4-openssl-dev wget software-properties-common rsync grsync screen openssh-server less nano net-tools && \
add-apt-repository -y ppa:deadsnakes/ppa && \
apt update && apt install -y python3.8 python3.8-dev python3.8-distutils librocksdb-dev openjdk-8-jdk pyvcf uwsgi && \
ln -sf /usr/bin/python3.8 /usr/bin/python3 && \
curl -L http://xrl.us/installperlnix | bash && \
chmod +x /data/project/AStorage/Anfisa-Annotations/entrypoint.sh && \
ln -sf /data/project/AStorage/Anfisa-Annotations/entrypoint.sh /usr/bin/entrypoint.sh && \
chmod +x /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/build_incontainer.sh && \
chmod +x /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env_incontainer.sh && \
mkdir -p /db/download/{Gerp,dbNSFP4,dbSNP} && chown -R vep:vep /db/ && \

#mkdir -p /data/project/AStorage/venv && \

mv /data/project/AStorage/Anfisa-Annotations/venv /data/project/AStorage/venv && \
mkdir -p /data/project/AStorage/schema && \
mkdir -p /data/project/AStorage/rdbs && \
mkdir -p /data/vep && chown -R vep:vep /data/

USER vep:vep
ENV PATH=$PATH:/opt/vep/.local/bin
RUN cd /data/project/AStorage/Anfisa-Annotations/annotation-service/ && \
./gradlew clean --refresh-dependencies && \
./gradlew build -Pfilename=annotation.jar --refresh-dependencies && \
mv build/libs/annotation.jar ./
RUN bash -c 'export PATH=$PATH:/opt/vep/.local/bin && wget https://bootstrap.pypa.io/get-pip.py && python3 get-pip.py && \
cd /data/project/AStorage/Anfisa-Annotations && pip install -r requirements.txt && cd a_storage/plainrocks && \
pip3 install Cython && pip3 install . && pip3 install -e git+https://github.com/ForomePlatform/forome_misc_tools.git#egg=forome-tools && pip3 install uwsgi'

RUN bash -c 'mv /data/project/AStorage/Anfisa-Annotations/docker/uwsgi.ini /data/project/AStorage/uwsgi.ini && \
cp /data/project/AStorage/Anfisa-Annotations/docker/astorage.cfg.template /data/project/AStorage/astorage.cfg && \
cp /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env_incontainer.sh /data/project/AStorage/Anfisa-Annotations/pipeline/projects/ensembl-vep/env.sh && \
mkdir -p /data/project/AStorage/logs/ && chown -R vep:vep /data/project/AStorage/ && chmod 755 /data/project/AStorage/logs/'

#RUN  ln -sf /proc/1/fd/1 /data/project/AStorage/logs/uwsgi.log
EXPOSE 80
EXPOSE 443
EXPOSE 8290
EXPOSE 3141
EXPOSE 3142

ENTRYPOINT [ "/usr/bin/entrypoint.sh" ]
