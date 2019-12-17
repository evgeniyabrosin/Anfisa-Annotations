# Copyright (c) 2019. Vladimir Ulitin, Partners Healthcare and members of Forome Association
#
# Developed by Vladimir Ulitin <v.ulitin@mobin.me>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#	 http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM docker.forome.org/forome/ensembl-vep:98
MAINTAINER Vladimir A. Ulitin <v.ulitin@mobin.me>

USER root

export DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    \
    #Set timezone
    && export DEBIAN_FRONTEND=noninteractive \
    && echo "Etc/UTC" > /etc/timezone \
    && apt-get install -y tzdata \
    && dpkg-reconfigure -f noninteractive tzdata \
    \
    && apt-get install -y openjdk-11-jre \
    \
    && mkdir /usr/sbin/forome/ \
    && mkdir /usr/sbin/forome/annotation/ \
    && mkdir /usr/sbin/forome/annotation/jar \
    && mkdir /data/ \
    \
    #Чистим
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

ADD annotation-service/annotation.jar /usr/sbin/forome/annotation/jar/

WORKDIR /data/
CMD ["java", "-cp", "/usr/sbin/forome/annotation/jar/annotation.jar", "org.forome.annotation.annotator.main.AnnotatorScanInventory", "-config", "/config.json", "-scan-inventory"]
