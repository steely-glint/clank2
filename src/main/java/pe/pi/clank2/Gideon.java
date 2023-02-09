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
import java.security.SecureRandom;

/**
 *
 * @author thp
 */
public class Gideon {

    private final int width;
    private final int height;
    private final int bpp;
    private Color high;
    private Color target;
    static SecureRandom rand = new SecureRandom();
    private final int lineLen;
    long lastseen = 0;

    public Gideon(int width, int height, int bpp) {
        this.width = width;
        this.height = height;
        this.bpp = bpp;
        this.lineLen = width * bpp;
        lastseen = System.currentTimeMillis();
    }

    public void nav(int max, int[] speeds, byte[] frame,int amax) {
        long now = System.currentTimeMillis();
        int focus = height / 4;
        int sline = focus;
        int val = toHSV(frame, sline * lineLen);
        if (val < 0) {
            sline = height / 2;
            val = toHSV(frame, sline * lineLen);
            if (val < 1) {
                sline = 3 * height / 4;
                val = toHSV(frame, sline * lineLen);
                int c = 0;
                while ((val < 0) && (c < 2)) {
                    sline = rand.nextInt(height - 1);
                    val = toHSV(frame, sline * lineLen);
                    c++;
                }
            }
        }

        if (val >= 0) {
            if (sline > focus) {
                // scale the speed by how close the selected line is...
                int hdif = sline - focus;
                // thats either .25 height or .5 height probably - worst random case .75 height
                int slower = (max * hdif) / height;
                max = max - slower;
            }
            speeds[0] = max;
            speeds[1] = max;
            int halfw = width / 2;
            int diff = halfw - val;
            int dabs = Math.abs(diff);
            // linear % steer 
            //int steer = max -( (2 * max * dabs) / width);

            // dabs is in the range of 0 to width/2 (0->112)
            // so speed pct is in range of 100 -> -12 
            int speedpct = 100 - dabs;
            int steer = (max * speedpct) / 100;

            if (diff < 0) {
                speeds[0] = steer;
            } else {
                speeds[1] = steer;
            }

            // slow down both if we are turning hard. (steer is negative)
            if (steer < 0) {
                if (diff < 0) {
                    speeds[1] = max + steer;
                } else {
                    speeds[0] = max + steer;
                }
            }
            lastseen = now;
        } else {
            long lostfor = (now - lastseen);
            // if we have seen nothing useful in 3 frames, back up.
            if ((lostfor > 100) && (lostfor < 1300)) {
                speeds[1] = speeds[0] = -1 * max/2; // reverse half a metre max
            } else if ((lostfor > 1500) &&(lostfor < 2500)){
                speeds[0] = max/4;
                speeds[1] = -max/4;
            } else {
                speeds[0] = 0;
                speeds[1] = 0;
            }
        }
        paintSpeeds(frame,speeds,amax);
    }

    float sensitivity = 15;
    float[] whiteLower = {0, 0, 100 - sensitivity};
    float[] whiteUpper = {360, sensitivity, 100};
    float[] yellowLower = {30, 50, 50};
    float[] yellowUpper = {70, 100, 100};

    boolean isMatch(float[] hsv, float[] upper, float[] lower) {
        boolean ret = true;
        for (int i = 0; i < 3; i++) {
            if ((hsv[i] <= upper[i]) && (hsv[i] >= lower[i])) {
                ret = true;
            } else {
                ret = false;
                Log.verb("not true " + lower[i] + " < " + hsv[i] + " < " + upper[i] + " i=" + i);
                break;
            }
        }
        return ret;
    }

    boolean isYellow(float[] hsv) {
        boolean ret = isMatch(hsv, yellowUpper, yellowLower);
        return ret;
    }

    boolean isWhite(float[] hsv) {
        boolean ret = isMatch(hsv, whiteUpper, whiteLower);
        return ret;
    }

