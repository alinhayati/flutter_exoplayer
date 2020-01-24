package danielr2001.audioplayer.audioplayers;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import java.io.IOException;

public class InsightLoadErrorPolicy extends DefaultLoadErrorHandlingPolicy {


    @Override
    public long getBlacklistDurationMsFor(int dataType, long loadDurationMs, IOException exception, int errorCount) {
        return 0;
    }

    @Override
    public long getRetryDelayMsFor(int dataType, long loadDurationMs, IOException exception, int errorCount) {
        if (exception instanceof HttpDataSource.HttpDataSourceException) {
            return 5000; // Retry every 5 seconds.
        } else {
            return C.TIME_UNSET; // Anything else is surfaced.
        }
    }

    @Override
    public int getMinimumLoadableRetryCount(int dataType) {
        return Integer.MAX_VALUE;
    }
}