#! /bin/bash

cp /home/pi/pirotator

# Specifically start using java 8 as we are waiting for pi4j v2 to be ready before we can upgrade
/usr/lib/jvm/java-8-openjdk-armhf/bin/java -Duser.home=/home/pi/pirotator -cp ./pirotator.jar org.prowl.pirotator.PiRotator

