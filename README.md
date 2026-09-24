# Cached Video Player

A flutter plugin that has been forked from the official [video_player](https://pub.dev/packages/video_player) package except that it supports caching in Android and iOS.
Web plugin will work like official [video_player](https://pub.dev/packages/video_player) i.e. without caching support.

## Installation

First, add `cached_video_player` as a [dependency in your pubspec.yaml file](https://flutter.io/platform-plugins/).

Follow the Android and iOS configuration steps of the official [video_player](https://pub.dev/packages/video_player#installation) package. This plugin won't work in Desktop.

### Issues
* `maxFileSize` and `maxCacheSize` are hardcoded at 100MiB and 1GiB respectively in Android.

### Contributors

* [Vikram Pratap Singh](https://github.com/vikram25897)
* [EnderTan](https://github.com/EnderTan)
* [Philipp Bauer](https://github.com/ciriousjoker)
## Suiiz iOS cache storage

This fork bundles the MIT-licensed KTVHTTPCache sources and stores downloadable
video data in `Library/Caches/KTVHTTPCache`, not `Documents/KTVHTTPCache`.
No separate local KTVHTTPCache pod override is needed. The cache may be purged
by iOS; callers must be able to re-download videos. The existing 500 MiB limit
is unchanged. On initialization the plugin deletes only the legacy
`Documents/KTVHTTPCache` directory on a background queue, once per process.
User documents and drafts outside that cache directory are preserved.

The `packages/appinio_video_player` package preserves the existing Appinio
wrapper API while depending on this fork. Consumers can use that Git package
path instead of overriding its old transitive cache dependency.
