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

public class PartList {
	
	private Vector <String> parts = new Vector <String>();
	private Vector <Integer> count = new Vector <Integer>();
	private int lineItems;
	private String listName;
	
	public PartList(String aname){
		listName = aname;
		lineItems = 0;
	}
	
	public PartList(){
		lineItems = 0;
	}
	
	public void clearDesign(){
		parts.clear();
		count.clear();
		lineItems = 0;
	}
	
	public String getName(){
		return listName;
	}
	
	public void setName(String aname){
		listName = aname;
	}
	
	public void addList(PartList alist, int count){
		for(int i = 0; i<alist.getLineItems(); i++){
			addPart(alist.getPartName(i), alist.getCount(i)*count);
		}
	}
	
	public void aggregateList(PartList alist, int count){
		int partIndex;
		int newCount;
		for(int i = 0; i<alist.getLineItems(); i++){
			partIndex = findPartByName(alist.getPartName(i));
			
			//If the part with the same name exists just change quantity
			if(partIndex != -1)
			{
				newCount = getCount(partIndex);
				newCount += alist.getCount(i)*count;
				setCount(partIndex, newCount);
			}
			else addPart(alist.getPartName(i), alist.getCount(i)*count);
		}
		
	}
	
	private int findPartByName(String aitem){
		for(int i = 0; i<parts.size(); i++){
			if(parts.get(i).equalsIgnoreCase(aitem))return i;
		}
		return -1;
	}
	
	public void addPart(String aitem, int acount){
		
		int index;
		index = findPartByName(aitem);
		if(index != -1){
			count.set(index, count.get(index)+ acount);
		}
		else {
			parts.add(aitem);
			count.add(acount);
			lineItems++;
		}

	}
	
	public int getLineItems(){
		return lineItems;
	}
	
	public String getPartName(int index){
		return parts.get(index);
	}
	
	public Integer getCount(int index){
		return count.get(index);
	}
	
	public void setCount(int index, int acount){
		count.set(index, acount);
	}
	
	public String toString(){
		String theString = new String();
		for(Integer i = 0; i<parts.size(); i++){
			theString += "Index: ";
			theString +=  i.toString();
			theString += " Part: ";
			theString += parts.get(i);
			theString += ", Count: ";
			theString += count.get(i).toString();
			theString += "\n";
		}
		return theString;
	}
	
	public String toCSVline(Integer index){
		return parts.get(index) + "," + count.get(index).toString();
	}
	
	public String toDigikeyLine(Integer index){
		return count.get(index).toString() + "," + parts.get(index);
	}
	
	public String toMouserLine(Integer index){
		return parts.get(index) + "|" + count.get(index).toString();
	}
	
	public String toArrowLine(Integer index){
		return toCSVline(index);
	}

}
