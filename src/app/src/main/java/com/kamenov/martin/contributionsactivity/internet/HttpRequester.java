package com.kamenov.martin.contributionsactivity.internet;

import com.kamenov.martin.contributionsactivity.internet.contracts.GetHandler;
import com.kamenov.martin.contributionsactivity.internet.contracts.PostHandler;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Martin on 29.3.2018 г..
 */

public class HttpRequester {
    private OkHttpClient client;
    private MediaType JSON;

    public HttpRequester() {
        this.client = new OkHttpClient();
    }

    public void get(final GetHandler handler, String url) {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.handleError(call, e);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                handler.handleGet(call, response);
            }
        });
    }

    public void post(final PostHandler handler, String url, String bodyText) {
        JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, bodyText);
        Request request = new Request.Builder()
                .post(body)
                .addHeader("Content-Type", "application/json")
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.handleError(call, e);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                handler.handlePost(call, response);
            }
        });
    }
}
