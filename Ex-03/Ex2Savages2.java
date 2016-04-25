import java.util.concurrent.locks.*;

public class Ex2Savages2 {
	public static class Portion {
	volatile int val = -1;
	volatile boolean isPortionFree = true;
	Portion(int inVal) {
		val = inVal;
	}
	
	void markUsed() {
		isPortionFree = false;
	}
	
	// To be called by Cook (Producer)
	void setVal(int inVal) {
		val = inVal;
		isPortionFree = true;
	}
	
	boolean isFree() {
		return isPortionFree;
	}
	
	int getVal() {
		return val;
	}
};
public static class ThreadShared {
	int numOfThreads;
	volatile int threadsPending;
	
	ReentrantLock  lock;
	ThreadShared(int totalNumOfThreads)
	{
		numOfThreads = totalNumOfThreads;
		threadsPending = totalNumOfThreads;
		lock = new ReentrantLock();
	}
	
	synchronized void setThreadExecuted(int threadNum) {
		threadsPending--;
		System.out.println("Executed thread " + threadNum + ". Pending threads count =  " + threadsPending);
	}
	
	boolean allExecuted() {
		//return ((executionChecker ^ executed) == 0);
		return (threadsPending == 0);
	}
	
	void resetExector() {
		lock.lock();
		if(threadsPending == 0) {
			//executed = 0;
			threadsPending = numOfThreads;
			
			System.out.println(" Reset executor ");
		}
		lock.unlock();
	}
}
public static class Pot {
	ReentrantLock portionLock[];
	int totalNumOfPortions;
	volatile int remainingPortions;
	Portion[] nPortionPot;
	volatile boolean continueExec = true;
	int refillCount = 0;
	
	Pot(int numOfPortions) {
		totalNumOfPortions = numOfPortions;
		remainingPortions = numOfPortions;
		nPortionPot = new Portion[numOfPortions];
		portionLock = new ReentrantLock[numOfPortions];
		for(int i = 0; i < numOfPortions; i++) {
			nPortionPot[i] = new Portion(i);
			portionLock[i] = new ReentrantLock();
		}
	}
	
	boolean isEmpty()
	{
		if(remainingPortions == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	boolean isPortionFree(int index)
	{
		return nPortionPot[index].isFree();
	}
	
	synchronized void decrementPortionCount() {
		remainingPortions--;
	}
	
	synchronized void refillPot() {
		if(remainingPortions == 0) {
			if(refillCount == 15) {
				continueExec = false;
			} else {
				for(int i = 0; i < totalNumOfPortions; i++) {
					nPortionPot[i].setVal(i);
				}
				remainingPortions = totalNumOfPortions;
				refillCount++;
				System.out.println("Refilled Pot");
			}
		}
	}
	
	boolean eatPortion(int index, int threadNum) {
		//System.out.println("Thread num : " + threadNum + " trying to eat portion num " + nPortionPot[index].getVal());
		portionLock[index].lock();
		if(!nPortionPot[index].isFree()) { 
			//System.out.println("Thread num : " + threadNum + " denied portion num " + nPortionPot[index].getVal());
			portionLock[index].unlock();
			return false;
		}
		System.out.println("Thread num : " + threadNum + " at portion num " + nPortionPot[index].getVal());
		nPortionPot[index].markUsed();
		decrementPortionCount();
		portionLock[index].unlock();
		return true;
	}
};

public static class Savage implements Runnable { 
	private Pot sharedPot;
	private int myThreadIndex;
	ThreadShared sharedThreadData;
    public Savage(Pot  potObj, ThreadShared shdThreadData, int threadNum) { 
    	sharedPot = potObj;
    	sharedThreadData = shdThreadData;
    	myThreadIndex = threadNum;
    }
	
    public void run() {
    	int myPortionIndex = myThreadIndex%sharedPot.totalNumOfPortions;
    	boolean executed = false;
    	int i = 1;
    	while(true/*sharedPot.continueExec*/) {
	    	while(!executed) {
	    		while(!sharedPot.isPortionFree(myPortionIndex)) {
	    	   		if(sharedPot.isEmpty()) {
	    	   			sharedPot.refillPot();
	    	   			continue;
	    	   		}
	    	   		myPortionIndex = (myThreadIndex+i++)%sharedPot.totalNumOfPortions;
	    	   	}
	    	   	//Reached here means got a free portion
	    	   	if(sharedPot.eatPortion(myPortionIndex, myThreadIndex) == true) {
	    	   		executed = true;
	    	   	} else { // Retry
	    	   		i = 1;
	    	   		myPortionIndex = (myThreadIndex+i)%sharedPot.totalNumOfPortions;
	    	   	}
	    	}
	    	sharedThreadData.setThreadExecuted(myThreadIndex);
	    	while(!sharedThreadData.allExecuted()) {}
	    	sharedThreadData.resetExector();
	    	executed = false;
	    	i = 1;
    	}
    }
};

//	public static void printUsage() {
//		System.out.println("java Ex2Savages1 <numOfPortions> <numOfThreads>");
//	}

	public static void main(String[] args) {
		if(args.length != 2) {
			System.out.println("Usage : java Ex2Savages1 <numOfPortions> <numOfThreads>");
			return;
		}
		int numPortions = 0, numThreads = 0;
		try {
	        numPortions = Integer.parseInt(args[0]);
	        numThreads = Integer.parseInt(args[1]);
	    } catch (NumberFormatException e) {
	        System.err.println("Argument must be an integer.");
	        System.exit(1);
	    }
		
		if(numPortions >= numThreads) {
			System.out.println("Num of portions must be less than number of threads.. Exiting");
			return;
		}
			
		Pot foodPot = new Pot(numPortions);
		ThreadShared sharedThreadData = new ThreadShared(numThreads);
		Thread savageThreads[] = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			savageThreads[i] = new Thread(new Savage(foodPot, sharedThreadData, i+1));
		}	
		
		for(int i = 0; i < numThreads; i++) {
			savageThreads[i].start();
		}
		for(int i = 0; i < numThreads; i++) {
			try {
				savageThreads[i].join();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
};
