import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class ThreadPool {

    final private AtomicInteger poolCount;

    final private int capacity;
    
    private ReentrantLock submitTaskRLock = new ReentrantLock();
    
    private ReentrantLock joinRLock = new ReentrantLock();

    public ThreadPool(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity should be greater than zero");
        }
        this.capacity = capacity;
        this.poolCount = new AtomicInteger(this.capacity);
    }

    public void submitTask(Runnable task) throws InterruptedException {
        synchronized(submitTaskRLock) {
            try {
                while (poolCount.get() <= 0) {
                    submitTaskRLock.wait();
                }
                PoolThread th = new PoolThread(task, this);
                poolCount.getAndDecrement();
                th.start();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private class SubmitMultipleTask implements Runnable {
        
        ThreadPool threadPool;
        Runnable[] task;
        
        public SubmitMultipleTask(ThreadPool threadPool, Runnable[] task) {
            this.threadPool = threadPool;
            this.task = task;
        }
        
        @Override
        public void run() {
            try {
                int N = task.length;
                for(int i = 0; i < N; i++) {
                    threadPool.submitTask(task[i]);
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void submitTask(Runnable[] task) throws InterruptedException {
        submitTask(new SubmitMultipleTask(this, task));
    }

    public void join() throws InterruptedException {
        synchronized(joinRLock) {
            while (poolCount.get() < capacity) {
                joinRLock.wait();
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
                synchronized (threadPool.submitTaskRLock) {
                    threadPool.submitTaskRLock.notify();
                }
            }

        }
    }
}
