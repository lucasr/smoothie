NOTE: Smoothie is currently just a draft API. Do not rely on it for production
code. Feedback is very welcome.

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
  placeholder items while scrolling.

How do I use it?
================

1. Add Smoothie's jar as a dependency to your project.

2. Implement an ItemEngine. You're only required to implement two methods:
   loadItem() and displayItem().

3. On Activity/Fragment creation, attach a Smoothie instance to your
   ListView/GridView passing the target view and your engine:

    ListView listView = (ListView) findViewById(R.id.listview);
    Smoothie smoothie = new Smoothie(listView, new YourItemEngine());

4. On your adapter's getView(), call Smoothie's loadItem() passing the item
   view and the loading parameters necessary to load the item asynchronously.

Want to help?
=============

Here's a list of pending stuff on the library:

* Implement a reference engine with async image loading based on
  Android-BitmapCache.
* Proper prefetch support with a custom priority queue on ItemLoader's executor
  service.

File new issues to discuss specific aspects of the API and to propose new
features.
