class ThreadPool {

    final private AtomicInteger poolCount;

    final private int capacity;

    public ThreadPool(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity should be greater than zero");
        }
        this.capacity = capacity;
        this.poolCount = new AtomicInteger(this.capacity);
    }

    public void submitTask(Runnable task) throws InterruptedException {
        synchronized (this) {
            while (poolCount.get() <= 0) {
                this.wait();
            }
            PoolThread th = new PoolThread(task, this);
            poolCount.getAndDecrement();
            th.start();
        }
    }

    public void join() throws InterruptedException {
        synchronized (this) {
            while (poolCount.get() < capacity) {
                wait();
            }
        }
    }

    public int aliveThreads() {
        return capacity - poolCount.get();
    }

    private class PoolThread extends Thread {

        public PoolThread(Runnable task, ThreadPool threadPool) {
            this.task = task;
            this.threadPool = threadPool;
        }

        final private Runnable task;
        final private ThreadPool threadPool;

        @Override
        public void run() {
            try {
                task.run();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                poolCount.getAndIncrement();
                synchronized (threadPool) {
                    threadPool.notify();
                }
            }

        }
    }
}
