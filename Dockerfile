FROM ubuntu:16.04

# Install Java
RUN apt-get update -y && \
    apt-get install -y dos2unix software-properties-common curl && \
    add-apt-repository ppa:webupd8team/java -y && \
    apt-get update -y && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    apt-get install -y oracle-java8-installer && \
    apt-get remove software-properties-common -y && \
    apt-get autoremove -y && \
    apt-get clean

ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Ant
ENV ANT_VERSION 1.10.5
RUN cd && \
    wget -q http://www.us.apache.org/dist//ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -xzf apache-ant-${ANT_VERSION}-bin.tar.gz && \
    mv apache-ant-${ANT_VERSION} /opt/ant && \
    rm apache-ant-${ANT_VERSION}-bin.tar.gz

ENV ANT_HOME /opt/ant
ENV PATH=$ANT_HOME/bin:$PATH

#Install Python
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:jonathonf/python-3.6
RUN apt-get update

RUN apt-get install -y build-essential python3.6 python3.6-dev python3-pip python3.6-venv
RUN apt-get update && apt-get install -y libxft-dev libfreetype6 libfreetype6-dev

#Install matplotlib
RUN pip3 install matplotlib

ADD . .

#WORKDIR "/opendiabetes-uam-heuristik"

RUN ant all-compile
ENTRYPOINT ["java", "-jar", "/code/OpenDiabetes-Algo/dist/OpenDiabetes-Algo.jar"]
