#!/bin/bash

# Usage and help
usage="
usage: $(basename "$0") [-h] SEACAT CATVISION
\n
\nProgram to merge SeaCat Client and CatVision SDK for publishing
\n
\nwhere:
\n\t -h  show this help text
\n\t SEACAT  path to the SeaCat Client SDK (aar)
\n\t CATVISION  path to the CatVision SDK (aar)"

if [ "$#" -ne 2 ]; then
  echo -e $usage
  exit 1
fi
if [ "$1" == "-h" ]; then
  echo -e $usage
  exit 0
fi

# ARGS
PATH_SEACAT_CLIENT_AAR=$1
PATH_CATVISION_AAR=$2
if [ ! -f ${PATH_SEACAT_CLIENT_AAR} ]; then
	echo "$PATH_SEACAT_CLIENT_AAR is not a file.";
	exit 1
fi
if [ ! -f ${PATH_CATVISION_AAR} ]; then
	echo "$PATH_CATVISION_AAR is not a file.";
	exit 1
fi


# Settings
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
TMP=${BASEDIR}/tmp
TMP_CVIO=${TMP}/cvio
TMP_SEACAT=${TMP}/seacat
TMP_BUILD=${TMP}/build
TMP_JAR=${TMP}/tmp-jar
CVIO_AAR_FILENAME=CatVision_Android_*-release.aar
CVIO_AAR_FILE=$(ls -t ${BASEDIR}/cvio/build/outputs/aar/${CVIO_AAR_FILENAME} | head -n 1)
CVIO_AAR_FILE=$(basename $CVIO_AAR_FILE)

# Clean
rm -rf ${TMP}

# Create dirs
mkdir -p ${TMP_CVIO}
mkdir -p ${TMP_SEACAT}
mkdir -p ${TMP_BUILD}
mkdir -p ${TMP_JAR}

unzip ${PATH_SEACAT_CLIENT_AAR} -d ${TMP_SEACAT};
unzip ${PATH_CATVISION_AAR} -d ${TMP_CVIO};

# Copy entire CVIO build
cp -r ${TMP_CVIO}/* ${TMP_BUILD}/

# Merge JNI from SEACAT
# Just merge the jni for architectures that are supported by CVIO
for f in ${TMP_CVIO}/jni/*; do
	cp -r ${TMP_SEACAT}/jni/$(basename $f) ${TMP_BUILD}/jni/
done

# Merge classes.jar
(cd ${TMP_JAR}; jar -xf ${TMP_CVIO}/classes.jar)
(cd ${TMP_JAR}; jar -xf ${TMP_SEACAT}/classes.jar)
jar -cf ${TMP}/classes.jar -C ${TMP_JAR} .
mv -f ${TMP}/classes.jar ${TMP_BUILD}/classes.jar

# Replace old aar with new one
rm ${PATH_CATVISION_AAR}
jar cvf ${PATH_CATVISION_AAR} -C ${TMP_BUILD}/ .
exit 0
