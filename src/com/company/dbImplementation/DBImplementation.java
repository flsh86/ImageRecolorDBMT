package com.company.dbImplementation;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.locks.ReentrantReadWriteLock.*;

public class DBImplementation {
    public static final int HIGHEST_PRICE = 1000;
    public static void main(String[] args) {
        InventoryDataBase inventoryDataBase = new InventoryDataBase();

        Random random = new Random();
        for(int i = 0; i < 10000; i++) {
            inventoryDataBase.addItem(random.nextInt(HIGHEST_PRICE));
        }

        Thread writer = new Thread(() -> {
            while (true) {
                inventoryDataBase.addItem(random.nextInt(HIGHEST_PRICE));
                inventoryDataBase.removeItem(random.nextInt(HIGHEST_PRICE));

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
            }
        });

        writer.setDaemon(true);
        writer.start();

        int numberOfReadingThreads = 7;
        List<Thread> readingThreads = new ArrayList<>();

        for(int i = 0; i < numberOfReadingThreads; i++) {
            Thread reader = new Thread(() -> {
                for(int j = 0; j < 10000; j++) {
                    int upperBoundPrice = random.nextInt(HIGHEST_PRICE);
                    int lowerBoundPrice = upperBoundPrice > 0 ? random.nextInt(upperBoundPrice) : 0;
                    inventoryDataBase.getNumberOfItemsInPriceRange(lowerBoundPrice, upperBoundPrice);
                }
            });
            reader.setDaemon(true);
            readingThreads.add(reader);
        }

        long startReadingTime = System.currentTimeMillis();
        for(Thread t : readingThreads) {
            t.start();
        }

        for(Thread t : readingThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }

        long endReadingTime = System.currentTimeMillis();
        System.out.println(String.format("Reading took %d ms ", endReadingTime - startReadingTime));
    }

    public static class InventoryDataBase{
        private TreeMap<Integer, Integer> priceToQuantityMap = new TreeMap<>();
        private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        private ReadLock readLock = rwLock.readLock();
        private WriteLock writeLock = rwLock.writeLock();

        public InventoryDataBase() {
        }

        public InventoryDataBase(TreeMap<Integer, Integer> priceToQuantityMap) {
            this.priceToQuantityMap = priceToQuantityMap;
        }

        public int getNumberOfItemsInPriceRange(int lowerBound, int upperBound) {
            readLock.lock();
            try{
                Integer fromKey = priceToQuantityMap.ceilingKey(lowerBound);
                Integer toKey = priceToQuantityMap.floorKey(upperBound);

                if(fromKey == null || toKey == null) {
                    return 0;
                }

                NavigableMap<Integer, Integer> navigableMap = priceToQuantityMap.subMap(fromKey, true, toKey, true);

                int counter = 0;
                for(int numberOfItemsForPrice : navigableMap.values()) {
                    counter += numberOfItemsForPrice;
                }

                return counter;
            } finally {
                readLock.unlock();
            }

        }

        public void addItem(int price) {
//            priceToQuantityMap.merge(price, 1, Integer::sum);
            writeLock.lock();
            try{
                Integer numberOfItemsForPrice = priceToQuantityMap.get(price);
                if(numberOfItemsForPrice == null) {
                    priceToQuantityMap.put(price, 1);
                } else {
                    priceToQuantityMap.merge(price,1, Integer::sum);
                }
            } finally {
                writeLock.unlock();
            }
        }

        public void removeItem(int price) {
            writeLock.lock();
            try{
                Integer numberOfItemsForPrice = priceToQuantityMap.get(price);
                if(numberOfItemsForPrice == null || numberOfItemsForPrice == 1) {
                    priceToQuantityMap.remove(price);
                } else {
                    priceToQuantityMap.put(price, numberOfItemsForPrice - 1);
                }
            } finally {
                writeLock.unlock();
            }
        }

    }
}
