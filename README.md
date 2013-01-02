NOTE: Smoothie is currently just a draft API. Although the library is fairly
funcional, this is still alpha-quality code. Do not rely on it for production
code. Feedback is very welcome!

What is it?
===========

Easy async loading for Android's ListView/GridView.

Features
========

* Tiny API to implement asynchonous loading of items in Android's
  ListView and GridView.
* Tight integration between user interaction and async loading of items i.e.
  stops loading items on fling, enables loading when panning with finger
  down, etc.
* Prefetch items beyond the currently visible items to reduce the number of
  placeholder-type items while scrolling.

How do I use it?
================

1. Add Smoothie's jar as a dependency to your project.

2. Add an `AsyncListView` or `AsyncGridView` to your layout.

2. Implement an `ItemLoader`. You're only required to override three methods:
   `getItemParams()`, `loadItem()`, and `displayItem()`. You can override more
   methods if you want to handle loading items from memory, preloading items,
   resetting item views, etc.

3. On Activity/Fragment creation, attach an `ItemManager` instance to your
   AsyncListView/AsyncGridView:

   ```java
   ItemManager.Builder builder = new ItemManager.Builder(yourItemLoader);
   builder.setPreloadItemsEnabled(true).setPreloadItemsCount(5);
   builder.setThreadPoolSize(4);
   ItemManager itemManager = builder.build();

   AsyncListView listView = (AsyncListView) findViewById(R.id.list);
   listView.setItemManager(itemManager);
   ```

The sample app has an example of an ItemEngine powered by
[Android-BitmapCache](https://github.com/chrisbanes/Android-BitmapCache) that
fades images in as they finish loading on a ListView.

Want to help?
=============

File new issues to discuss specific aspects of the API and to propose new
features.

License
=======

    Copyright 2012 Lucas Rocha

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
