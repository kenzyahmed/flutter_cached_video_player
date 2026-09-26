# SUIIZ Android reel byte cache

Based on Flutter video_player_android 2.9.5 (upstream BSD license retained).
Only HTTP players with the internal X-Suiiz-Reel-Cache: 1 header opt in.
The marker is removed before contacting the origin. Other players and iOS are unchanged.

The suiiz/reel_cache channel prefetches the first 512 KiB of up to five HTTP URLs,
serially, without creating a player. Changing the list cancels obsolete active work.
Playback and prefetch share a process-wide Media3 SimpleCache under cacheDir,
with a 256 MiB LRU bound. Playback also caches later ranges. OS eviction is safe.
Partial failures leave valid cache spans and streaming falls back to the network.
URLs must be the same exact public media URLs passed to playback; authenticated
request headers are not supported by this prefetch API. Clear URLs are never logged.

Tests: ReelCacheTest proves playback reads a warmed range with an upstream that
throws if opened. Run :video_player_android:testDebugUnitTest --tests
io.flutter.plugins.videoplayer.ReelCacheTest -Pandroid.enableJetifier=false from
an Android host. The Jetifier override is needed with this host's older AGP test tooling.

Prefetch does not guarantee immediate playback: decoder initialization, uncached
ranges, metadata at the file tail, network throughput and keyframes still matter.
