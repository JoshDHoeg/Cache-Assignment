/**
 * @author Josh Hoeg
 * Introduction to Computer Organization
 * 
 * Worked with Charlie Clark & Mikhail Bondarenko
 * 
 * This java doc includes two classes, the first is a Block object class that will be used to 
 * hold the tag, data, age, and validity of a specific block in the cache. The second class will
 * implement an actual read only cache.
 */

import java.util.Arrays;


class Block{
	//private member variables
	private boolean validBit;
	private int tagBits;
	private byte[] dataBits;
	private int age;
	
	//default constructor
	Block(int bytesPerBlock){
		validBit = false;
		tagBits = -1;
		dataBits = new byte[bytesPerBlock];
		age = 0;
	}
	
	//constructor
	Block(int TagBits, byte[] DataBits){
		validBit = true;
		tagBits = TagBits;
		dataBits = DataBits;
		age = 1;
	}
	
	//set method
	public void setBlock(byte[] Data, int TagBits, boolean ValidBit){
		validBit = ValidBit;
		tagBits = TagBits;
		dataBits = Data;
		age = 1;
//		System.out.println("setBlock: validBit: " + validBit + ", tagBits: " + tag + ", data: " + Arrays.toString(dataBits) + ", age: " + age);
	}
	
	//get tag for the block
	public int getTagBits(){
//		System.out.println("getTagBits: " + tagBits);
		return tagBits;
	}
	
	//check if the block is valid
	public boolean isValidBit(){
//		System.out.println("isValidBit: " + validBit);
		return validBit;
	}
	
	//get the age in the block
	public int getAge(){
//		System.out.println("getAge: " + age);
		return age;
	}
	
	//get the data in the block
	public byte[] getDataBits(){
//		System.out.println("getDataBits: " + Arrays.toString(dataBits));
		return dataBits;
	}
	
	//set the age of the block
	public void setAge(int Age){
		age = Age;
//		System.out.println("setAge: " + age);
	}
	
	//add one to the age
	public void incrementAge(){
		age++;
//		System.out.println("incrementAge: " + age);
	}
}

public class Cache implements ReadOnlyCache{
	//Member variables
	private Memory Memory;
	private int blockNum, bytesPerBlock, numSets, associativity;
	private Block [][] cache;
	
	
	/**
	 * Constructor
	 * @param memory - An object implementing the Memory functionality.  This should be accessed on a cache miss
	 * @param blockCount - The number of blocks in the cache.
	 * @param bytesPerBlock - The number of bytes per block in the cache.
	 * @param associativity - The associativity of the cache.  1 means direct mapped, and a value of "blockCount" means fully-associative.
	 */
	public Cache(Memory memory, int blockCount, int bytesPerBlockCount, int Associativity){
		Memory = memory;
		blockNum = blockCount;
		bytesPerBlock = bytesPerBlockCount;
		associativity = Associativity;
		
		numSets = blockNum / associativity;
		cache = new Block[numSets][associativity];
		
		for (int s = 0; s < numSets; s++){
			for ( int a = 0; a < associativity; a++){
				
				cache[s][a] = new Block(bytesPerBlock);
				
			}
		}
		
	}

