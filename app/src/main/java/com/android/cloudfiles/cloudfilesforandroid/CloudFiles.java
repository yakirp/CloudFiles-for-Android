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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.UUID;

import okio.BufferedSink;
import okio.Okio;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class CloudFiles {

    private HashMap<String, String> urls = new HashMap<String, String>();
    private HashMap<String, String> cdnUrls = new HashMap<String, String>();

    private static final String tokenURL = "https://identity.api.rackspacecloud.com/v2.0/tokens";

    public static final MediaType JSON = MediaType
            .parse("application/json; charset=utf-8");

    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType
            .parse("text/x-markdown; charset=utf-8");

    private static final MediaType IMAGE = MediaType
            .parse("Content-Type: image/jpeg");

    private final OkHttpClient client;

    private String token;

    /**
     * Sstatic constructor to initiate CloudFiles instance
     *
     * @param userName
     * @param apiKey
     * @return @link CloudFiles
     * @throws IOException
     */
    public static CloudFiles initiate(String userName, String apiKey) throws IOException {
        CloudFiles instance = new CloudFiles();
        instance.authentication(userName, apiKey);
        return instance;
    }

    private CloudFiles() {
        client = new OkHttpClient();
    }

    public Response download(String region, String bucket, String key,
                             String target) throws IOException {

        String string = getObjectCdnUrl(region, bucket, key);

        Request request = new Request.Builder().url(string)

                .get().build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);

        FileOutputStream f = new FileOutputStream(target);
        BufferedSink sink = Okio.buffer(Okio.sink(f));
        sink.write(response.body().bytes());
        sink.close();

        return response;

    }

    public String uploadFromUrl(final String urlToUpload, String region, String bucket, String key) throws IOException {

        URL url = new URL(urlToUpload);
        URLConnection connection = url.openConnection();
        connection.connect();
        int fileLenth = connection.getContentLength();
        InputStream inputStream = url.openStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        key = UUID.randomUUID().toString();

        if (urlToUpload.contains(".")) {
            key = key + urlToUpload.substring(urlToUpload.lastIndexOf("."));
        }

        return upload(bufferedInputStream, fileLenth, region, bucket, key);
    }

    public String uploadFromBytes(final byte[] bytesToUpload, String region, String bucket, String key) throws IOException {

        ByteArrayInputStream bufferedInputStream = new ByteArrayInputStream(bytesToUpload);

        key = UUID.randomUUID().toString();

        return upload(bufferedInputStream, bytesToUpload.length, region, bucket, key);
    }

    public String uploadFromFile(final File fileToUpload, String region, String bucket, String key) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(fileToUpload);

        key = UUID.randomUUID().toString();

        String fileName = fileToUpload.getName();

        if (fileName.contains(".")) {
            key = key + fileName.substring(fileName.lastIndexOf("."));
        }

        return upload(fileInputStream, fileToUpload.length(), region, bucket, key);
    }

    private String upload(InputStream inputStream, final long size, String region, String bucket, String key)
            throws IOException {

        CountingFileRequestBody body = new CountingFileRequestBody(inputStream, size, "", new CountingFileRequestBody.ProgressListener() {

            @Override
            public void percentageTransferred(int percentage) {
                System.err.println(percentage);
            }

        });

        Request request = new Request.Builder()
                .url(urls.get(region) + "/" + bucket + "/" + key)
                .header("X-Auth-Token", getToken())
                .addHeader("Content-Length", String.valueOf(size))
                .put(body).build();

        Response response = client.newCall(request).execute();

        if (response.code() == 404) {
            createBucket(region, bucket);
            return upload(inputStream, size, region, bucket, key);
        }

        return getBucketCdnUrl(region, bucket) + "/" + key;


    }

    private String getObjectCdnUrl(String region, String bucket, String key)
            throws IOException {
        return getBucketCdnUrl(region, bucket) + "/" + key;
    }

    private Response createBucket(String region, String bucket)
            throws IOException {

        RequestBody body = RequestBody.create(IMAGE, "");
        Request request = new Request.Builder()
                .url(urls.get(region) + "/" + bucket)
                .header("X-Auth-Token", getToken()).put(body).build();
        client.newCall(request).execute();

        body = RequestBody.create(IMAGE, "");
        request = new Request.Builder().url(cdnUrls.get(region) + "/" + bucket)
                .header("X-Auth-Token", getToken())
                .addHeader("X-Cdn-Enabled", "True").put(body).build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);

        return response;

    }

    private String getBucketCdnUrl(String region, String bucket)
            throws IOException {

        Request request = new Request.Builder()
                .url(cdnUrls.get(region) + "/" + bucket)
                .header("X-Auth-Token", getToken())

                .head().build();

        Response response = client.newCall(request).execute();

        if (response.header("X-Cdn-Ssl-Uri") != null) {
            return response.header("X-Cdn-Ssl-Uri");
        } else {
            throw new IOException(bucket + " at region " + region
                    + " not found");
        }

    }

    private String generateToken(String userName, String apiKey) throws IOException {

        JsonObject cred = new JsonObject();
        cred.addProperty("username", userName);
        cred.addProperty("apiKey", apiKey);
        JsonObject rax = new JsonObject();
        rax.add("RAX-KSKEY:apiKeyCredentials", cred);

        JsonObject obj = new JsonObject();
        obj.add("auth", rax);
        String json = obj.toString();

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder().url(tokenURL).post(body)
                .build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful())
            throw new IOException("Unexpected code " + response);

        return response.body().string();
    }

    private void authentication(String userName, String apiKey)
            throws IOException {
        String json = generateToken(userName, apiKey);

        JsonParser jsonParser = new JsonParser();
        JsonObject jo = (JsonObject) jsonParser.parse(json);

        setToken(jo.getAsJsonObject("access").getAsJsonObject("generateToken")
                .get("id").getAsString());
        JsonArray services = jo.getAsJsonObject("access").get("serviceCatalog")
                .getAsJsonArray();

        for (JsonElement jsonElement : services) {
            JsonObject jobj = jsonElement.getAsJsonObject();

            if (jobj.get("name").getAsString().equals("cloudFiles")) {

                JsonArray endpoints = jobj.getAsJsonArray("endpoints");
                for (JsonElement jsonElement2 : endpoints) {

                    urls.put(jsonElement2.getAsJsonObject().get("region")
                                    .getAsString(),
                            jsonElement2.getAsJsonObject().get("publicURL")
                                    .getAsString());

                }
            }

            if (jobj.get("name").getAsString().equals("cloudFilesCDN")) {

                JsonArray endpoints = jobj.getAsJsonArray("endpoints");
                for (JsonElement jsonElement2 : endpoints) {

                    cdnUrls.put(jsonElement2.getAsJsonObject().get("region")
                                    .getAsString(),
                            jsonElement2.getAsJsonObject().get("publicURL")
                                    .getAsString());

                }
            }

        }

    }

    private String getToken() {
        return token;
    }

    private void setToken(String token) {
        this.token = token;
    }


}
