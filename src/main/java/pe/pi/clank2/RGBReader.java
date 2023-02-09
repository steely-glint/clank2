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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author thp
 */
public class RGBReader {

    public final byte[] buff;
    InputStream input;
    public final int width;
    public final int height;
    public final int bytesPerPixel;
    private Thread readerThread;
    private final String devName;

    public RGBReader(String devName, int width, int height, int bytesPerPixel) throws FileNotFoundException {
        this.devName = devName;
        this.width = width;
        this.height = height;
        this.bytesPerPixel = bytesPerPixel;
        buff = new byte[width * height * bytesPerPixel];
        input = new FileInputStream(devName);
    }

    public void start() {
        readerThread = new Thread(() -> {
            int len = buff.length;
            Log.debug("len = "+len);
            try {
                while (readerThread != null) {
                    long then = System.currentTimeMillis();
                    int n = input.readNBytes(buff, 0, len);
                    long now = System.currentTimeMillis();
                    Log.debug("read " + n + " delay =" + (now - then));
                    then = now;
                    withFrame(buff);
                    now = System.currentTimeMillis();
                    Log.debug("withFrame delay =" + (now - then));
                }
            } catch (Exception ex) {
                if (Log.getLevel() >= Log.DEBUG) {
                    ex.printStackTrace();
                }
                Log.error(this.getClass().getName() + " cant read " + devName + " because " + ex.getMessage());
            } finally {
                try {
                    input.close();
                    Log.debug("closed");
                } catch (IOException ex) {
                    Log.error(this.getClass().getName() + " cant close " + devName + " because " + ex.getMessage());
                }
            }
        }, devName + "-reader");
        readerThread.setPriority(Thread.MAX_PRIORITY);
        readerThread.start();
    }

    /*
    No-op implementation - expected to be overRidden.
     */
    public void withFrame(byte[] frame) {

    }

    public void stop(){
        readerThread = null;
    }
    public static void main(String[] args) {
        String dev = "/dev/video0";
        int w = 640;
        int h = 480;
        int bpp = 3;
        Log.setLevel(Log.ALL);
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
        RGBReader that;
        try {
            that = new RGBReader(dev,w,h,bpp);
            that.start();
        } catch (FileNotFoundException ex) {
            Log.error(dev+ "  not found");
        }
    }
}
