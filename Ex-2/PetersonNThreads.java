import java.util.BitSet;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class PetersonNThreads {
	static final int COUNTER_LIMIT = 200;
	static class SharedThread 
	{
		volatile int sharedCounter = 0;
		AtomicIntegerArray victimArray;
		BitSet[] flagArray ;
		Integer[] threadExecCount;
		int numThreads;
		SharedThread(int numOfThreads) {
			numThreads = numOfThreads;
			//flagArray = new AtomicIntegerArray(numThreads - 1);
			flagArray = new BitSet[numThreads - 1];
			threadExecCount = new Integer[numThreads];
			victimArray = new AtomicIntegerArray(numThreads);
			for(int i = 0; i < (numThreads - 1); i++) {
				flagArray[i] = new BitSet(numThreads);
				threadExecCount[i] = 0;
			}
			threadExecCount[numThreads-1] = 0;
		}
		
		int getCounter() { return sharedCounter; }
		
		void incCounter() { if (sharedCounter < COUNTER_LIMIT) ++sharedCounter; }
		
	};
	
	static class MutexThread implements Runnable 
	{
		SharedThread sharedObject;
		int myThreadNum;
		MutexThread(SharedThread sharedData, int threadNum)
		{
			this.sharedObject = sharedData;
			this.myThreadNum = threadNum;
		}
		
		public void run() {
			while(sharedObject.getCounter() < COUNTER_LIMIT) {
				enterCS();
				if(sharedObject.getCounter() < COUNTER_LIMIT)
					updateVals();
				leaveCS();
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
			sharedObject.incCounter();
			System.out.println("Thread " + myThreadNum + " set value to  " + sharedObject.getCounter() + "\n");
			sharedObject.threadExecCount[myThreadNum]++;
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