	/**
	 * Method to retrieve the value of the specified memory location.
	 * 
	 * @param address - The address of the byte to be retrieved.
	 * @return The value at the specified address.
	 */
	public byte load(int address)
	{
		//create a temp address to keep the actual address constant
		int tempAddress = address;
		
		//get the offset from the address
		int offset = getOffset(tempAddress, bytesPerBlock);
		tempAddress = tempAddress >> getOffsetShiftAmt(bytesPerBlock);
		
		//get the block index from the address
		int setIndex = getSetIndex(tempAddress, blockNum, associativity);
		tempAddress = tempAddress >> getSetShiftAmt(blockNum, associativity);
		
		//get the tag from the address
		int tag = tempAddress;
		
//		System.out.println("Offset: " + offset + " Set Index: " + setIndex + " Tag: " + tag);
		
		
		for( int a = 0; a < (associativity); a++){
			Block temp = cache[setIndex][a];
//			System.out.println("age: " + temp.getAge() + " tag: " + temp.getTagBits() + " data: " + Arrays.toString(temp.getDataBits()) + " valid: "+ temp.isValidBit());
			
			if(temp.isValidBit() && tag == temp.getTagBits()){
				this.increment();
				cache[setIndex][a].setAge(1);
				return cache[setIndex][a].getDataBits()[offset];
			}
		}
		
		int memAdd = address/bytesPerBlock;
		memAdd *= bytesPerBlock;
		byte [] tempData = Memory.read(memAdd, bytesPerBlock);

		if(associativity != 1){											//Fully Associated	or Set Associated
			this.increment();
			int oldestIndex = this.getOldestIndex(setIndex);
			cache[setIndex][oldestIndex].setBlock(tempData, tag, true);
//			System.out.println("memory read: " + Arrays.toString(cache[setIndex][oldestIndex].getDataBits()));
			return cache[setIndex][oldestIndex].getDataBits()[offset];
		}else{ 															//Direct Mapped
			this.increment();
			cache[setIndex][0].setBlock(tempData, tag, true);
			return cache[setIndex][0].getDataBits()[offset];
		}	
	}

	private static int getOffsetShiftAmt(int bytesPerBlockCount)
	{
		int numBits = (int) (Math.log((double)bytesPerBlockCount)/Math.log(2));
//		System.out.println("getOffsetShiftAmt: " + numBits);
		return numBits;
	}
	
	private int getOffset(int address, int bytesPerBlockCount) {
		int offset = (mask(0, getOffsetShiftAmt(bytesPerBlockCount)-1)) & address;
//		System.out.println("getOffset: " + offset);
		return offset;
	}

	private static int getSetShiftAmt(int blockNum, int associativity)
	{
		int numBits = (int) (Math.log((double)blockNum/associativity)/Math.log(2));
//		System.out.println("getSetShiftAmt: " + numBits);
		return numBits;
	}

	private static int getSetIndex(int address, int blockNum, int associativity)
	{
		int setIndex = (mask(0, getSetShiftAmt(blockNum, associativity) - 1)) & address;
//		System.out.println("getSet: " + setIndex);
		return setIndex;
	}
	
	private int getOldestIndex(int setIndex)
	{
		int oldestIndex = 0; 
		int oldest = 1;
		
		for(int i = 0; i < associativity; i++){
			cache[setIndex][i].incrementAge();
			if(oldest < cache[setIndex][i].getAge()){
				oldest = cache[setIndex][i].getAge();
				oldestIndex = i;
//				System.out.println("getOldestIndex: " + i);
			}
			if(cache[setIndex][i].getAge() == 1){
//				System.out.println("getOldestIndex:  " + i);
				return i;
			}
		}
//		System.out.println("getOldestIndex: " + oldestIndex);
		return oldestIndex;
	}
	
	private static int mask(int from, int to)
	{
		int mask = 0;
		for (int i = from; i < to + 1; i++){
			mask |= 1 << i;
		}
//		System.out.println("createMask: " + mask);
		return mask;
	}
	
	private void printAges(){
		System.out.print("{");
		for (int i = 0; i < numSets; i++){
			for ( int o = 0; o < associativity; o++){
				
				System.out.print(cache[i][o].getAge());
				if(i<numSets-1){
					
					System.out.print(", ");
				}
			}
		}
		System.out.println("}");
	}
	
	private void increment(){
		for (int s = 0; s < numSets; s++){
			for ( int a = 0; a < associativity; a++){
				
				if(cache[s][a].isValidBit()){
					
//					printAges();
					cache[s][a].incrementAge();
//					printAges();
				}
			}
		}
	}
}
