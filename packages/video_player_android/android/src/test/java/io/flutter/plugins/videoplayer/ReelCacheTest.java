package io.flutter.plugins.videoplayer;

import static org.junit.Assert.*;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.media3.datasource.ByteArrayDataSource;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.cache.CacheWriter;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ReelCacheTest {
  @Test
  public void playbackReadsPrefetchedBytesWithoutNetwork() throws Exception {
    Context context = ApplicationProvider.getApplicationContext();
    byte[] bytes = new byte[(int) ReelCache.PREFIX_BYTES];
    Arrays.fill(bytes, (byte) 42);
    DataSpec request = new DataSpec.Builder()
        .setUri("https://example.test/reel-cache-" + System.nanoTime() + ".mp4")
        .setLength(bytes.length).build();
    new CacheWriter(ReelCache.factory(context, () -> new ByteArrayDataSource(bytes))
        .createDataSource(), request, null, null).cache();
    DataSource playback = ReelCache.factory(context, () -> new DataSource() {
      public long open(DataSpec spec) { throw new AssertionError("Unexpected network open"); }
      public int read(byte[] buffer, int offset, int length) { throw new AssertionError("Unexpected network read"); }
      public android.net.Uri getUri() { return null; }
      public void close() {}
      public void addTransferListener(androidx.media3.datasource.TransferListener listener) {}
    }).createDataSource();
    try {
      playback.open(request);
      byte[] actual = new byte[bytes.length];
      int offset = 0;
      while (offset < actual.length) {
        int read = playback.read(actual, offset, actual.length - offset);
        if (read == -1) break;
        offset += read;
      }
      assertEquals(bytes.length, offset);
      assertArrayEquals(bytes, actual);
    } finally { playback.close(); }
  }
}
