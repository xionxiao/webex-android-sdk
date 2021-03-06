package com.ciscowebex.androidsdk.utils.http;

/*
 * Copyright 2016-2020 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import com.cisco.spark.android.core.LoggingInterceptor;
import com.ciscowebex.androidsdk.CompletionHandler;
import com.ciscowebex.androidsdk.auth.Authenticator;
import com.ciscowebex.androidsdk.internal.ResultImpl;
import com.github.benoitdion.ln.Ln;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by zhiyuliu on 31/08/2017.
 */

public class ServiceBuilder {

    public static final String HYDRA_URL = "https://api.ciscospark.com/v1/";
//    public static final String HYDRA_URL = "https://apialpha.ciscospark.com/v1/";

    private String _baseURL = HYDRA_URL;

    private Gson _gson;

    private List<Interceptor> _interceptors = new ArrayList<>(1);

    private boolean _interceptorChanged = false;

    public ServiceBuilder() {
        _interceptors.add(new DefaultHeadersInterceptor());
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Ln.i("RetrofitLog", "retrofitBack = " + message));
        if (LoggingInterceptor.LogHTTPBody) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }
        else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }
        _interceptors.add(loggingInterceptor);
    }

    public ServiceBuilder baseURL(String url) {
        _baseURL = url;
        return this;
    }

    public ServiceBuilder gson(Gson gson) {
        _gson = gson;
        return this;
    }

    public ServiceBuilder interceptor(Interceptor interceptor) {
        if (!_interceptorChanged) {
            _interceptors.clear();
            _interceptorChanged = true;
        }
        if (interceptor != null) {
            _interceptors.add(interceptor);
        }
        return this;
    }

    public <T> T build(Class<T> service) {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        for (Interceptor interceptor : _interceptors) {
            httpClient.addInterceptor(interceptor);
        }
        OkHttpClient client = httpClient.build();
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(_baseURL)
            .addCallAdapterFactory(RetryCallAdapterFactory.create())
            .addConverterFactory(_gson == null ? GsonConverterFactory.create() : GsonConverterFactory.create(_gson))
            .client(client)
            .build();
        return retrofit.create(service);
    }

    public static <T> void async(Authenticator authenticator, CompletionHandler<T> doAuthFailed, Closure<String> doPrepareReqeust, ListenerCallback doHandleResponse) {
        async(authenticator, doAuthFailed, false, doPrepareReqeust, doHandleResponse);
    }

    public static <T> void async(Authenticator authenticator, CompletionHandler<T> doAuthFailed, boolean notifyFailed, Closure<String> doPrepareReqeust, ListenerCallback doHandleResponse) {
        authenticator.getToken(result -> {
            String token = result.getData();
            if (token == null) {
                if (doPrepareReqeust != null && notifyFailed) {
                    doPrepareReqeust.invoke(null);
                }
                if (doAuthFailed != null) {
                    doAuthFailed.onComplete(ResultImpl.error(result.getError()));
                }
            }
            else {
                if (doHandleResponse != null) {
                    doHandleResponse.setUnauthErrorListener(response -> {
                        if (!handleUnauthError(authenticator, doAuthFailed, doPrepareReqeust, doHandleResponse)){
                            if (doPrepareReqeust != null && notifyFailed) {
                                doPrepareReqeust.invoke(null);
                            }
                            if (doAuthFailed != null) {
                                doAuthFailed.onComplete(ResultImpl.error(response));
                            }
                        }
                    });
                }
                Call call = doPrepareReqeust.invoke("Bearer " + token);
                if (call != null) {
                    call.enqueue(doHandleResponse);
                }
            }
        });
    }

    private static <T> boolean handleUnauthError(Authenticator authenticator, CompletionHandler<T> doAuthFailed, Closure<String> doPrepareReqeust, Callback doHandleResponse) {
        Ln.d("handleUnauthError");
        if (authenticator != null) {
            Ln.d("refreshToken");
            authenticator.refreshToken(result -> {
                String token = result.getData();
                if (token == null) {
                    if (doPrepareReqeust != null) {
                        doPrepareReqeust.invoke(null);
                    }
                    if (doAuthFailed != null) {
                        doAuthFailed.onComplete(ResultImpl.error(result.getError()));
                    }
                }
                else {
                    Call call = doPrepareReqeust.invoke("Bearer " + token);
                    if (call != null) {
                        call.enqueue(doHandleResponse);
                    }
                }
            });
            return true;
        }
        return false;
    }

    public interface Closure<P> {
        Call invoke(P p);
    }

    public interface UnauthErrorListener {
        void onUnauthError(Response response);
    }
}
