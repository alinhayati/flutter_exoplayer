package danielr2001.audioplayer.audioplayers;

import android.content.Context;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

class InsightCacheDataSourceFactory implements DataSource.Factory {

    private Context context;
    private Cache cache;
    private CacheDataSink cacheDataSink;
    private FileDataSource fileDataSource = new FileDataSource();

    InsightCacheDataSourceFactory(Context context) {
        this.context = context;
        long DEFAULT_MEDIA_CACHE_SIZE = 200 * 1024 * 1024L;
        cache = new SimpleCache(
                new File(context.getCacheDir(), "media"),
                new LeastRecentlyUsedCacheEvictor(DEFAULT_MEDIA_CACHE_SIZE),
                new ExoDatabaseProvider(context)
        );
        cacheDataSink = new CacheDataSink(cache, DEFAULT_MEDIA_CACHE_SIZE);

    }

    @Override
    public DataSource createDataSource() {

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();

        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(okHttpClient
                , Util.getUserAgent(context, "exoPlayerLibrary"));
        return new CacheDataSource(cache,
                httpDataSourceFactory.createDataSource(),
                fileDataSource,
                cacheDataSink,
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }
}
