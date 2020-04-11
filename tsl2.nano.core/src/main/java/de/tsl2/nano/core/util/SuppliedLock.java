package de.tsl2.nano.core.util;

import java.util.concurrent.locks.StampedLock;

/**
 * provides simplified use of a StampedLock
 */
public class SuppliedLock {
    StampedLock lock = new StampedLock();

    public void write(SupplierEx<?> supplierEx) {
        long stamp = lock.writeLock();
        try {
            supplierEx.get();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void read(SupplierEx<?> supplierEx) {
        long stamp = lock.readLock();
        try {
            supplierEx.get();
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
