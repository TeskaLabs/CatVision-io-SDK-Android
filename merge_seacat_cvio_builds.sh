#!/bin/bash

BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TMP=${BASEDIR}/tmp
TMP_CVIO=${TMP}/cvio
TMP_SEACAT=${TMP}/seacat
TMP_BUILD=${TMP}/build
TMP_JAR=${TMP}/tmp-jar


CVIO_AAR_FILE=CatVision_Android_a78ca93-dirty-release.aar

# Clean
rm -rf ${TMP}

# Create dirs
mkdir -p ${TMP_CVIO}
mkdir -p ${TMP_SEACAT}
mkdir -p ${TMP_BUILD}
mkdir -p ${TMP_JAR}


unzip ${BASEDIR}/seacat/SeaCatClient_Android_v1611-rc-3-release.aar -d ${TMP_SEACAT};
unzip ${BASEDIR}/cvio/build/outputs/aar/${CVIO_AAR_FILE} -d ${TMP_CVIO};

# Copy cvio
cp -r ${TMP_CVIO}/* ${TMP_BUILD}/

# Merge cvio and seacat jni
cp -r ${TMP_SEACAT}/jni/* ${TMP_BUILD}/jni

# Merge classes.jar
(cd ${TMP_JAR}; jar -xf ${TMP_CVIO}/classes.jar)
(cd ${TMP_JAR}; jar -xf ${TMP_SEACAT}/classes.jar)
jar -cf ${TMP}/classes.jar -C ${TMP_JAR} .
mv -f ${TMP}/classes.jar ${TMP_BUILD}/classes.jar

# zip aar
jar cvf ${CVIO_AAR_FILE} -C ${TMP_BUILD}/ .
