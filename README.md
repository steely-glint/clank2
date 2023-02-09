# clank2
## Code for a Yellow line follower robot on a raspi zero.

This is the codebase that came second at 2023 robocars Berlin.

It runs on a Raspberry Pi with 2 motors controled by 2 PWM pins

Clank was a pizeroW with a picam and a motordriver h-bridge driving two polu motors.

For some back story please read the slides in [Robocars2023-clank.pdf]

To run, install java 11:
- on a Pi Zero you'll need to get a jdk from azul.com as debian no longer has jdk builds for armv6 
- on all newer Pi's you can use apt-get 

Enable your camera (in bullseye you need to enable legacy camera) with raspi-config
Enable _both_ pwm pins by adding the lines in config.txt.append to the end of /boot/config.txt .
Reboot.

now you can run c2zerostart.sh
Show a yellow object to the camera, your robot should steer towards it.

If you want to see what it is thinking, create a named pipe in the current directory:
mknod ./rgbpipe p
edit c2zerostart.sh and replace /dev/null with ./rgbpipe
install gstreamer and attach a monitor, then run gstconsume as well.
you should see video similar to that in onTrack.movA

(note that the c2zerostart.sh will block until you start the gstconsume process.)

You can also create gstreamer pipelines to send the video to a file or remote VLC or WebRTC or mjpegs,
whatever works best in your situation


Please Fork, send PRs raise issues etc.
