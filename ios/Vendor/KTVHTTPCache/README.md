# KTVHTTPCache iOS cache implementation

Source: https://github.com/ChangbaDevs/KTVHTTPCache (2.0.1 lineage).
Vendored from the existing Suiiz cache fork at commit `7a53085cc7c2c543962151928e30ffdb1cc1cf1a`.
MIT license retained in LICENSE.

The only storage behavior change is KTVHCPathTool.basePath using
NSCachesDirectory instead of NSDocumentDirectory. Re-downloadable media is
purgeable and excluded from ordinary document backups. The existing 500 MiB
cache limit and eviction behavior are unchanged.

These sources compile into cached_video_player; do not also link the original
KTVHTTPCache pod, which would duplicate Objective-C classes.
