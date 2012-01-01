/*
* Copyright (C) 2012 teho Labs/B. A. Bryce
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in
* the Software without restriction, including without limitation the rights to
* use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
* of the Software, and to permit persons to whom the Software is furnished to do
* so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package teho.logistics;

import java.util.Vector;

public class InventoryItem {
	
	private String name;
	private String partType;
	private Vector <String> partNumbers = new Vector <String>();
	private Vector <String> sources = new Vector <String>();
	private int count;
	private double unitCost;
	
	// Constructor, the name should be a unique identifier
	public InventoryItem(String aname){		
		name = aname;
	}
	
	public int getSourceCount(){
		return sources.size();
	}
	
	// get and set functions for partType field
	public String getPartType(){
		return partType;
	}
	
	public void setPartType(String apartType){
		partType = apartType;
	}
	
	//return the name of this item
	public String getName(){
		return name;
	}
	
	public void setName(String aname){
		name = aname;
	}
	
	public String getSource(int index){
		if(index<sources.size()) return sources.get(index);
		return null;		
	}
	
	public String getPartNumber(int index){
		if(index<partNumbers.size())return partNumbers.get(index);
		return null;
	}
	
	//add unit count and re-price the effective unit cost
	public void addUnits(int n, double cost){
		if(n != 0)
		{
			double currentCost;
			currentCost = (unitCost * count) + cost;
			count = count + n;
			unitCost = currentCost/count;
		}
	}
	
	//returns the total cost of the units withdrawn
	public double removeUnits(int n){
		count = count - n;
		return unitCost*n;
	}
	
	//return the number of units in stock
	public int getCount(){
		return count;
	}
	
	// Look for if the part number is any of the valid ones for this item
	// TRUE = Part match; FALSE = No match
	public Boolean matchPartSource(String apartNumber, String asource){
		for(int i = 0; i < partNumbers.size(); i++){
			if(partNumbers.get(i).equalsIgnoreCase(apartNumber) && sources.get(i).equalsIgnoreCase(asource)) return true;
		}
		return false;
	}
	
	//Just checks if the partnumber matches any for this part
	public Boolean matchPartNumber(String apartNumber){
		for(int i = 0; i < partNumbers.size(); i++){
			if(partNumbers.get(i).equalsIgnoreCase(apartNumber)) return true;
		}
		return false;
	}
	
	public void addPartSource(String apartNumber, String asource){
		partNumbers.add(apartNumber);
		sources.add(asource);
	}
	
	public void removePartSource(String apartNumber, String asource){
		for(int i = 0; i < partNumbers.size(); i++){
			if(partNumbers.get(i).equalsIgnoreCase(apartNumber) && sources.get(i).equalsIgnoreCase(asource)){
				partNumbers.remove(i);
				sources.remove(i);
			}
		}
	}

	public String toString(){
		String accumulator = new String();
		accumulator += "Name:\n" + name + "\n";
		accumulator += "\nPart type:\n";
		accumulator += partType + "\n";
		accumulator += "\nPart numbers | Sources:\n";
		for (int i=0; i<partNumbers.size(); i++){
			accumulator += partNumbers.get(i) + " | " + sources.get(i) + "\n";
		}
		accumulator += "\nCount:\n";
		accumulator += new Integer(count).toString() + "\n";
		
		accumulator += "\nUnit Cost:\n";
		accumulator += new Double(unitCost).toString()+ "\n";
		
		return accumulator;
	}
	
	//maxSources should be the maximal number of sources used for the CSV inventory
	//this assures that the columns are correctly delimited
	public String toCSVline(int maxSources){
		String accumulator = new String();
		accumulator += name + ",";
		accumulator += partType + ",";
		accumulator += new Integer(count).toString() + ",";
		accumulator += new Double(unitCost).toString();
		for (int i=0; i<maxSources; i++){
			if(i<partNumbers.size())accumulator += "," + partNumbers.get(i) + "," + sources.get(i);
			else accumulator += ",,";
		}
		//accumulator += "\n";
		
		return accumulator;
	}

}
