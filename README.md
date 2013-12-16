pojo-mvcc
=========

Provides a simple in-memory POJO Multi Version Concurrency Control (MVCC) cache


Background
=========
This project contains a simple in-memory Multi-version concurrency control cache for use in Java projects.

In our words: Multi-version concurrency control (MVCC) is a standard technique for avoiding conflicts between reads and writes of the same object. POJO-MVCC guarantees that each transaction sees a consistent view of the object by reading non-current data for objects modified by concurrent transactions. MVCC is a fairly common technique in database transaction implementation and is becoming more common in caching implementations.

One of the benefits of a MVCC approach is that is more efficient than traditional locking implementations for sharing objects in certain situations. This library aims to provide an API that is similar to that of java.util.Map, but has MVCC features - without requiring the use of a database or the configuration of a fully-featured enterprise caching provider.

The emphasis for this project is on simple. That being said, it has the following features:

Completely in-Memory MVCC - so there is no need to start up a database, write to the file-system etc.

Completely Standalone - there is no clustering, replication, domains etc. to configure. Not implemented using a database or any third-party libraries.

Configurable - supports pluggable expiration of revisions, pluggable rules for resolving conflicts etc.

Subversion-style API - operations are similar to subversion: refresh(), checkout(), commit(), revert(), export().

Easy to use - just create a RootElementStore instance and a CacheElementFactory and you're good to go.

Thread safe - allowing shared access of objects between threads. The RootElementStore is protected using read/write locks (java.util.concurrent.ReadWriteLock).

Allows the creation of a object cache, and modification, addition and removal through a separate cloned copy of the cache. When the changes are to be made that can be committed back to the master-cache.


Basically this library gives similar semantics to Subversion for POJO's.

See the Getting Started page to get started in four lines of code!

Why
=========

This library can be used for those times when you want to keep snapshots of an object's state at certain points in time. Such uses could be providing a primitive Software Transactional Memory implementation, providing some transaction support or use as a read-cache for a web-service.

In other projects we have been working on there has been a few cases where a "versioned java.util.Map could have come in real handy. Getting sick of always wishing for one, but not wanting to use a more "complete" solution (such as EHCache, JBoss Cache etc.) we sat down and implemented it.

Basically, you can use this library to create a sort of Software Transactional Memory for sharing objects between threads.

What it isn't
=========
This is not an enterprise caching provider. It serves a very specific purpose - providing a MVCC'd java.util.Map for use in applications. We think that this library works quite well and exceeds any reasonable performance and memory expectations (see Performance).

How it works
=========
This library works similar to subversion in it's internal implementation. The RootObjectCache maintains a RevisionObjectList for each revision. When a checkout() is performed the keys from the current RevisionObjectList are cloned into a RevisionObjectCache. When an object is retrieved from the RevisionObjectCache is performed (using the get() method) the original object from the RootObjectCache is cloned (using the CacheElementFactory clone() method). Additions and removals are also stored in separate lists.

When a commit() is performed all added objects are cloned (using the CacheElementFactory clone() method), removed objects are deleted from the RootObjectCache and all modified objects are refreshed (again using the CacheElementFactory clone()).

When a modified object is commit()ted back into the RootObjectCache the CacheElementFactory's merge() method is called, allowing you to specify how you want merge issues to be resolved. It is possible to provide whatever merging strategy you like using this method - most recent wins, no merging of stale data etc.

Only modified objects are kept in the RevisionObjectCache, minimising the amount of object cloning that must be performed. The RootObjectCache only stores "deltas" for object changes - again reducing the amount of memory used. The RootObjectCache also allows a CacheExpiryPolicy and a CacheExpiryHandler to be provided which will determine when older revisions should be removed from the cache, and what to do with expired objects (write them to disk etc.).
