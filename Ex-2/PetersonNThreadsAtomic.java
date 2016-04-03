import java.util.BitSet;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicInteger;

public class PetersonNThreadsAtomic {
	static final int COUNTER_LIMIT = 100;
	static class SharedThread 
	{
		AtomicInteger sharedCounter;
		AtomicIntegerArray victimArray;
		volatile BitSet[] flagArray ;
		Integer[] threadExecCount;
		int numThreads;
		SharedThread(int numOfThreads) {
			numThreads = numOfThreads;
			flagArray = new BitSet[numThreads - 1];
			threadExecCount = new Integer[numThreads];
			victimArray = new AtomicIntegerArray(numThreads);
			sharedCounter = new AtomicInteger(0);
			for(int i = 0; i < (numThreads - 1); i++) {
				flagArray[i] = new BitSet(numThreads);
				threadExecCount[i] = 0;
			}
			threadExecCount[numThreads-1] = 0;
		}
		
		//int getCounter() { return sharedCounter.get(); }
		
		//void incCounter() { if (sharedCounter. < COUNTER_LIMIT) ++sharedCounter; }
		
	};
	
	static class MutexThread implements Runnable 
	{
		SharedThread sharedObject;
		int myThreadNum;
		Boolean updated = false;
		MutexThread(SharedThread sharedData, int threadNum)
		{
			this.sharedObject = sharedData;
			this.myThreadNum = threadNum;
		}
		
		public void run() {
			while(sharedObject.sharedCounter.get() < COUNTER_LIMIT) {
				updated = false;
				enterCS();
				updateVals();
				leaveCS();
				if(updated)
					sharedObject.threadExecCount[myThreadNum]++;
			}
		}
		
		void enterCS() {
			//BitSet myBitVal ;
			for(int i = 0; i < (sharedObject.numThreads - 1); i++ ) {
				sharedObject.flagArray[i].set(myThreadNum);
				sharedObject.victimArray.set(i, myThreadNum);
				//myBitVal.set(myThreadNum);
				while( sharedObject.flagArray[i].cardinality() > 1/*Other than itself*/  && sharedObject.victimArray.get(i) == myThreadNum) {
					continue;
				} 
			}
		}
		
		void updateVals() {
			//C.S.
			if(sharedObject.sharedCounter.get() < COUNTER_LIMIT) {
				//sharedObject.sharedCounter.incrementAndGet();
				updated = true;
				System.out.println("Thread " + myThreadNum + " set value to  " + sharedObject.sharedCounter.incrementAndGet() + "\n");
			}
		}
		
		void leaveCS() {
			for(int i = 0; i < (sharedObject.numThreads -1); i++ ) {
				sharedObject.flagArray[i].clear(myThreadNum);
			}
		}
	};
	

	public static void main(String[] args) {
		System.out.println("Enter total no. of n");
		Scanner scan = new Scanner(System.in);
		int n = scan.nextInt();
		scan.close();
		SharedThread sharedObj = new SharedThread(n);
		Thread threads[] = new Thread[n];
		
		for(int i = 0; i < n; i ++) {
			  threads[i] = new Thread(new MutexThread(sharedObj, i));
		}
		
		for(int i = 0; i < n; i ++) {
			  threads[i].start();
		}
		try {
			for(int i = 0; i < n; i ++) {
				threads[i].join();
			}
		} catch (InterruptedException e ) {}
		System.out.println("Final value of shared counter =" + sharedObj.sharedCounter + "\n");
		for(int i = 0; i < n; i ++) {
			System.out.println("Count of accesses by thread " + i + " = " + sharedObj.threadExecCount[i] + "\n");
		}
	}

};
