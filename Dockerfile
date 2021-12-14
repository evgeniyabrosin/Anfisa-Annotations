FROM ensemblorg/ensembl-vep:release_105.0
USER root
COPY --chown=vep:vep . /data/project/AStorage/Anfisa-Annotations

RUN tar -xvf /data/forannotation/schema.tar -C /data/project/AStorage && \
    tar -xvf /data/forannotation/venv.tar -C /data/project/AStorage

RUN apt update && apt install -y sudo git curl libcurl4-openssl-dev wget software-properties-common rsync grsync screen openssh-server less nano net-tools && \
add-apt-repository -y ppa:deadsnakes/ppa && \
apt update && apt install -y python3.8 python3.8-dev python3.8-distutils librocksdb-dev openjdk-8-jdk && \
ln -sf /usr/bin/python3.8 /usr/bin/python3 && \
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

RUN apt update && apt install -y ubuntu-release-upgrader-core && \
unlink /usr/bin/python3 && \
ln -s /usr/bin/python3.6 /usr/bin/python3 && \
cd  /usr/lib/python3/dist-packages && \
cp apt_pkg.cpython-36m-x86_64-linux-gnu.so apt_pkg.so

RUN DEBIAN_FRONTEND='noninteractive' apt-get -y -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' upgrade
RUN DEBIAN_FRONTEND='noninteractive' apt-get -y -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' dist-upgrade
#RUN DEBIAN_FRONTEND='noninteractive' apt-get -y -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' do-release-upgrade

RUN do-release-upgrade -f DistUpgradeViewNonInteractive

RUN apt-get -y install python3-pip && \
pip3 install -e git+https://github.com/ForomePlatform/forome_misc_tools.git#egg=forome-tools

USER vep:vep
ENV PATH=$PATH:/opt/vep/.local/bin
RUN cd /data/project/AStorage/Anfisa-Annotations/annotation-service/ && \
./gradlew clean --refresh-dependencies && \
./gradlew build -Pfilename=annotation.jar --refresh-dependencies && \
mv build/libs/annotation.jar ./
RUN bash -c 'export PATH=$PATH:/opt/vep/.local/bin && wget https://bootstrap.pypa.io/get-pip.py && python3 get-pip.py && \
cd /data/project/AStorage/Anfisa-Annotations && pip install -r requirements.txt && cd a_storage/plainrocks && \
pip3 install Cython && pip3 install . && pip3 install -e git+https://github.com/ForomePlatform/forome_misc_tools.git#egg=forome-tools'

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

ENTRYPOINT ["tail", "-f", "/dev/null"]
