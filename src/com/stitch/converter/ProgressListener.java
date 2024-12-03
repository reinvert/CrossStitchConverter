package com.stitch.converter;

public interface ProgressListener {
    void onProgress(double progress, String message);
    void finished();
}