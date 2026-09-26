package io.flutter.plugins.videoplayer;

import android.content.Context;
import android.net.Uri;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheWriter;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import io.flutter.plugin.common.MethodChannel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One disk cache shared by byte prefetch and opted-in reel players. */
@UnstableApi
final class ReelCache {
  static final String HEADER = "X-Suiiz-Reel-Cache";
  static final long PREFIX_BYTES = 512 * 1024;
  private static SimpleCache cache;
  private static volatile boolean diagnostics;
  private final Context context;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private List<String> wanted = Collections.emptyList();
  private CacheWriter active;
  private String activeUrl;
  private int generation;

  ReelCache(Context context) { this.context = context.getApplicationContext(); }

  private static synchronized SimpleCache cache(Context context) {
    if (cache == null) {
      cache = new SimpleCache(new File(context.getCacheDir(), "suiiz-reel-media"),
          new LeastRecentlyUsedCacheEvictor(256L * 1024 * 1024),
          new StandaloneDatabaseProvider(context.getApplicationContext()));
    }
    return cache;
  }

  static CacheDataSource.Factory factory(Context context, DataSource.Factory upstream) {
    java.util.concurrent.atomic.AtomicBoolean reported = new java.util.concurrent.atomic.AtomicBoolean();
    return new CacheDataSource.Factory().setCache(cache(context))
        .setEventListener(new CacheDataSource.EventListener() {
          public void onCacheIgnored(int reason) {}
          public void onCachedBytesRead(long cacheSizeBytes, long cachedBytesRead) {
            if (diagnostics && reported.compareAndSet(false, true))
              android.util.Log.i("REEL-CACHE", "disk_read bytes=" + cachedBytesRead);
          }
        })
        .setUpstreamDataSourceFactory(upstream)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
  }

  synchronized void prefetch(List<String> urls) {
    List<String> next = new ArrayList<>();
    for (String url : urls) {
      String scheme = Uri.parse(url).getScheme();
      if (("https".equals(scheme) || "http".equals(scheme)) && !next.contains(url)) next.add(url);
      if (next.size() == 5) break;
    }
    if (wanted.equals(next)) return;
    wanted = next;
    int ticket = ++generation;
    if (active != null && !wanted.contains(activeUrl)) active.cancel();
    worker.execute(() -> fill(ticket, next));
  }

  private void fill(int ticket, List<String> urls) {
    for (String url : urls) {
      CacheWriter writer;
      synchronized (this) {
        if (ticket != generation) return;
        // CacheWriter skips spans already stored, including bytes from playback.
        writer = new CacheWriter(factory(context, new DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5000).setReadTimeoutMs(5000)
            .setAllowCrossProtocolRedirects(true)).createDataSource(),
            new DataSpec.Builder().setUri(url).setLength(PREFIX_BYTES).build(),
            new byte[32 * 1024], null);
        active = writer;
        activeUrl = url;
      }
      try {
        writer.cache();
        if (diagnostics) android.util.Log.i("REEL-CACHE",
            "prefetched bytes=" + cache(context).getCachedBytes(url, 0, PREFIX_BYTES));
      } catch (Exception ignored) {
        // Prefetch is optional. Playback falls through to the network on a miss.
      } finally {
        synchronized (this) { if (active == writer) { active = null; activeUrl = null; } }
      }
    }
  }

  void handle(io.flutter.plugin.common.MethodCall call, MethodChannel.Result result) {
    if (call.method.equals("prefetch")) {
      diagnostics = Boolean.TRUE.equals(call.argument("diagnostics"));
      List<String> urls = call.argument("urls");
      prefetch(urls == null ? Collections.emptyList() : urls);
      result.success(null);
    } else if (call.method.equals("cachedBytes")) {
      String url = call.argument("url");
      worker.execute(() -> {
        long bytes = url == null ? 0 : cache(context).getCachedBytes(url, 0, PREFIX_BYTES);
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> result.success(bytes));
      });
    } else { result.notImplemented(); }
  }

  synchronized void close() {
    ++generation;
    wanted = Collections.emptyList();
    if (active != null) active.cancel();
    worker.shutdownNow();
    // The process-wide cache stays open: players/other engines can still read it.
  }
}
