package com.example.destiny.cachetest.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.destiny.cachetest.base.BaseApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Destiny on 2016/7/21.
 */
public class OkHttp3Utils {

    private static OkHttpClient mOkHttpClient;

    //设置缓存目录
    private static File cacheDirectory = new File(BaseApplication.getInstance().getApplicationContext().getCacheDir().getAbsolutePath(), "MyCache");
    private static Cache cache = new Cache(cacheDirectory, 100 * 1024 * 1024);
    private static Context sContext;


    public static OkHttpClient getOKhttpClient() {
        if (null == mOkHttpClient) {
            mOkHttpClient = new OkHttpClient.Builder()
                    //添加拦截器
                    .addInterceptor(new HttpCacheInterceptor())
                    //设置请求读写的超时时间
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .cache(cache)
                    .build();
        }
        return mOkHttpClient;
    }


    private static class HttpCacheInterceptor implements Interceptor {
        private static final Charset UTF8 = Charset.forName("UTF-8");
        private CacheControl mMaxAge = new CacheControl.Builder()
                .maxAge(3600, TimeUnit.SECONDS)
                .build();
        ;

        public enum Level {
            NONE,
            HEADERS,
            BODY
        }

        public interface Logger {
            void log(String message);

            Logger DEFAULT = message -> Platform.get().log(Platform.INFO, message, null);
        }

        public HttpCacheInterceptor() {
            this(Logger.DEFAULT);
        }

        public HttpCacheInterceptor(Logger logger) {
            this.logger = logger;
        }

        private final Logger logger;

        private volatile Level level = Level.BODY;

        /**
         * Change the level at which this interceptor logs.
         */
        public HttpCacheInterceptor setLevel(Level level) {
            if (level == null)
                throw new NullPointerException("level == null. Use Level.NONE instead.");
            this.level = level;
            return this;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {

            long startNs = System.nanoTime();
            Request request = chain.request();

            if (!isNetworkReachable(BaseApplication.getInstance().getApplicationContext())) {
                if (null == sContext) {
                    sContext = BaseApplication.getInstance().getApplicationContext();
                }
                Observable.just(1).subscribeOn(AndroidSchedulers.mainThread())
                        .subscribe(integer -> {
                            ToastUtil.show(sContext, "暂无网络", ToastUtil.LENGTH_SHORT);
                        });
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)//无网络时只从缓存中读取
                        .build();
            }

            Response response = chain.proceed(request);
            if (!isNetworkReachable(BaseApplication.getInstance().getApplicationContext())) {//有网络
                int maxStale = 60 * 60 * 24 * 28; // 无网络时，设置超时为4周
                response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "only-if-cached, max-stale=" + maxStale)
                        .build();
            } else {
                int maxAge = 5 * 60; // 有网络时 设置缓存超时时间1个小时
                response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "max-age=" + maxAge)
                        .build();
            }

            //************************网络访问LOG记录***********************//
            logClientMessage(startNs, request, response);

            return response;
        }


        //************************网络访问LOG记录***********************//
        protected void logClientMessage(long startNs, Request request, Response response) throws IOException {
            Level level = this.level;

            if (level == Level.NONE) {
                return;
            }

            boolean logBody = level == Level.BODY;
            boolean logHeaders = logBody || level == Level.HEADERS;

            RequestBody requestBody = request.body();
            boolean hasRequestBody = requestBody != null;

            String requestStartMessage = request.method() + ' ' + request.url();
            if (!logHeaders && hasRequestBody) {
                requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
            }
            logger.log(requestStartMessage);

            if (logHeaders) {
                logger.log("Request Headers:" + request.headers().toString());
                if (!logBody || !hasRequestBody) {
                    logger.log("--> END " + request.method());
                } else if (bodyEncoded(request.headers())) {
                    logger.log("--> END " + request.method() + " (encoded body omitted)");
                } else if (request.body() instanceof MultipartBody) {

                } else {
                    Buffer buffer = new Buffer();
                    requestBody.writeTo(buffer);

                    Charset charset = UTF8;
                    MediaType contentType = requestBody.contentType();
                    if (contentType != null) {
                        contentType.charset(UTF8);
                    }

                    logger.log(buffer.readString(charset));

                    logger.log(request.method() + " (" + requestBody.contentLength() + "-byte body)");
                }
                logger.log("Headers:" + response.headers().toString());

            }


            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
            logger.log(response.code() + ' ' + response.message() + " (" + tookMs + "ms" + ')');
        }

        private boolean bodyEncoded(Headers headers) {
            String contentEncoding = headers.get("Content-Encoding");
            return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
        }

        private static String protocol(Protocol protocol) {
            return protocol == Protocol.HTTP_1_0 ? "HTTP/1.0" : "HTTP/1.1";
        }
    }


    /**
     * 判断网络是否可用
     *
     * @param context Context对象
     */
    public static Boolean isNetworkReachable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        if (current == null) {
            return false;
        }
        return (current.isAvailable());
    }
}
