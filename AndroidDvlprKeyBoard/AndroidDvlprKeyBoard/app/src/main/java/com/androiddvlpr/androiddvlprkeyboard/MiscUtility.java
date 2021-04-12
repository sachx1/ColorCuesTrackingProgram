package com.androiddvlpr.androiddvlprkeyboard;

import android.util.Log;

public class MiscUtility {

    //This can be improved or simplified
    public static String probabilityToColor(float prob) {
        String output = "#FF";
        Log.i("DEBUG", prob + " is the probability");
        float whiteLine = 0.03846F;
        int red = prob <= whiteLine ? 255 : (int) (255 * (1  / (1.5 + prob)));
        int green = prob >= whiteLine ? 255 : (int) (255 * prob);
        int blue = (int) (prob <= whiteLine ? 255 * (prob / whiteLine) : 255 * (whiteLine / 1 + prob));
        if(blue > 0) {
            blue *= 0.35;
        }
        Log.i("DEBUG", "blue: " + blue);
        if(red < 16) {
            output += "0" + Integer.toHexString(red);
        } else {
            output += Integer.toHexString(red);
        }

        if(green < 16) {
            output += "0" + Integer.toHexString(green);
        } else {
            output += Integer.toHexString(green);
        }
        if(blue < 16) {
            output += "0" + Integer.toHexString(blue);
        } else {
            output += Integer.toHexString(blue);
        }

        if(!Float.isNaN(prob)) {
            Log.i("DEBUG", "Color: " + output);

            return output;
        } else{
            return  "#FFFF0000";
        }
    }

    public static char mapNumberToLetter(int i) {
        return (char) (65 + i);
    }

    //Given a sentence, find and return the word being worked on
    public static String findCurrentWord(String sentence) {
        String[] parts = sentence.split(" ");
        String lastWord = parts[parts.length - 1];
        return lastWord;
    }
}
