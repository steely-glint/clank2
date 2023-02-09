#!/bin/sh -v
aplay -l 
sleep 1
(echo 0 > /sys/class/pwm/pwmchip0/export)
(echo 1 > /sys/class/pwm/pwmchip0/export)
(echo 6 > /sys/class/gpio/export)
(echo 5 > /sys/class/gpio/export)
sleep 1 
(echo out > /sys/class/gpio/gpio5/direction)
(echo out > /sys/class/gpio/gpio6/direction)
(echo 1000000 > /sys/class/pwm/pwmchip0/pwm0/period)
(echo 1000000 > /sys/class/pwm/pwmchip0/pwm1/period)
(echo 1 > /sys/class/pwm/pwmchip0/pwm0/enable)
(echo 1 > /sys/class/pwm/pwmchip0/pwm1/enable)

v4l2-ctl -d /dev/video0 \
 --set-fmt-video width=224,height=224,pixelformat=2 -p 30

v4l2-ctl -c saturation=100

cd /home/pi
JAVA_HOME=/home/pi/zulu11.62.17-ca-jdk11.0.18-linux_aarch32hf export JAVA_HOME

${JAVA_HOME}/bin/java -Xmx128m -cp target/clank2-1.0-SNAPSHOT.jar pe.pi.clank2.Clank2 224 224 3 /dev/null  
