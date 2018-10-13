class ThreadPool {

    private Map<Long, PoolThread> pool = new ConcurrentHashMap<>();

    final private int capacity;

    public ThreadPool(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity should be greater than zero");
        }
        this.capacity = capacity;
    }

    public void submitTask(Runnable task) throws InterruptedException {
        synchronized (this) {
            while (pool.size() >= capacity) {
                this.wait();
            }
            PoolThread th = new PoolThread(task, this);
            pool.put(th.getId(), th);
            th.start();
        }
    }

    public void join() throws InterruptedException {
        synchronized (this) {
            while (pool.size() > 0) {
                wait();
            }
        }
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
                pool.remove(this.getId());
            } catch (Exception ex) {
                ex.printStackTrace();
                pool.remove(this.getId());
                try {
                    this.join();
                } catch (InterruptedException ex1) {
                    Logger.getLogger(ThreadPool.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
            synchronized (threadPool) {
                threadPool.notify();
            }
        }
    }
}
