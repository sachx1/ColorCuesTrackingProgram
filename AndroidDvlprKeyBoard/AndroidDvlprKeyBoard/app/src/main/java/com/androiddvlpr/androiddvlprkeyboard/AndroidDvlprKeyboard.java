package com.androiddvlpr.androiddvlprkeyboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

@TargetApi(Build.VERSION_CODES.CUPCAKE)
@RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class AndroidDvlprKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private KeyboardView keyboardView;
    private Keyboard keyboard;
    Boolean isCaps = false;


    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateInputView() {
        keyboardView = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard,null);
        keyboard = new Keyboard(this, R.xml.qwerty);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);
        return keyboardView;
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {

        InputConnection inputConnection = getCurrentInputConnection();
        playClick(primaryCode);

        switch (primaryCode){

            case Keyboard.KEYCODE_DELETE:
                inputConnection.deleteSurroundingText(1,0);
                break;

            case Keyboard.KEYCODE_SHIFT:
                isCaps = !isCaps;
                keyboard.setShifted(isCaps);
                keyboardView.invalidateAllKeys();
                break;

            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER));
                break;

            default:
                char code = (char) primaryCode;
                if(Character.isLetter(code) && isCaps){
                    code = Character.toUpperCase(code);
                }
                inputConnection.commitText(String.valueOf(code),1);
        }

    }

    private void playClick(int i){

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch(i){
            case 32:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;

            case Keyboard.KEYCODE_DONE:
            case 10:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;

            case Keyboard.KEYCODE_DELETE:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;

            default:
                audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }

    }


    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }
}
