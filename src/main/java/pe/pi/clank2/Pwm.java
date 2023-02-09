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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * manage the gpio for clank
 *
 * @author thp
 */
public class Pwm {

    private final SeekableByteChannel chan;
    private final ByteBuffer mess;
    private Integer old = null;

    public Pwm(String name) throws IOException {
        Path path = Paths.get(name);
        mess = ByteBuffer.allocate(20);
        chan = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
    }

    public void setValue(int v) throws IOException {
        if ((old == null) || (old != v)) {
            chan.position(0);
            mess.clear();
            String val = Integer.toString(v);
            mess.put(val.getBytes());
            mess.flip();
            chan.write(mess);
            old = v;
        }
    }

    static public void main(String[] args) {
        Log.setLevel(Log.ALL);
        try {
            Pwm g = new Pwm("/sys/class/pwm/pwmchip0/pwm0/duty_cycle");
            g.setValue(1000000);
            Thread.sleep(1000);
            long nap = 1000 / 30;
            g.setValue(0);
            long then = System.currentTimeMillis();
            for (int i = 0; i <= 1000; i++) {
                int v = i * 1000;
                Log.debug("Loop " + i + " v is " + v);
                g.setValue(v);
                Thread.sleep(nap);
            }
            g.setValue(0);

            long diff = System.currentTimeMillis() - then;
            System.out.println("1000 iterations in " + diff + " ms");
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
