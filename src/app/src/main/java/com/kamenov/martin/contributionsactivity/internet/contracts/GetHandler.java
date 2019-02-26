package com.kamenov.martin.contributionsactivity.internet.contracts;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Martin on 27.4.2018 г..
 */

public interface GetHandler {
    void handleGet(Call call, Response response);

    void handleError(Call call, Exception ex);
}