    static float[] rgb_to_hsv(int r, int g, int b) {
        float hsv[] = new float[3];
        Color.RGBtoHSB(r, g, b, hsv);
        hsv[0] *= 360;
        hsv[1] *= 100;
        hsv[2] *= 100;
        return hsv;
    }

    // return the center pixel of the longest yellow blob (if any)
    private int toHSV(byte[] frame, int midlineStart) {
        int last = midlineStart + lineLen;
        int yellowStart = 0;
        int yellowLength = 0;
        int maxLen = 0;
        int center = -1;
        boolean inBlob = false;
        int pixel = 0;
        for (int i = midlineStart; i < last; i += bpp) {
            int r = Byte.toUnsignedInt(frame[i]);
            int g = Byte.toUnsignedInt(frame[i + 1]);
            int b = Byte.toUnsignedInt(frame[i + 2]);
            float[] hsv = rgb_to_hsv(r, g, b);
            boolean y = isYellow(hsv);
            if (y) {
                if (inBlob) {
                    yellowLength++;
                } else {
                    inBlob = true;
                    yellowStart = pixel;
                    yellowLength = 1;
                }
            } else {
                if (inBlob) {
                    if (yellowLength > maxLen) {
                        center = yellowStart + (yellowLength / 2);
                        maxLen = yellowLength;
                    }
                    inBlob = false;
                    yellowLength = 0;
                }
            }
            if (y) {
                if (high != null) {
                    int rgb = high.getRGB();
                    frame[i] = (byte) ((rgb & 0xff0000) >> 16);
                    frame[i + 1] = (byte) ((rgb & 0x00ff00) >> 8);
                    frame[i + 2] = (byte) (rgb & 0x0000ff);
                }
            }
            pixel++;
        }
        // cover the case that it is yellow to the right hand edge
        if (inBlob && (yellowLength > maxLen)) {
            center = yellowStart + (yellowLength / 2);
        }
        if ((center > 0) && (center < (lineLen / 3))) {
            if (target != null) {
                int c = midlineStart + (3 * center);
                int rgb = target.getRGB();
                frame[c] = (byte) ((rgb & 0xff0000) >> 16);
                frame[c + 1] = (byte) ((rgb & 0x00ff00) >> 8);
                frame[c + 2] = (byte) (rgb & 0x0000ff);
            }

        }
        return center;
    }

    public static void main(String args[]) {
        Log.setLevel(Log.VERB);
        int r = 45, g = 215, b = 0;
        float[] hsv = rgb_to_hsv(r, g, b);
        Log.verb("h s v = " + hsv[0] + " " + hsv[1] + " " + hsv[2]);
        Color y = Color.YELLOW;
        hsv = rgb_to_hsv(y.getRed(), y.getGreen(), y.getBlue());
        Log.verb("h s v = " + hsv[0] + " " + hsv[1] + " " + hsv[2]);

    }

    void setDecorateColours(Color high, Color target) {
        this.high = high;
        this.target = target;
    }

    private void paintSpeeds(byte[] frame, int[] speeds, int amax) {
        Color leftc = speeds[0] >0 ?Color.GREEN:Color.RED;
        Color rightc = speeds[1] >0 ?Color.GREEN:Color.RED;
        int nl = Math.abs(speeds[0])*height/amax;
        int nr = Math.abs(speeds[1])*height/amax;
        for (int l=0;l<height;l++){
            if (l<=nl){
                int px = l*lineLen;
                int rgb = leftc.getRGB();
                frame[px] = (byte) ((rgb & 0xff0000) >> 16);
                frame[px + 1] = (byte) ((rgb & 0x00ff00) >> 8);
                frame[px+ 2] = (byte) (rgb & 0x0000ff);
            }
            if (l<=nr){
                int px = (l*lineLen)+((width-2)*bpp);
                int rgb = rightc.getRGB();
                frame[px] = (byte) ((rgb & 0xff0000) >> 16);
                frame[px + 1] = (byte) ((rgb & 0x00ff00) >> 8);
                frame[px+ 2] = (byte) (rgb & 0x0000ff);
            }
        }

    }



}
