package org.pentaho.di.core.hash;

import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;

public class ByteArrayHashIndex {
	
	private static final int   STANDARD_INDEX_SIZE =  512;
	private static final float STANDARD_LOAD_FACTOR = 0.78f;
	
	private RowMetaInterface keyRowMeta;
	private ByteArrayHashIndexEntry[] index;
	private int size;
	private int resizeThresHold;
	
	/**
	 * Create a Byte array hash index to store row
	 * @param rowMeta
	 */
	public ByteArrayHashIndex(RowMetaInterface keyRowMeta, int size) {
		this.keyRowMeta = keyRowMeta;
		
		// Find a suitable capacity being a factor of 2:
		int factor2Size = 1;
		while (factor2Size<size) factor2Size<<=1; // Multiply by 2
		
		this.size = factor2Size;
		this.resizeThresHold = (int)(factor2Size*STANDARD_LOAD_FACTOR);
		
		index = new ByteArrayHashIndexEntry[factor2Size];
	}
	
	public ByteArrayHashIndex(RowMetaInterface keyRowMeta) {
		this(keyRowMeta, STANDARD_INDEX_SIZE);
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isEmpty() {
		return size==0;
	}
	
	 public byte[] get(byte[] key) throws KettleValueException {
		int hashCode = generateHashCode(key, keyRowMeta);

		int indexPointer = hashCode & (index.length - 1);
		ByteArrayHashIndexEntry check = index[indexPointer];
		
		while (check!=null) {
			if (check.hashCode == hashCode && check.equalsKey(key)) {
				return check.value;
			}
			check = check.nextEntry;
		}
		return null;
	}
	 
    public void put(byte[] key, byte[] value) throws KettleValueException {
		int hashCode = generateHashCode(key, keyRowMeta);
		int indexPointer = hashCode & (index.length - 1);
		
		// First see if there is an entry on that pointer...
		//
		boolean searchEmptySpot = false;
		
		ByteArrayHashIndexEntry check = index[indexPointer];
		ByteArrayHashIndexEntry previousCheck = null;

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
		index[indexPointer] = new ByteArrayHashIndexEntry(hashCode, key, value, index[indexPointer]);
		
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
    	// Increase the size of the index...
    	//
    	size++;
    	
    	// See if we've reached our resize threshold...
    	//
		if (size >= resizeThresHold) {
			
			ByteArrayHashIndexEntry[] oldIndex = index;
			
			// Double the size to keep the size of the index a factor of 2...
			// Allocate the new array...
			//
			int newSize = 2 * index.length;

			ByteArrayHashIndexEntry[] newIndex = new ByteArrayHashIndexEntry[newSize];

			// Loop over the old index and re-distribute the entries
			// We want to make sure that the calculation 
			//     entry.hashCode & ( size - 1) 
			// ends up in the right location after re-sizing...
			//
			for (int i = 0; i < oldIndex.length; i++) {
				ByteArrayHashIndexEntry entry = oldIndex[i];
				if (entry != null) {
					oldIndex[i] = null;
					
					// Make sure we follow all the linked entries...
					// This is a bit of extra work, TODO: see how we can avoid it!
					// 
					do {
						ByteArrayHashIndexEntry next = entry.nextEntry;
						int indexPointer = entry.hashCode & (newSize - 1);
						entry.nextEntry = newIndex[indexPointer];
						newIndex[indexPointer] = entry;
						entry = next;
					} 
					while (entry != null); 
				}
			}
			
			// Replace the old index with the new one we just created...
			//
			index = newIndex;
			
			// Also change the resize threshold...
			//
			resizeThresHold = (int) (newSize * STANDARD_LOAD_FACTOR);
		}
	}
	 
    public static int generateHashCode(byte[] key, RowMetaInterface rowMeta) throws KettleValueException
    {
    	Object[] rowData = RowMeta.getRow(rowMeta, key);
        return rowMeta.hashCode(rowData);
    }
	
	private static final class ByteArrayHashIndexEntry {
		private int hashCode;
		private byte[] key;
		private byte[] value;
		private ByteArrayHashIndexEntry nextEntry;
		
		/**
		 * @param hashCode
		 * @param key
		 * @param value
		 * @param nextEntry
		 */
		public ByteArrayHashIndexEntry(int hashCode, byte[] key, byte[] value, ByteArrayHashIndexEntry nextEntry) {
			this.hashCode = hashCode;
			this.key = key;
			this.value = value;
			this.nextEntry = nextEntry;
		}
		
        public boolean equalsKey(byte[] cmpKey)
        {
            return equalsByteArray(key, cmpKey);
        }
        
        /**
         * The row is the same if the value is the same
         * The data types are the same so no error is made here.
         */
        public boolean equals(Object obj)
        {
        	ByteArrayHashIndexEntry e = (ByteArrayHashIndexEntry)obj;

            return equalsValue(e.value);
        } 

        public boolean equalsValue(byte[] cmpValue)
        {
            return equalsByteArray(value, cmpValue);
        }
		
        public static final boolean equalsByteArray(byte[] value, byte[] cmpValue)
        {
            if (value.length != cmpValue.length) return false;
            for (int i=value.length-1;i>=0;i--)
            {
                if (value[i] != cmpValue[i]) return false;
            }
            return true;
        }
		
	}
}
