package render;

import java.awt.Color;

/**
 * Small 5x7 pixel font helper. Provides bitmaps for common ASCII characters
 * and convenience methods to draw characters and strings using
 * SoftwareRenderer.drawPixel(...) so everything stays in our pixel pipeline.
 */
public class PixelFont {
    // Return a 7-element array of 5-bit rows for the provided character.
    // Unknown characters return an empty (spaces) glyph.
    public static int[] getCharBitmap(char ch) {
        switch (Character.toUpperCase(ch)) {
            case 'A': return new int[]{0b01110,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001};
            case 'B': return new int[]{0b11110,0b10001,0b10001,0b11110,0b10001,0b10001,0b11110};
            case 'C': return new int[]{0b01110,0b10001,0b10000,0b10000,0b10000,0b10001,0b01110};
            case 'D': return new int[]{0b11100,0b10010,0b10001,0b10001,0b10001,0b10010,0b11100};
            case 'E': return new int[]{0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b11111};
            case 'F': return new int[]{0b11111,0b10000,0b10000,0b11110,0b10000,0b10000,0b10000};
            case 'G': return new int[]{0b01110,0b10001,0b10000,0b10111,0b10001,0b10001,0b01110};
            case 'H': return new int[]{0b10001,0b10001,0b10001,0b11111,0b10001,0b10001,0b10001};
            case 'I': return new int[]{0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b11111};
            case 'J': return new int[]{0b00111,0b00010,0b00010,0b00010,0b10010,0b10010,0b01100};
            case 'K': return new int[]{0b10001,0b10010,0b10100,0b11000,0b10100,0b10010,0b10001};
            case 'L': return new int[]{0b10000,0b10000,0b10000,0b10000,0b10000,0b10000,0b11111};
            case 'M': return new int[]{0b10001,0b11011,0b10101,0b10101,0b10001,0b10001,0b10001};
            case 'N': return new int[]{0b10001,0b11001,0b10101,0b10011,0b10001,0b10001,0b10001};
            case 'O': return new int[]{0b01110,0b10001,0b10001,0b10001,0b10001,0b10001,0b01110};
            case 'P': return new int[]{0b11110,0b10001,0b10001,0b11110,0b10000,0b10000,0b10000};
            case 'Q': return new int[]{0b01110,0b10001,0b10001,0b10001,0b10101,0b10010,0b01101};
            case 'R': return new int[]{0b11110,0b10001,0b10001,0b11110,0b10100,0b10010,0b10001};
            case 'S': return new int[]{0b01111,0b10000,0b10000,0b01110,0b00001,0b00001,0b11110};
            case 'T': return new int[]{0b11111,0b00100,0b00100,0b00100,0b00100,0b00100,0b00100};
            case 'U': return new int[]{0b10001,0b10001,0b10001,0b10001,0b10001,0b10001,0b01110};
            case 'V': return new int[]{0b10001,0b10001,0b10001,0b10001,0b10001,0b01010,0b00100};
            case 'W': return new int[]{0b10001,0b10001,0b10001,0b10101,0b10101,0b11011,0b10001};
            case 'X': return new int[]{0b10001,0b10001,0b01010,0b00100,0b01010,0b10001,0b10001};
            case 'Y': return new int[]{0b10001,0b10001,0b01010,0b00100,0b00100,0b00100,0b00100};
            case 'Z': return new int[]{0b11111,0b00001,0b00010,0b00100,0b01000,0b10000,0b11111};
            case '0': return new int[]{0b01110,0b10001,0b10011,0b10101,0b11001,0b10001,0b01110};
            case '1': return new int[]{0b00100,0b01100,0b00100,0b00100,0b00100,0b00100,0b01110};
            case '2': return new int[]{0b01110,0b10001,0b00001,0b00010,0b00100,0b01000,0b11111};
            case '3': return new int[]{0b01110,0b10001,0b00001,0b00110,0b00001,0b10001,0b01110};
            case '4': return new int[]{0b00010,0b00110,0b01010,0b10010,0b11111,0b00010,0b00010};
            case '5': return new int[]{0b11111,0b10000,0b11110,0b00001,0b00001,0b10001,0b01110};
            case '6': return new int[]{0b00110,0b01000,0b10000,0b11110,0b10001,0b10001,0b01110};
            case '7': return new int[]{0b11111,0b00001,0b00010,0b00100,0b01000,0b01000,0b01000};
            case '8': return new int[]{0b01110,0b10001,0b10001,0b01110,0b10001,0b10001,0b01110};
            case '9': return new int[]{0b01110,0b10001,0b10001,0b01111,0b00001,0b00010,0b01100};
            case '!': return new int[]{0b00100,0b00100,0b00100,0b00100,0b00000,0b00000,0b00100};
            case '?': return new int[]{0b01110,0b10001,0b00001,0b00110,0b00100,0b00000,0b00100};
            case '.': return new int[]{0b00000,0b00000,0b00000,0b00000,0b00000,0b00110,0b00110};
            case ',': return new int[]{0b00000,0b00000,0b00000,0b00000,0b00110,0b00110,0b00100};
            case ':': return new int[]{0b00000,0b00110,0b00110,0b00000,0b00110,0b00110,0b00000};
            case '-': return new int[]{0b00000,0b00000,0b00000,0b11111,0b00000,0b00000,0b00000};
            case '_': return new int[]{0b00000,0b00000,0b00000,0b00000,0b00000,0b00000,0b11111};
            case '(': return new int[]{0b00010,0b00100,0b01000,0b01000,0b01000,0b00100,0b00010};
            case ')': return new int[]{0b01000,0b00100,0b00010,0b00010,0b00010,0b00100,0b01000};
            case '/': return new int[]{0b00001,0b00010,0b00100,0b01000,0b10000,0b00000,0b00000};
            case ' ': return new int[]{0,0,0,0,0,0,0};
            default: return new int[]{0,0,0,0,0,0,0};
        }
    }

    // Draw a single character using the supplied SoftwareRenderer
    public static void drawChar(SoftwareRenderer r, int x, int y, char ch, int scale, Color color){
        int[] bmp = getCharBitmap(ch);
        for(int row=0; row<7; row++){
            int bits = bmp[row];
            for(int col=0; col<5; col++){
                if(((bits >> (4-col)) & 1) == 1){
                    for(int sy=0; sy<scale; sy++){
                        for(int sx=0; sx<scale; sx++){
                            r.drawPixel(x + col*scale + sx, y + row*scale + sy, color);
                        }
                    }
                }
            }
        }
    }

    public static void drawText(SoftwareRenderer r, int x, int y, String text, int scale, Color color){
        int cx = x;
        int spacing = 1 * scale;
        for(int i=0;i<text.length();i++){
            char ch = text.charAt(i);
            if (ch == '\n') { cx = x; y += (7*scale + spacing); continue; }
            drawChar(r, cx, y, ch, scale, color);
            cx += (5*scale) + spacing;
        }
    }

    public static int measureTextWidth(String text, int scale){
        int spacing = 1 * scale;
        return text.length() * ((5*scale) + spacing);
    }
}
