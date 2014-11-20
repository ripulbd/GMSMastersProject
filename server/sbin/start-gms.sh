#!/bin/bash

fullPath () {
        t='TEMP=`cd $TEMP; pwd`'
        for d in $*; do
                eval `echo $t | sed 's/TEMP/'$d'/g'`
        done
}


echo "***** Starting GMS *****"
#echo $JAVA_HOME
#echo `which java`
if [ ! -n "$JAVA_HOME" ]
then
	 #where is java?
        TEMPVAR=`which java`
        #j2sdk/bin folder is in the dir that java was in
        TEMPVAR=`dirname $TEMPVAR`
        #then java install prefix is folder above
        JAVA_HOME=`dirname $TEMPVAR`
        echo "Setting JAVA_HOME to $JAVA_HOME"
fi

#setup GMS_HOME
if [ ! -n "$GMS_HOME" ]
then
        #find out where this script is running
        TEMPVAR=`dirname $0`
        #make the path abolute
        fullPath TEMPVAR
        #terrier folder is folder above
        GMS_HOME=`dirname $TEMPVAR`
        echo "Setting GMS_HOME to $GMS_HOME"
fi


#setup GMS_ETC
if [ ! -n "$GMS_ETC" ]
then
        GMS_ETC=$GMS_HOME/etc
	echo "Setting GMS_ETC to $GMS_ETC"
fi

#Setting ClassPath
for jar in ./lib/*.jar ./lib/*.zip; do
   if [ ! -n "$GMS_CLASSPATH" ]
        then
                GMS_CLASSPATH=$jar
        else
                GMS_CLASSPATH=$GMS_CLASSPATH:$jar
        fi
done

GMS_CLASSPATH=$GMS_CLASSPATH:src/
JAVA_OPTIONS=

echo $JAVA_HOME/bin/java -Xmx512M $JAVA_OPTIONS $GMS_OPTIONS -Dgms.etc=$GMS_ETC -Dgms.home=$GMS_HOME -cp $GMS_CLASSPATH $@

$JAVA_HOME/bin/java -Xmx512M $JAVA_OPTIONS $GMS_OPTIONS \
	 -Dgms.etc=$GMS_ETC \
       -Dgms.home=$GMS_HOME \
	 -cp $GMS_CLASSPATH com.sipc.wyatt.ExMain $@

