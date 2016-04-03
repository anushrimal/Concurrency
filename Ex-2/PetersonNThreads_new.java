import java.util.Scanner;
import java.util.concurrent.atomic.AtomicIntegerArray;


public class PetersonNThreads_new {
	static final int COUNTER_LIMIT = 50;
	static class SharedThread 
	{
		volatile int sharedCounter;
		AtomicIntegerArray victimArray;
		AtomicIntegerArray flagArray ;  // The index of the array represents level in algorithm
		                                // Each integer in the array 
		Integer[] threadExecCount;
		int numThreads;
		SharedThread(int numOfThreads) {
			numThreads = numOfThreads;
			flagArray = new AtomicIntegerArray(numThreads - 1);
			threadExecCount = new Integer[numThreads];
			victimArray = new AtomicIntegerArray(numThreads);
			for(int i = 0; i < (numThreads - 1); i++) {
			//	flagArray[i] = new BitSet(numThreads);
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
			int myBitVal = 1 << myThreadNum;
			for(int i = 0; i < (sharedObject.numThreads - 1); i++ ) {
				sharedObject.flagArray.accumulateAndGet(i, myBitVal, ((a,b)->a|b) );
				sharedObject.victimArray.set(i, myThreadNum);
				/*for(int j = i; j <(sharedObject.numThreads - 1); j++) {
						if((sharedObject.flagArray.get(j) ^ myBitVal) > 0 && sharedObject.victimArray.get(i) == myThreadNum) {
							j = i;
						}
			    }*/
				while( (sharedObject.flagArray.get(i)^ myBitVal) > 0/*Other than itself*/  && sharedObject.victimArray.get(i) == myThreadNum) {
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
			int myBitVal = 1 << myThreadNum;
			myBitVal = ~myBitVal; // To clear only this thread's bits
			for(int i = 0; i < (sharedObject.numThreads -1); i++ ) {
				sharedObject.flagArray.accumulateAndGet(i, myBitVal, ((a,b)-> a&b));
			}
		}
	
	};
	

	public static void main(String[] args) {
		System.out.println("Enter total no. of n and number of processors");
		Scanner scan = new Scanner(System.in);
		int n = scan.nextInt();
		int num_procs = scan.nextInt();
		if(num_procs == 1)
			setSolarisAffinity();
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
		int tot_cnt = 0;
		for(int i = 0; i < n; i ++) {
			System.out.println("Count of accesses by thread " + i + " = " + sharedObj.threadExecCount[i] + "\n");
			tot_cnt += sharedObj.threadExecCount[i];
		}
		System.out.println("Count of total accesses by threads = " + tot_cnt + "\n");
	}

	public static <id> void setSolarisAffinity() {
		try {
			// retrieve process id 
			String pid_name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
			String [] pid_array = pid_name.split("@");
			int pid = Integer.parseInt( pid_array[0] );
			// random processor
			int processor = new java.util.Random().nextInt( 32 );
			// Set process affinity to one processor (on Solaris)
			Process p = Runtime.getRuntime().exec("/usr/sbin/pbind -b ");
			p.waitFor();
		}
		catch (Exception err) {
			err.printStackTrace();
		}
	}
};
