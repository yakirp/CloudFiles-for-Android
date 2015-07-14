/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.android.cloudfiles.cloudfilesforandroid;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.internal.Util;

public class CountingFileRequestBody extends RequestBody {

    private static final int SEGMENT_SIZE = 2048; // okio.Segment.SIZE

    private final InputStream inputStream;
    private final ProgressListener listener;
    private final String contentType;
    private final long size;

    public CountingFileRequestBody(InputStream inputStream, long size, String contentType, ProgressListener listener) {
        this.inputStream = inputStream;
        this.contentType = contentType;
        this.listener = listener;
        this.size = size;
    }

    @Override
    public long contentLength() {

        return this.size;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(inputStream);
            long total = 0;
            long read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                this.listener.percentageTransferred((int) ((total / (float) size) * 100));

            }
        } finally {
            Util.closeQuietly(source);
        }
    }

    public interface ProgressListener {
        void percentageTransferred(int percentage);
    }

    private long getUrlContentSize(String contentUrl) throws IOException {
        URL url = new URL(contentUrl);
        URLConnection connection = url.openConnection();
        connection.connect();

        return connection.getContentLength();
    }


}
