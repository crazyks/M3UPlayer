M3UPlayer
=========

This is a simple m3u player for Android.

## Features:   
1. Support standard m3u file.  
2. Support channel groups.  
3. Support default m3u file configuration.  
4. Support last played m3u file remember.
5. Support hdpi and mdpi screen.

## Depends:
The icon loader uses the project [https://github.com/nostra13/Android-Universal-Image-Loader](https://github.com/nostra13/Android-Universal-Image-Loader "Android-Universal-Image-Loader").

## Configurations:  
To setup the default m3u file path, you should append a property to /system/build.prop.  
Here is a sample:  
Add `ro.playlist.default=/system/etc/default.m3u` to the end of `/system/build.prop`.

If there is no that property, this application will load /system/etc/playlist.m3u by default.

## Others：  
This is an eclipse project, if you are using Android Studio or other IDE, please convert the project manually.