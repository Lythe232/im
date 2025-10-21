package com.lythe.jni;

public class NativeLib {

    // Used to load the 'jni' library on application startup.
    static {
        System.loadLibrary("jni");
    }

    /**
     * A native method that is implemented by the 'jni' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}