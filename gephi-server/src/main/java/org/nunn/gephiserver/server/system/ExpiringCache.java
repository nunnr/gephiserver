package org.nunn.gephiserver.server.system;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/** Simple combination of Map with a clean up thread that removes entries after an amount of time **/
public class ExpiringCache<TKey, TValue> {
	
	/** Implement this interface to have code executed whenever a cache entry expires */
	public static interface ExpirationEventHandler<TKeyExpired, TValueExpired> {
		/** Method called on expiration.
		 * Implementations should perform work on a separate thread, to minimise lock time on this cache.
		 * @param key The key of the cache data that has been evicted.
		 * @param data The cache data that has been evicted. */
		void onExpiration(TKeyExpired key, TValueExpired data);
	}
	
	private class CacheEntry {
		private final long expiry;
		private final TValue data;
		
		private CacheEntry(TValue data) {
			this.expiry = System.currentTimeMillis() + lifetimeMillis;
			this.data = data;
		}
	}
	
	private final LinkedHashMap<TKey, CacheEntry> cache;
	private final long lifetimeMillis;
	private ExpirationEventHandler<TKey, TValue> expirationEventHandler;
	private Thread worker;
	private volatile boolean runCleaner;
	
	public ExpiringCache(long lifetimeMillis, ExpirationEventHandler<TKey, TValue> expirationEventHandler, boolean enableExpiry) {
		this.cache = new LinkedHashMap<>();
		if (lifetimeMillis < 1) {
			throw new IllegalArgumentException("The lifetimeMillis argument must be greater than 1.");
		}
		this.lifetimeMillis = lifetimeMillis;
		this.expirationEventHandler = expirationEventHandler != null ? expirationEventHandler : (key, data) -> {};
		if (enableExpiry) {
			enableExpiry();
		}
	}
	
	public ExpiringCache(long lifetimeMillis, ExpirationEventHandler<TKey, TValue> expirationEventHandler) {
		this(lifetimeMillis, expirationEventHandler, true);
	}
	
	public ExpiringCache(long lifetimeMillis) {
		this(lifetimeMillis, null);
	}
	
	public synchronized void enableExpiry() {
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
		CacheEntry entry = new CacheEntry(data);
		synchronized (cache) {
			cache.put(key, entry);
			cache.notifyAll();
		}
	}
	
	public TValue get(TKey key) {
		CacheEntry entry;
		synchronized (cache) {
			entry = cache.get(key);
			cache.notifyAll();
		}
		return entry != null ? entry.data : null;
	}
	
	public TValue remove(TKey key) {
		CacheEntry entry;
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
			for (Iterator<Entry<TKey, CacheEntry>> it = cache.entrySet().iterator(); it.hasNext();) {
				Entry<TKey, CacheEntry> entry = it.next();
				CacheEntry cacheEntry = entry.getValue();
	
				diff = cacheEntry.expiry - System.currentTimeMillis();
				if (diff > 0) {
					break; // time remains, exit clean up loop
				}
	
				it.remove();
				expirationEventHandler.onExpiration(entry.getKey(), cacheEntry.data);
			}
		}
		
		return diff;
	}
	
	private class Cleaner implements Runnable {
		@Override
		public void run() {
			synchronized (cache) {
				while (runCleaner) {
					try {
						if (Thread.interrupted()) {
							throw new InterruptedException();
						}
						long diff = cleanup();
						if (diff > -1) {
							cache.wait(diff); // wake self up when last tested item expired
						}
					}
					catch (InterruptedException ex) {
						runCleaner = false;
					}
				}
			}
		}
	}
	
}