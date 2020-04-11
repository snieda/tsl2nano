package de.tsl2.nano.core.util;

import java.util.concurrent.locks.StampedLock;

/**
 * provides simplified use of a StampedLock
 */
public class SuppliedLock {
    StampedLock lock = new StampedLock();

    public <T> T write(SupplierEx<T> supplierEx) {
        long stamp = lock.writeLock();
        try {
            return supplierEx.get();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public <T> T read(SupplierEx<T> supplierEx) {
        long stamp = lock.readLock();
        try {
            return supplierEx.get();
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
