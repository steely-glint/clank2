/*
MIT License

Copyright (c) 2023 Tim Panton

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package pe.pi.clank2;

import com.phono.srtplight.Log;
import java.awt.Color;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 *
 * @author thp
 */

public class Clank2 {

    static OutputStream pipe = null;

    public static void main(String[] args) {
        String dev = "/dev/video0";
        String pipeName = null;

        int w = 224;
        int h = 224;
        int bpp = 3;
        Log.setLevel(Log.DEBUG);
        if (args.length > 0) {
            dev = args[0];
        }
        if (args.length > 1) {
            w = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            h = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            bpp = Integer.parseInt(args[3]);
        }
        if (args.length > 4) {
            pipeName = args[4];
        }
        Gideon gid = new Gideon(w, h, bpp);
        gid.setDecorateColours(Color.YELLOW, Color.BLACK);
        int speeds[] = new int[2];
        int max = 1000000;
        try {
            if (pipeName != null) {
                Log.info("Clank glass looking for " + pipeName);
                Path path = Paths.get(pipeName);
                pipe = Files.newOutputStream(path, StandardOpenOption.WRITE);
                Log.info("Clank glass opened " + pipeName);
            }
            Pwm leftMotor = new Pwm("/sys/class/pwm/pwmchip0/pwm0/duty_cycle");
            Pwm rightMotor = new Pwm("/sys/class/pwm/pwmchip0/pwm1/duty_cycle");
            Gpio leftDirection = new Gpio("/sys/class/gpio/gpio5/value");
            Gpio rightDirection = new Gpio("/sys/class/gpio/gpio6/value");
            RGBReader rgbReader = new RGBReader(dev, w, h, bpp) {
                private boolean closeing;

                @Override
                public void withFrame(byte[] frame) {
                    if (!closeing) {
                        try {
                            gid.nav(max, speeds, frame,max);
                            rightMotor.setValue(Math.abs(speeds[0]));
                            leftMotor.setValue(Math.abs(speeds[1]));
                            rightDirection.setValue(speeds[0] < 0);
                            leftDirection.setValue(speeds[1] < 0);
                            Log.debug("speeds " + speeds[1] + " " + speeds[0]);
                            if (pipe != null) {
                                pipe.write(frame);
                            }
                        } catch (Exception x) {
                            Log.error("exception steering " + x.getMessage());
                            if (Log.getLevel() >= Log.DEBUG) {
                                x.printStackTrace();
                            }
                        }
                    }
                }
            };
            Log.info("starting a rgb reader.");
            rgbReader.start();
        } catch (Exception x) {
            Log.error("exception opening Yellow line thing" + x.getMessage());
        }
    }
}
