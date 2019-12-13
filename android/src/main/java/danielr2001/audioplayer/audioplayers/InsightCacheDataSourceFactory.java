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

import okhttp3.OkHttpClient;

class InsightCacheDataSourceFactory implements DataSource.Factory {
    private Context context;
    private Cache cache;

    InsightCacheDataSourceFactory(Context context) {
        this.context = context;
    }

    @Override
    public DataSource createDataSource() {
        final long DEFAULT_MEDIA_CACHE_SIZE = 200 * 1024 * 1024L;
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient()
                , Util.getUserAgent(this.context, "exoPlayerLibrary"));
        if (cache == null)
            cache = new SimpleCache(new File(this.context.getCacheDir().getAbsolutePath() +
                    "media"), new LeastRecentlyUsedCacheEvictor(DEFAULT_MEDIA_CACHE_SIZE),
                    new ExoDatabaseProvider(this.context));
        return new CacheDataSource(cache,
                httpDataSourceFactory.createDataSource(),
                new FileDataSource(),
                new CacheDataSink(cache, DEFAULT_MEDIA_CACHE_SIZE),
                CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null);
    }
}
