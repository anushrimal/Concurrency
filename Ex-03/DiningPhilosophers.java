import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

/* This is based on Chandy-Misra solution algorithm for the problem*/

public class DiningPhilosophers {
//Halt condition
//public static final int TOTAL_RUNS = 10; // Assuming the total number of servings is limited by this number

public static class Fork {
	AtomicBoolean isClean; // A philosopher would eat only when the fork is clean
	AtomicInteger requestedBy; // Index of philosopher requesting for the fork
	AtomicInteger ownedBy; // Index of philosopher currently holding the fork
	int f_id; // Index of fork in forks array
	ReentrantLock forkLock; // Lock to access forkIsFree condition
	Condition forkIsFree; // Condition to get/set ownership of fork
		
	Fork(int forkId) {
		f_id = forkId;
		isClean = new AtomicBoolean();
		isClean.set(false); // Initially, all forks are dirty
		requestedBy = new AtomicInteger();
		requestedBy.set(-1); // No one requests it
		ownedBy = new AtomicInteger();
		ownedBy.set(-1); // No one owns it
		forkLock = new ReentrantLock();
		forkIsFree = forkLock.newCondition();
	}
	
	// To be done by philosopher who owns a dirty fork
	boolean releaseFork(int philId)
	{
		if(ownedBy.get() == philId) {
			if(isClean.get()) { // Do not give up the fork if it is clean
				return  false;
			} else { //Fork is dirty. Clean it and send it to the neighbor
				isClean.set(true);
				forkLock.lock();
				{
					ownedBy.set(requestedBy.get()); // Transfer fork to neighbor
					requestedBy.set(philId); // Become requester of fork once neighbor is done using it
					forkIsFree.signalAll(); // Signal neighbor about change in ownership
				}
				forkLock.unlock();
				return true;
			}
		} 
		return false;	// Not the owner of the fork	
	}
	
	// To be done by philosopher who want to own a clean fork to eat
	boolean getFork(int philId) {
		forkLock.lock();
		while(ownedBy.get() != philId) { // Wait until made owner of the fork by neighbor
			try {
				forkIsFree.await();
			} catch(InterruptedException e) {
				continue;
			}
		}
		forkLock.unlock();
		if(!isClean.get()) { // Check if received a clean fork
			return false; 
		}
		return true; // Acquired a clean fork 
	}
};

public static class Philosopher implements Runnable {
	int p_id;
	Fork leftFork;
	Fork rightFork;
	AtomicBoolean haveLeft, haveRight; // Ownership flags
	volatile int runCounter = 0; 
	AtomicBoolean exit;

	Philosopher(int philId, int leftPhilIndex, Fork left, int rightPhilIndex, Fork right) {
		p_id = philId; 
		haveLeft = new AtomicBoolean();
		haveRight = new AtomicBoolean();
		leftFork = left;
		rightFork = right;
		//Initially give both the forks to lower indexed philosopher
		if(p_id < leftPhilIndex) { 
			haveLeft.set(true);  
			leftFork.ownedBy.set(p_id); // Own left fork
		} else {
			haveLeft.set(false); 
			leftFork.requestedBy.set(p_id); // Request left fork
		}
		if(p_id < rightPhilIndex) { 
			haveRight.set(true);
			rightFork.ownedBy.set(p_id); // Own right fork
		} else {
			haveRight.set(false);
			rightFork.requestedBy.set(p_id); // Request right fork
		}
		exit = new AtomicBoolean();
		exit.set(false);
	}
	public void run() {
		//while(runCounter++ < TOTAL_RUNS && !exit.get()) 
		while(true) { // Loop until TOTAL_RUNS refills 
			if(haveLeft.get() && !leftFork.isClean.get()) {
				haveLeft.set(false);
				leftFork.releaseFork(p_id);
				continue;
			} 				
			
			if(haveRight.get() && !rightFork.isClean.get()) {
				haveRight.set(false);
				rightFork.releaseFork(p_id);
				continue;
			}
			while(!haveLeft.get()) {
				haveLeft.set(leftFork.getFork(p_id));
			}
			
			while(!haveRight.get()) {
				haveRight.set(rightFork.getFork(p_id));
			}
			eat(runCounter);
			leftFork.isClean.set(false);
			rightFork.isClean.set(false);
			
			sleep();
			
		}
		//exit.set(true);
	}
	
	void eat(int round) {
		System.out.println("Philosopher " + p_id + " eating round " + round + " with forks " + leftFork.f_id + " & " + rightFork.f_id);
	}
	
	void sleep() {
		//System.out.println("Philosopher " + p_id + " sleeping");
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			
		}
	}
};

	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Usage : java DiningPhilosophers <numOfPhilosophers>");
			return;
		}
		int numOfPhilosophers = 0;
		try {
	        numOfPhilosophers = Integer.parseInt(args[0]);
	    } catch (NumberFormatException e) {
	        System.err.println("Argument must be an integer.");
	        System.exit(1);
	    }
		//Initialize Forks
		Fork forks[] = new Fork[numOfPhilosophers];
		for(int i = 0; i < numOfPhilosophers; i++) {
			forks[i] = new Fork(i);
		}
		//Initialize Philosophers 
		Thread philosophers[] = new Thread[numOfPhilosophers];
		int rightIndex = 0, leftIndex = 0;
		for(int i = 0; i < numOfPhilosophers; i++) {
			if(i == 0) {
				rightIndex = numOfPhilosophers - 1;
			} else {
				rightIndex = i - 1;
			}
			if(i == (numOfPhilosophers -1) ) {
				leftIndex = 0;
			} else {
				leftIndex = (i + 1);
			}
			philosophers[i] = new Thread(new Philosopher(i, leftIndex, forks[i], rightIndex, forks[rightIndex]));
		}
		for(int i = 0; i < numOfPhilosophers; i++) {
			philosophers[i].start();
		}
		for(int i = 0; i < numOfPhilosophers; i++) { 
			try {
				philosophers[i].join();
			} catch (InterruptedException e) {
				System.out.println("Philosopher " + i + "had an exception while join");
				
			}
		}
		return;
	}
};
