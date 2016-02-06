package org.nunn.gephiserver.system;

import java.util.Iterator;
import java.util.LinkedHashMap;

/** Simple combination of Map with a clean up thread that removes entries after an amount of time **/
public class ExpiringCache<TKey, TValue> {
	
	/** Implement this interface to have code executed whenever a cache entry expires */
	public static interface ExpirationEventHandler<TEntry> {
		/** Method called on expiration.
		 * @param evictedEntry The cache entry the has been evicted. */
		void onExpiration(TEntry evictedEntry);
	}
	
	private class CacheEntry<TEntry> {
		private final long expiry;
		private final TEntry data;
		
		private CacheEntry(TEntry data) {
			this.expiry = System.currentTimeMillis() + lifetimeMillis;
			this.data = data;
		}
	}
	
	private final LinkedHashMap<TKey, CacheEntry<TValue>> cache;
	private final long lifetimeMillis;
	private ExpirationEventHandler<TValue> expirationEventHandler;
	private Thread worker;
	private volatile boolean runCleaner;
	
	public ExpiringCache(long lifetimeMillis, ExpirationEventHandler<TValue> expirationEventHandler, boolean enableExpiry) {
		this.cache = new LinkedHashMap<TKey, CacheEntry<TValue>>();
		this.lifetimeMillis = lifetimeMillis;
		this.expirationEventHandler = expirationEventHandler;
		if (enableExpiry) {
			enableExpiry();
		}
	}
	
	public ExpiringCache(long lifetimeMillis, ExpirationEventHandler<TValue> expirationEventHandler) {
		this(lifetimeMillis, expirationEventHandler, true);
	}
	
	public ExpiringCache(long lifetimeMillis) {
		this(lifetimeMillis, new ExpirationEventHandler<TValue>() {
			@Override
			public void onExpiration(TValue evictedEntry) {
				// default implementation does nothing
			}
		});
	}
	
	public void enableExpiry() {
		runCleaner = true;
		if (worker == null || Thread.State.TERMINATED.equals(worker.getState())) {
			worker = new Thread(new Cleaner(), "Expiring_Cache_Worker");
			worker.start();
		}
		synchronized (cache) {
			cache.notifyAll();
		}
	}
	
	public void disableExpiry() {
		runCleaner = false;
		synchronized (cache) {
			cache.notifyAll();
		}
	}
	
	public void put(TKey key, TValue data) {
		CacheEntry<TValue> entry = new CacheEntry<>(data);
		synchronized (cache) {
			cache.put(key, entry);
			cache.notifyAll();
		}
	}
	
	public TValue get(TKey key) {
		CacheEntry<TValue> entry;
		synchronized (cache) {
			entry = cache.get(key);
			cache.notifyAll();
		}
		return entry != null ? entry.data : null;
	}
	
	public TValue remove(TKey key) {
		CacheEntry<TValue> entry;
		synchronized (cache) {
			entry = cache.remove(key);
			cache.notifyAll();
		}
		return entry != null ? entry.data : null;
	}
	
	public boolean containsKey(TKey key) {
		boolean found;
		synchronized (cache) {
			found = cache.containsKey(key);
			cache.notifyAll();
		}
		return found;
	}
	
	public boolean containsValue(TValue value) {
		boolean found;
		synchronized (cache) {
			found = cache.containsValue(value);
			cache.notifyAll();
		}
		return found;
	}
	
	public int size() {
		return cache.size();
	}
	
	public boolean isEmpty() {
		return cache.isEmpty();
	}
	
	public void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}
	
	/** Performs clean up as soon as possible.
	 * @return Time in milliseconds to next cache entry expiry. */
	public long cleanup() {
		long diff = 0;
		
		synchronized (cache) {
			for (Iterator<CacheEntry<TValue>> it = cache.values().iterator(); it.hasNext();) {
				CacheEntry<TValue> cacheEntry = it.next();
	
				diff = cacheEntry.expiry - System.currentTimeMillis();
				if (diff > 0) {
					break; // time remains, exit clean up loop
				}
	
				it.remove();
				expirationEventHandler.onExpiration(cacheEntry.data);
			}
		}
		
		return diff;
	}
	
	private class Cleaner implements Runnable {
		@Override
		public void run() {
			synchronized (cache) {
				while (runCleaner) {
					if (cache.isEmpty()) { // nothing to do, so wait
						try {
							cache.wait(); // will be notified when item next added
						}
						catch (InterruptedException ex) {
							runCleaner = false;
						}
					}
					else { // may be expired items, perform clean up
						long diff = cleanup();
						try {
							if (diff > 0) {
								cache.wait(diff); // wake self up when last tested item expired
							}
						}
						catch (InterruptedException ex) {
							runCleaner = false;
						}
					}
					if (Thread.interrupted()) {
						runCleaner = false;
					}
				}
			}
		}
	}
	
}
