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

FROM debian:buster
MAINTAINER Vladimir A. Ulitin <v.ulitin@mobin.me>

ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

RUN echo "Etc/UTC" > /etc/timezone \
    && dpkg-reconfigure -f noninteractive tzdata \
    \
    && apt-get update \
    && apt-get install openjdk-11-jre \
    \
    && mkdir /usr/sbin/forome/ \
    && mkdir /usr/sbin/forome/annotation/ \
    && mkdir /usr/sbin/forome/annotation/jar \
    \
    #Чистим
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD source-annotattion/annotation.jar /usr/sbin/forome/annotation/jar/

CMD ["java", "-cp", "/usr/sbin/forome/annotation/jar/annotation.jar"]
