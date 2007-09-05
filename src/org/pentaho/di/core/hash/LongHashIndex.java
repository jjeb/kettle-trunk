package org.pentaho.di.core.hash;

import org.pentaho.di.core.exception.KettleValueException;

public class LongHashIndex {
	
	private static final int   STANDARD_INDEX_SIZE =  512;
	private static final float STANDARD_LOAD_FACTOR = 0.78f;
	
	private LongHashIndexEntry[] index;
	private int size;
	private int resizeThresHold;
	
	/**
	 * Create a new long/long hash index
	 * @param size the initial size of the hash index
	 */
	public LongHashIndex(int size) {
		
		// Find a suitable capacity being a factor of 2:
		int factor2Size = 1;
		while (factor2Size<size) factor2Size<<=1; // Multiply by 2
		
		this.size = factor2Size;
		this.resizeThresHold = (int)(factor2Size*STANDARD_LOAD_FACTOR);
		
		index = new LongHashIndexEntry[factor2Size];
	}
	
	/**
	 * Create a new long/long hash index
	 */
	public LongHashIndex() {
		this(STANDARD_INDEX_SIZE);
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isEmpty() {
		return size==0;
	}
	
	 public Long get(Long key) throws KettleValueException {
		int hashCode = generateHashCode(key);

		int indexPointer = hashCode & (index.length - 1);
		LongHashIndexEntry check = index[indexPointer];
		
		while (check!=null) {
			if (check.hashCode == hashCode && check.equalsKey(key)) {
				return check.value;
			}
			check = check.nextEntry;
		}
		return null;
	}
	 
    public void put(Long key, Long value) throws KettleValueException {
		int hashCode = generateHashCode(key);
		int indexPointer = hashCode & (index.length - 1);
		
		// First see if there is an entry on that pointer...
		//
		boolean searchEmptySpot = false;
		
		LongHashIndexEntry check = index[indexPointer];
		LongHashIndexEntry previousCheck = null;

		while (check!=null) {
			searchEmptySpot = true;
			
			// If there is an identical entry in there, we replace the entry
			// And then we just return...
			//
			if (check.hashCode == hashCode && check.equalsKey(key)) {
				check.value = value;
				return;
			}
			previousCheck = check;
			check = check.nextEntry;
		}
		
		// If we are still here, that means that we are ready to put the value down...
		// Where do we need to search for an empty spot in the index?
		//
		while (searchEmptySpot) {
			indexPointer++;
			if (indexPointer>=size) indexPointer=0;
			if (index[indexPointer]==null) {
				searchEmptySpot=false;
			}
		}

		// OK, now that we know where to put the entry, insert it...
		//
		index[indexPointer] = new LongHashIndexEntry(hashCode, key, value, index[indexPointer]);
		
		// Don't forget to link to the previous check entry if there was any...
		//
		if (previousCheck!=null) {
			previousCheck.nextEntry = index[indexPointer];
		}
		
		// If required, resize the table...
		//
		resize();
	}
    
    private final void resize() {
		if (size++ >= resizeThresHold) {
			LongHashIndexEntry[] oldIndex = index;
			
			int newSize = 2 * index.length;

			LongHashIndexEntry[] newIndex = new LongHashIndexEntry[newSize];

			for (int i = 0; i < oldIndex.length; i++) {
				LongHashIndexEntry entry = oldIndex[i];
				if (entry != null) {
					oldIndex[i] = null;
					do {
						LongHashIndexEntry next = entry.nextEntry;
						int indexPointer = entry.hashCode & (newSize - 1);
						entry.nextEntry = newIndex[indexPointer];
						newIndex[indexPointer] = entry;
						entry = next;
					} 
					while (entry != null);
				}
			}
			index = newIndex;
			resizeThresHold = (int) (newSize * STANDARD_LOAD_FACTOR);
		}
	}
	 
    public static int generateHashCode(Long key) throws KettleValueException
    {
    	return key.hashCode();
    }
	
	private static final class LongHashIndexEntry {
		private int hashCode;
		private Long key;
		private Long value;
		private LongHashIndexEntry nextEntry;
		
		/**
		 * @param hashCode
		 * @param key
		 * @param value
		 * @param nextEntry
		 */
		public LongHashIndexEntry(int hashCode, Long key, Long value, LongHashIndexEntry nextEntry) {
			this.hashCode = hashCode;
			this.key = key;
			this.value = value;
			this.nextEntry = nextEntry;
		}
		
        public boolean equalsKey(Long cmpKey)
        {
            return key.equals(cmpKey);
        }
        
        /**
         * The row is the same if the value is the same
         * The data types are the same so no error is made here.
         */
        public boolean equals(LongHashIndexEntry entry)
        {
            return entry.key.equals(key);
        } 

        public boolean equalsValue(Long cmpValue)
        {
            return value.equals(cmpValue);
        }
	}
}
