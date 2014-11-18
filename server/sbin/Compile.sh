#!/bin/bash

fullPath () {
        t='TEMP=`cd $TEMP; pwd`'
        for d in $*; do
                eval `echo $t | sed 's/TEMP/'$d'/g'`
        done
}


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
echo "Using Java $JAVA_HOME"

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


echo "******* Setting Classpath *******";
if [ ! -n "$GMSCLASSPATH" ]
then
for jar in ./lib/*.jar ./lib/*.zip; do
        if [ ! -n "$GMSCLASSPATH" ]
        then
                GMSCLASSPATH=$jar
        else
                GMSCLASSPATH=$GMSCLASSPATH:$jar
        fi
done
fi

echo "******* Compiling *******"
echo "javac -cp $GMSCLASSPATH `find . -name '*.java'`"
$JAVA_HOME/bin/javac -cp $GMSCLASSPATH `find . -name '*.java'`
cd ./src
$JAVA_HOME/bin/jar -cf ../lib/GMS.jar `find . -name '*.class'`
