#! /bin/bash

# Set the RPi into it's lowest possible power state. 
# If you want to use HDMI or USB then you probably want to comment this out
/opt/vc/bin/tvservice -o
#echo '1-1' | sudo tee /sys/bus/usb/drivers/usb/unbind
#echo 'auto' > '/sys/bus/usb/devices/usb1/power/control'

# Turn off the RPi leds on the board as we have our own.
echo 0 | sudo tee /sys/class/leds/led1/brightness
echo none | sudo tee /sys/class/leds/led0/trigger

# Kludge so that GPSd is happy.
socat pty,raw,echo=0,ignoreof,link=./gps0,iexten=0,nonblock pty,raw,echo=0,ignoreof,link=./gpsd0,iexten=0,nonblock &
sleep 3

# Start the rotator
cd /home/pi/pirotator

# Specifically start using java 8 as we are waiting for pi4j v2 to be ready before we can upgrade
/usr/lib/jvm/java-8-openjdk-armhf/bin/java -Duser.home=/home/pi/pirotator -cp ./pirotator.jar org.prowl.pirotator.PiRotator

