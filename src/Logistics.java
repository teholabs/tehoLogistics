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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Scanner;
import java.util.Vector;

public class Logistics {
	
	private static Vector <InventoryItem> inventoryItems = new Vector <InventoryItem>();
	private static Vector <PartList> designs = new Vector <PartList>();
	
	
	public Logistics(){
		readInventory();
		readDesigns();		
	}
		
	public void addPart(String partNumber, double unitCost, int count, String source){
		boolean partNotAdded = true; 
		
		String consoleInput;
		//look for part in inventory
		for(int i = 0; i<inventoryItems.size(); i++)
		{
			if(inventoryItems.get(i).matchPartNumber(partNumber))
			{
				//found add part
				partNotAdded = false;
				inventoryItems.get(i).addUnits(count, unitCost*count);
			}
		}
		//failed to find part prompt for needed input and add it
		if(partNotAdded)
		{
			System.out.println("Part was not found: " + partNumber);
			
			do
			{
				System.out.println("Add part? (y/n)");
				consoleInput = getConsoleString();
			}while(consoleInput.compareToIgnoreCase("y") != 0 && consoleInput.compareToIgnoreCase("n") != 0);
				
			if(consoleInput.compareToIgnoreCase("y") == 0)
			{	
				while(partNotAdded)
				{	
					
					do
					{
						System.out.println("Add to existing part? (y/n)");
						consoleInput = getConsoleString();
					}while(consoleInput.compareToIgnoreCase("y") != 0 && consoleInput.compareToIgnoreCase("n") != 0);
					
					if(consoleInput.compareToIgnoreCase("n") == 0)
					{
						String itemName, partType;			
						System.out.println("Item Name:");
						itemName = getConsoleString();
						if(itemName != null)
						{
							System.out.println("Part Type:");
							partType = getConsoleString();
							if(partType != null)
							{
								addInventoryItem(itemName, partType, partNumber, source, count, unitCost);
								partNotAdded = false;
							}
						
						}
						
					}
					else if(consoleInput.compareToIgnoreCase("y") == 0)
					{
						//search for part and add it to the right one
						
						int partID;
						partID = findPart();
						if(partID != -1){
							inventoryItems.get(partID).addPartSource(partNumber, source);									
							inventoryItems.get(partID).addUnits(count, unitCost*count);
							partNotAdded = false;
						}
					}										
				}	
			}			
		}		
	}
	
	public int findPartList(){
		String consoleInput;			
		
		String outputString = new String();
		int i = 0;
		do{
			outputString = getPartListNamesAsString(i, 10);
			i += 10;			
			if(!outputString.isEmpty()){
				System.out.println(outputString);
				System.out.println("Enter list ID / enter to continue listing");
				consoleInput = getConsoleString();
				if(!consoleInput.isEmpty())
				{	
					Integer listID;
					listID = Integer.parseInt(consoleInput.trim());
					return listID;
					
				}
			}
		} while(!outputString.isEmpty());
		
		return -1;
	}
	
	
	public int findPart(){
		
		String apartType;
		String consoleInput;
		System.out.println("Part Type?");
		apartType = getConsoleString();				
		
		String outputString = new String();
		int i = 0;
		do{
			outputString = getInventoryAsStringByPartType(apartType, 10, i*10);
			i++;			
			if(!outputString.isEmpty()){
				System.out.println(outputString);
				System.out.println("Enter part ID / enter to continue listing");
				consoleInput = getConsoleString();
				if(!consoleInput.isEmpty())
				{	
					Integer partID;
					partID = Integer.parseInt(consoleInput.trim());
					return partID;
					
				}
			}
		} while(!outputString.isEmpty());
		
		return -1;
	}
	
	public void processInvoices(){		
		// All the new files to process are in the incoming directory
		File rootFolder = new File("incoming");
		File[] files;
		files = rootFolder.listFiles();
		
		for(int i=0; i<files.length; i++){
			importInvoice(files[i].getPath());
			//move for delete file!
		}
	}
	
	public void importInvoice(String path){
		String invoiceType = "";
		if(path.contains("pdf"))
		{
			PDFtoText(path);
			File current = new File(path);
			current.delete();
		}
		else {
			File current = new File(path);
			File oldFile = new File("workingInvoice.txt");
			oldFile.delete();
			File workingFile = new File("workingInvoice.txt");
			current.renameTo(workingFile);
		}
		invoiceType = determineInvoiceType();
		
		if(invoiceType.equalsIgnoreCase("mouser"))parseMouserInvoice();
		else if(invoiceType.equalsIgnoreCase("digikey"))parseDigikeyInvoice();
		else if(invoiceType.equalsIgnoreCase("arrow"))parseArrowInvoice();		
	}
	
	
	public void parseMouserInvoice(){
		System.out.println("Mouser type\n");
		String currentLine; 
		String lineNumber, partNumber, count, unitPrice;
		
		Vector <Double> unitPrices = new Vector <Double>();
		Vector <String> partNumbers = new Vector <String>();
		Vector <Integer> counts = new Vector <Integer>();
		Double totalCost, costFraction;		
		totalCost = 0.0;
		
		FileReader invoiceText;
		try {
			invoiceText = new FileReader("workingInvoice.txt");		
			Scanner lineScanner = new Scanner(invoiceText);
			
			while (lineScanner.hasNextLine()){	
				currentLine = lineScanner.nextLine();
				if(currentLine.startsWith("No.")){
					while(lineScanner.hasNextLine()){
						lineNumber = lineScanner.next();
						if(lineNumber.startsWith("Freight"))break;
						partNumber = lineScanner.next();
						lineScanner.next();// number ordered
						count = lineScanner.next();
						lineScanner.next();// pending count
						unitPrice = lineScanner.next();
						
						System.out.println("Part Number: " + partNumber + " Count: " + count + " Unit Price: " + unitPrice);
						partNumbers.add(partNumber);						
						counts.add(Integer.parseInt(count));
						unitPrices.add(Double.parseDouble(unitPrice));
						
						lineScanner.nextLine();
						lineScanner.nextLine();
						lineScanner.nextLine();
						currentLine = lineScanner.nextLine();
					}
				}
				else if(currentLine.contains("USD $")){
					totalCost = Double.parseDouble(currentLine.substring(currentLine.indexOf("USD $")+5).trim());
					System.out.println(": " + totalCost.toString());					
					
				}
			}
			
			try {
				invoiceText.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Invoice Text file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Invoice Text File Found");
		} 
		
		//adjust cost to include shipping overhead
		if(totalCost != 0.0)
		{
			for(int i = 0; i<partNumbers.size(); i++)
			{
				costFraction = (unitPrices.get(i)*counts.get(i))/totalCost;
				unitPrices.set(i, unitPrices.get(i)*(1+costFraction));	
				//add parts to inventory here
				addPart(partNumbers.get(i), unitPrices.get(i), counts.get(i), "Mouser");
			}
			
		}

		
	}
	// add backorder handling ordered != shipped
	public void parseDigikeyInvoice(){
		System.out.println("Digikey type\n");
		String lines[] = {"", "", "", "", "", ""};
		String partNumber, count, unitPrice;
		Scanner itemScanner;
		
		Vector <Double> unitPrices = new Vector <Double>();
		Vector <String> partNumbers = new Vector <String>();
		Vector <Integer> counts = new Vector <Integer>();
		Double totalCost, costFraction;		
		totalCost = 0.0;
		
		FileReader invoiceText;
		try {
			invoiceText = new FileReader("workingInvoice.txt");		
			Scanner lineScanner = new Scanner(invoiceText);
			
			while (lineScanner.hasNextLine()){	
				lines[0] = lines [1];
				lines[1] = lines [2];
				lines[2] = lines [3];
				lines[3] = lines [4];
				lines[4] = lines [5];
				lines[5] = lineScanner.nextLine();
				
				if (lines[5].startsWith("COUNTRY/ORIGIN")){
					itemScanner = new Scanner(lines[1]);
					itemScanner.next(); //Index
					itemScanner.next(); //Box
					itemScanner.next(); //Ordered
					itemScanner.next(); //Canceled
					count = itemScanner.next();
					partNumber = itemScanner.next();
					unitPrice = itemScanner.next();
					System.out.println("Part Number: " + partNumber + " Count: " + count + " Unit Price: " + unitPrice);
					partNumbers.add(partNumber);						
					counts.add(Integer.parseInt(count));
					unitPrices.add(Double.parseDouble(unitPrice));
				}
				else if(lines[5].contains("TOTAL CHARGED")){
					totalCost = Double.parseDouble(lines[5].substring(lines[5].length()-8).trim());
					System.out.println("Total cost: " + totalCost.toString());		
				}
				
			}
			
			try {
				invoiceText.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Invoice Text file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Invoice Text File Found");
		} 
		
		
		//adjust cost to include shipping overhead
		if(totalCost != 0.0)
		{
			for(int i = 0; i<partNumbers.size(); i++)
			{
				costFraction = (unitPrices.get(i)*counts.get(i))/totalCost;
				unitPrices.set(i, unitPrices.get(i)*(1+costFraction));	
				//add parts to inventory here
				addPart(partNumbers.get(i), unitPrices.get(i), counts.get(i), "Digikey");
			}
			
		}
		
	}
	
	public void parseArrowInvoice(){
		System.out.println("Arrow type\n");
		StringBuilder contents = new StringBuilder();
		String fileText;
		FileReader invoiceText;
		String partNumber, count, unitPrice;
		
		Vector <Double> unitPrices = new Vector <Double>();
		Vector <String> partNumbers = new Vector <String>();
		Vector <Integer> counts = new Vector <Integer>();
		Double totalCost, costFraction;		
		totalCost = 0.0;
		
		int currentIndex = 0;
		int endIndex, stringStart, stringEnd;
		try {
			invoiceText = new FileReader("workingInvoice.txt");		
			Scanner lineScanner = new Scanner(invoiceText);
			
			while (lineScanner.hasNextLine()){
				contents.append(lineScanner.nextLine());
			}
			
			fileText = contents.toString();
			fileText = fileText.replace("&nbsp;", " ");
			currentIndex = fileText.indexOf("#EXPRICE#");
			endIndex = fileText.indexOf("</table>", currentIndex);
			while(currentIndex < endIndex){
				currentIndex = fileText.indexOf("<tr", currentIndex);
				if(currentIndex > endIndex)break;
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				// line number
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				stringStart = currentIndex + 1;
				stringEnd = fileText.indexOf("</p", currentIndex);
				
				//System.out.println("Count: " + fileText.substring(stringStart, stringEnd));
				count = fileText.substring(stringStart, stringEnd).trim();
				// count
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				stringStart = currentIndex + 1;
				stringEnd = fileText.indexOf("</p", currentIndex);
				String description = fileText.substring(stringStart, stringEnd);
				//System.out.println("Des: " + description);
				//System.out.println("Part: " + description.substring(0, description.indexOf(" ")));
				// description
				partNumber = description.substring(0, description.indexOf(" ")).trim();
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				//supplier
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				//status
				currentIndex = fileText.indexOf("<p", currentIndex);
				currentIndex = fileText.indexOf(">", currentIndex);
				stringStart = currentIndex + 1;
				stringEnd = fileText.indexOf("</p", currentIndex);
				//System.out.println("Price: " + fileText.substring(stringStart, stringEnd).trim());
				unitPrice = fileText.substring(stringStart, stringEnd).trim();
				//price
				
				System.out.println("Part Number: " + partNumber + " Count: " + count + " Unit Price: " + unitPrice);
				partNumbers.add(partNumber);						
				counts.add(Integer.parseInt(count));
				unitPrices.add(Double.parseDouble(unitPrice));
			}
			currentIndex = fileText.indexOf("Total", currentIndex);
			currentIndex = fileText.indexOf("<td", currentIndex);
			currentIndex = fileText.indexOf(">", currentIndex);
			stringStart = currentIndex + 1;
			stringEnd = fileText.indexOf("</td", currentIndex);
			totalCost = Double.parseDouble(fileText.substring(stringStart, stringEnd).trim());
			System.out.println("Total cost: " + totalCost.toString());	
			//total price
	
			try {
				invoiceText.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Invoice Text file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Invoice Text File Found");
		} 
		
		//adjust cost to include shipping overhead
		if(totalCost != 0.0)
		{
			for(int i = 0; i<partNumbers.size(); i++)
			{
				costFraction = (unitPrices.get(i)*counts.get(i))/totalCost;
				unitPrices.set(i, unitPrices.get(i)*(1+costFraction));
				//add parts to inventory here
				addPart(partNumbers.get(i), unitPrices.get(i), counts.get(i), "Arrow");
			}
			
		}
		
	}
	
	// This looks though the text file for company name so the parser needed can be known
	public String determineInvoiceType(){
		String invoiceType = "";
		String currentLine; 
		
		FileReader invoiceText;
		try {
			invoiceText = new FileReader("workingInvoice.txt");		
			Scanner lineScanner = new Scanner(invoiceText);
			
			while (lineScanner.hasNextLine()){	
				currentLine = lineScanner.nextLine();
				if(currentLine.toLowerCase().contains("mouser")){
					invoiceType = "mouser";
					break;
				}
				else if(currentLine.toLowerCase().contains("digikey")){
					invoiceType = "digikey";
					break;
				}
				else if(currentLine.toLowerCase().contains("digi-key")){
					invoiceType = "digikey";
					break;
				}
				else if(currentLine.toLowerCase().contains("arrow")){
					invoiceType = "arrow";
					break;
				}
			}
			
			try {
				invoiceText.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Invoice Text file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Invoice Text File Found");
		} 
		
		return invoiceType;
	}
	
	
	public void PDFtoText(String filePath){
		String commandString = "java -jar pdfbox-app.jar ExtractText ";
		try {
			Process pdfbox = Runtime.getRuntime().exec(commandString + filePath + " workingInvoice.txt");
			try {
				pdfbox.waitFor(); // Wait until the text file is done writing!
			} catch (InterruptedException e) {
				System.out.println("PDFBox failure, did not terminate properly");
			} 
		} catch (IOException e) {
			System.out.println("Could not parse PDF file\n");
		}
	}
	
	private void renameFile(String oldFileName, String newFileName){
		File oldFile = new File(oldFileName);
		File newFile = new File(newFileName);
		oldFile.renameTo(newFile);
	}
	
	public int getMaxSources(){
		int maxSize = 0;
		for(int i=0; i< inventoryItems.size(); i++){
			if(inventoryItems.get(i).getSourceCount() > maxSize)
			{
				maxSize = inventoryItems.get(i).getSourceCount();
			}
		}
		
		return maxSize;
	}
	
	public void writeInventory(int maxSources){
		Vector <String> lines = new Vector <String>();
		
		String header = "Name,Type,Count,Unit Price";
		
		for(Integer k = 1; k<=maxSources; k++){
			header += ",Part Number ";
			header += k.toString();
			header += ",Source ";
			header += k.toString();
			
		}		
		lines.add(header);		
		for(int i = 0; i<inventoryItems.size(); i++){
			lines.add(inventoryItems.get(i).toCSVline(maxSources));
		}		
		writeCSV("inventory", lines);
		
	}
	
	//fileName does not include the extension, assumes file exists
	private void writeCSV(String fileName, Vector <String> lines){
		FileWriter inventoryFile;	
		
		renameFile(fileName + ".csv", fileName + ".bak");		
		try {		
			inventoryFile = new FileWriter(fileName + ".csv");			
			for(int i = 0; i<lines.size(); i++){
				inventoryFile.write(lines.get(i));
				inventoryFile.write("\r\n");
			}
			try {
				inventoryFile.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Inventory file");
			}
			
		} catch (IOException e) {
			System.out.println("Inventory File write error");
		} 
		
	}
	
	
	private void readInventory(){
		
		FileReader inventoryFile;
		try {
			inventoryFile = new FileReader("inventory.csv");		
			Scanner lineScanner = new Scanner(inventoryFile);
			//The first line is header info toss it
			if(lineScanner.hasNextLine())lineScanner.nextLine();
			while (lineScanner.hasNextLine()){				
				CSVlineToInventory(lineScanner.nextLine());			
			}
			try {
				inventoryFile.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close Inventory file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Inventory File Found");
		} 
		
	}
	
	private void CSVlineToInventory(String aline){
		Scanner inv = new Scanner(aline);
		inv.useDelimiter(",");		
		String itemName, partNumber, source, partType;
		int count;
		double unitCost;
		if(inv.hasNext()){
			itemName = inv.next();
			partType = inv.next();
			count = inv.nextInt();			
			unitCost = (inv.nextDouble())*count;
			partNumber = inv.next();
			source = inv.next();
			addInventoryItem(itemName, partType, partNumber, source, count, unitCost);
			
			while(inv.hasNext()){
				partNumber = inv.next();
				if(inv.hasNext()){
					source = inv.next();
					if(partNumber.length()> 0 && source.length()> 0){				
						addInventoryPartSource(itemName, partNumber, source);		
					}		
				}
			}
		}

	}
	
	
	public PartList readSingleListFile(String fileName, int skipCount){
		
		PartList alist = new PartList();
		int skipped = 0;
		
		FileReader designFile;
		try {
			designFile = new FileReader(fileName);		
			Scanner lineScanner = new Scanner(designFile);
			Vector <String> rawList = new Vector <String>();
			String temp;			
			
			while (lineScanner.hasNextLine()){
				Scanner inv = new Scanner(lineScanner.nextLine());
				inv.useDelimiter(",");				
				if(inv.hasNext()){
					temp = inv.next();
					if (temp.equalsIgnoreCase("endList")){						
						if(skipped == skipCount)
						{
							alist = createPartList(rawList);
							break;
						}
						rawList.clear();
						skipped++;
					}
					else rawList.add(temp);
					if(inv.hasNext()){
						temp = inv.next();
						if(temp.length()>0) rawList.add(temp);							
					}
				}
			}
			try {
				designFile.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close lists file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No List File Found");
		} 
		
		return alist;
	}

	
	private void readDesigns(){
		
		FileReader designFile;
		try {
			designFile = new FileReader("lists.csv");		
			Scanner lineScanner = new Scanner(designFile);
			Vector <String> rawList = new Vector <String>();
			String temp;			
			
			while (lineScanner.hasNextLine()){
				Scanner inv = new Scanner(lineScanner.nextLine());
				inv.useDelimiter(",");				
				if(inv.hasNext()){
					temp = inv.next();
					if (temp.equalsIgnoreCase("endList")){
						designs.add(createPartList(rawList));
						rawList.clear();
					}
					else rawList.add(temp);
					if(inv.hasNext()){
						temp = inv.next();
						if(temp.length()>0) rawList.add(temp);							
					}
				}
			}
			try {
				designFile.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close lists file");
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("No Design File Found");
		} 
		
	}
	
	
	private PartList createPartList(Vector <String> alist){
		
		PartList thePartList = new PartList(alist.get(0));
		
		for(int i=1; i<alist.size(); i+=2){
			thePartList.addPart(alist.get(i), new Integer(alist.get(i+1)));
		}
				
		return thePartList;
		
		
	}
	
	//returns the number of the item currently in stock
	public int getInventoryItemCount(String item){
		for(int i = 0; i<inventoryItems.size(); i++){
			if(inventoryItems.get(i).getName().equalsIgnoreCase(item)){
				return inventoryItems.get(i).getCount();
			}
		}
		return 0; // we should never be here! 
	}
	
	//Start Inventory management functions
	
	public InventoryItem getInventoryItem(int index){
		return inventoryItems.get(index);
	}
	
	public void deleteInventoryItem(int index){
		inventoryItems.remove(index);
	}
	//This function returns the index in the vector of the item with name <itemName>
	//Failure results in an index of -1
	public int getInventoryItemIndexByName(String itemName){
		for(int i=0; i<inventoryItems.size(); i++){
			if(inventoryItems.get(i).getName().equalsIgnoreCase(itemName))return i;
		}
		return -1; //Not in records
	}


	//This function adds an InventoryItem with <itemName>
	public void addInventoryItem(String itemName){
		InventoryItem anitem = new InventoryItem(itemName);
		inventoryItems.add(anitem);
	}
	
	//This function adds an InventoryItem to the Inventory with the basic needed fields
	public void addInventoryItem(String itemName, String partType, String partNumber, String source, int count, double unitCost){
		InventoryItem anitem = new InventoryItem(itemName);
		anitem.addPartSource(partNumber, source);
		anitem.addUnits(count, unitCost);
		anitem.setPartType(partType);
		inventoryItems.add(anitem);
	}
	
	//This function adds <countToAdd> with total cost <cost> to item at <index>
	public void addInventory(int index, int countToAdd, double cost){
		inventoryItems.get(index).addUnits(countToAdd, cost);
	}
	
	//This function adds <countToAdd> with total cost <cost> to item <itemName> 
	//If <itemName> is not found an error is printed
	public void addInventory(String itemName, int countToAdd, double cost){
		int index;
		index = getInventoryItemIndexByName(itemName);
		if(index != -1){
			inventoryItems.get(index).addUnits(countToAdd, cost);
		}
		else System.out.println("No item with that name in inventory");
	}
	
	//This function removes <n> of the item with name <itemName>
	//If <itemName> is not found 0 is returned and an error is printed
	//It returns the total cost of the items removed
	public double removeInventory(String itemName, int n){
		int index;
		index = getInventoryItemIndexByName(itemName);
		if(index != -1){
			return inventoryItems.get(index).removeUnits(n);
		}
		else System.out.println("No item with that name in inventory");
		
		return 0;
	}
	
	//This function removes <n> of the item at <index> in the vector
	//It returns the total cost of the items removed
	public double removeInventory(int index, int n){
		return inventoryItems.get(index).removeUnits(n);
	}
	
	
	public void addInventoryPartSource(int index, String apartNumber, String asource){
		inventoryItems.get(index).addPartSource(apartNumber, asource);
	}
	
	public void addInventoryPartSource(String aname, String apartNumber, String asource){
		int index;
		index = getInventoryItemIndexByName(aname);
		if(index != -1){
			inventoryItems.get(index).addPartSource(apartNumber, asource);
		}
		else System.out.println("no item with that name in inventory");
	}
	
	
	// Stub function to return a string line for for an inventory item based on index
	// Basic stocking info only
	public String getInventoryItemStockString(Integer index){
		Integer acount = new Integer(inventoryItems.get(index).getCount());
		return "ID: " + index.toString() + ", Name: " + inventoryItems.get(index).getName() + ", Count: " + acount.toString() + "\n";
	}
	
	public int getInventoryLength(){
		return inventoryItems.size();
	}
	
	//returns all of the inventory as a big long string
	public String getInventoryAsString(){
		String inventoryAsString = new String();
		for(int i = 0; i<inventoryItems.size(); i++){
			inventoryAsString += getInventoryItemStockString(i);
		}
		return inventoryAsString;
	}
	
	//returns up to count items from startIndex
	public String getInventoryAsString(int startIndex, int count){
		String inventoryAsString = new String();
		int k = 0;
		int i = startIndex;
		while(i<inventoryItems.size() && k<count){
			inventoryAsString += getInventoryItemStockString(i);
			k++;
			i++;
		}
		return inventoryAsString;
	}
	//returns all of the inventory items of type <atype>
	public String getInventoryAsStringByPartType(String atype){
		String inventoryAsString = new String();
		for(Integer i = 0; i<inventoryItems.size(); i++){
			if(inventoryItems.get(i).getPartType().equalsIgnoreCase(atype))inventoryAsString += "Item Number: " + i.toString() + " " + getInventoryItemStockString(i);
		}
		return inventoryAsString;
	}
	
	//returns up to <count> of the inventory items of type <atype>
	//it will skip up to ignoreCount items
	public String getInventoryAsStringByPartType(String atype, int count, int ignoreCount){
		String inventoryAsString = new String();
		int k = 0;
		int j = 0;
		for(int i = 0; i<inventoryItems.size(); i++){
			if(inventoryItems.get(i).getPartType().equalsIgnoreCase(atype)){
				k++;
				if(k>ignoreCount && j<count){
					inventoryAsString += getInventoryItemStockString(i);
					j++;
				}
			}
		}
		return inventoryAsString;
	}
	
	
	// This function removes the needed inventory to build the boards
	// it returns to unit cost of the boards in question
	// if more than extra parts were used inventory correction should be added
	// This function does not check if the board count is buildable!
	public double buildBoards(int index, int numberOfBoards){
		double cost = 0;
		for(int k = 0; k<designs.get(index).getLineItems(); k++)
		{
			double temp;
			temp = removeInventory(designs.get(index).getPartName(k), designs.get(index).getCount(k)*numberOfBoards);
			if(temp == 0)System.out.println("An item was not found inventory error!");
			cost += temp;
		}
		return cost;
	}
	
	/*	
	public double buildBoards(String boardName, int numberOfBoards){
		double cost = 0;
		for(int i = 0; i<designs.size(); i++){
			if(designs.get(i).getName().equalsIgnoreCase(boardName)){
				for(int k = 0; k<designs.get(i).getLineItems(); k++){
					double temp;
					temp = removeInventory(designs.get(i).getPartName(k), designs.get(i).getCount(k)*numberOfBoards);
					if(temp == 0)System.out.println("An item was not found inventory error!");
					cost += temp;
				}
				return cost;
			}
		}
		return cost; // Should be greater than 0! 
	}
	*/
	
	
	public double removeAndCostPartList(PartList alist){
		double cost = 0;
		for(int k = 0; k<alist.getLineItems(); k++)
		{
			double temp;
			temp = removeInventory(alist.getPartName(k), alist.getCount(k));
			if(temp == 0)System.out.println("An item was not found inventory error!");
			cost += temp;
		}
		return cost;
	}

	
	//This function returns the number of the <boardName>
	//that can be constructed with the current inventory
	public int buildableBoardCount(int index){
		int numberBuildable;
		numberBuildable = getInventoryItemCount(designs.get(index).getPartName(1))/designs.get(index).getCount(1);
		int temp;
		for(int k = 0; k<designs.get(index).getLineItems(); k++){
			 temp = getInventoryItemCount(designs.get(index).getPartName(k))/designs.get(index).getCount(k);
			 if (temp<numberBuildable)numberBuildable = temp;
			 
		}

		return numberBuildable;

	}
	
	//This function returns a PartList of the needed parts to complete the desired buildList
	//If there are no needed parts the PartsList will be empty
	public PartList buildableList(PartList aList){
		PartList order = new PartList();
		int partCount;
		for(int k = 0; k<aList.getLineItems(); k++){
			//System.out.println("Number on hand after build - " + aList.getPartName(k));
			partCount = getInventoryItemCount(aList.getPartName(k))-aList.getCount(k);
			//System.out.println(partCount);
			if(partCount < 0)order.addPart(aList.getPartName(k), partCount*-1);
		}
		return order;
	}
	


	public String getPartListNamesAsString(int startIndex, int count) {
		String partListNames = new String();
		int k = 0;
		Integer i = startIndex;
		while(i<designs.size() && k<count){
			partListNames += "Index: " + i.toString() + " " + designs.get(i).getName() + '\n';
			k++;
			i++;
		}
		return partListNames;
	}
	
	public int getPartListIndex(String partListName){
		for(int i=0; i<designs.size(); i++){
			if(designs.get(i).getName().equalsIgnoreCase(partListName))return i;
		}
		return -1;
	}
	
	public PartList getPartList(int index){
		return designs.get(index);
	}
	
	public void removePartList(int index){
		designs.remove(index);
	}
	
	
	
	
	
	
	/*
	 * Start Supplier web parsing functions
	 * 
	 */
	
	//Goes through apartList and gets the best source for all parts
	public Vector <PartList> sourcePartsList(PartList apartList){		
		Vector <Double> listCosts = new Vector <Double>();
		Vector <PartList> lists = new Vector <PartList>();
		Vector <String> returnedValues;
		Integer count;
		Double cost, temp;
		String partNumber, source;
		
		boolean partAdded;
		
		for(int i = 0; i<apartList.getLineItems(); i++){
			returnedValues = getPartPrice(apartList.getPartName(i), apartList.getCount(i));
			source = returnedValues.get(0);
			partNumber = returnedValues.get(1);
			temp = Double.parseDouble(returnedValues.get(2));
			count = temp.intValue();
			cost = Double.parseDouble(returnedValues.get(3))*count;
			partAdded = false;
			if(i == 0){
				lists.add(new PartList(source));
				lists.get(0).addPart(partNumber, apartList.getCount(i));
				listCosts.add(cost);
				partAdded = true;
			}
			else{
				int k = 0;
				
				while(k<lists.size()){
					if(lists.get(k).getName().equalsIgnoreCase(source)){
						lists.get(k).addPart(partNumber, apartList.getCount(i));
						cost += listCosts.get(k);						
						listCosts.set(k, cost);
						partAdded = true;
					}
					k++;
				}
				if(k == lists.size() && partAdded == false){
					lists.add(new PartList(source));
					lists.get(k).addPart(partNumber, apartList.getCount(i));
					listCosts.add(cost);					
				}
			}
		}
		
		NumberFormat currencyFormatter;
		String totalCost;
		currencyFormatter = NumberFormat.getCurrencyInstance();
		
		System.out.println(((Integer) lists.size()).toString());
		for(int h=0; h<lists.size(); h++){
			System.out.println(lists.get(h).getName() + " List\n");
			System.out.println(lists.get(h).toString());
			totalCost = currencyFormatter.format(listCosts.get(h));
			System.out.println("\nTotal Cost: " + totalCost + "\n");
		}
		
		return lists;
	}
	
	
	private double[] getCostForUnits(Vector <String> costs, int units){
		Double totalCost;
		Integer unitsToBuy = units;
		
		int[] priceBreaks = new int[costs.size()/2];
		double[] unitCosts = new double[costs.size()/2];
		
		int j = 0;
		int i = 0;
		while(i<costs.size()){
			priceBreaks[j] = Integer.parseInt(costs.get(i).replaceAll(",", ""));
			i++;
			unitCosts[j] = Double.parseDouble(costs.get(i));			
			j++;
			i++;
			
		}
		
		//Find largest number under request
		i = 0;
		while(i<(costs.size()/2) && priceBreaks[i]<units)i++;
		if(i>0)i--;
		
		totalCost = unitCosts[i]*units;
		
		if (costs.size() == 2){
			unitsToBuy = priceBreaks[0];
			totalCost = priceBreaks[0]*unitCosts[0];
		}
		else {
			if((i+1)<(costs.size()/2) && priceBreaks[i+1]*unitCosts[i+1] < totalCost){
				totalCost = priceBreaks[i+1]*unitCosts[i+1];
				unitsToBuy = priceBreaks[i+1];
			}
		}
		
		return new double[] {(double) unitsToBuy, totalCost};		
	}
	
	//Function to search for current price at all sources
	public double[] checkPartPrice(String source, String partNumber, int units){
		
		String searchString = new String();
		Vector <String> costs;
		double[] unitsCost;
		unitsCost = new double[] {-1, -1};
		
		if(source.equalsIgnoreCase("Mouser")){
			System.out.println("Searching Mouser for " + partNumber + "...\n");
			searchString = "http://www.mouser.com/_/N-scv7?Keyword=" + partNumber;
			costs = ParseMouserItem(getHTMLcode(searchString));
			unitsCost = getCostForUnits(costs, units);	
			
		}
		else if(source.equalsIgnoreCase("Digikey") || source.equalsIgnoreCase("Digi-key")){
			System.out.println("Searching Digikey for " + partNumber + "...\n");			
			String manufacturePartNumber;
			
			try {
				manufacturePartNumber = URLEncoder.encode(octoPartResolve(partNumber), "ISO-8859-1");
				if(!manufacturePartNumber.isEmpty())
				{
					searchString = "http://search.digikey.com/us/en/products/" + manufacturePartNumber + "/" + partNumber;
					costs = ParseDigikeyItem(getHTMLcode(searchString), partNumber);
					unitsCost = getCostForUnits(costs, units);
				}
				else 
				{
					System.out.println("Failed to resolve Digikey part number");
				}
				
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}			
					
		}
		else if(source.equalsIgnoreCase("Arrow")){
			System.out.println("Searching Arrow for " + partNumber + "...\n");
			searchString = "http://components.arrow.com/part/search/" + partNumber;
			searchString = "http://components.arrow.com/part/detail/" + resolveArrowItem(getHTMLcode(searchString), partNumber, units);
			costs = ParseArrowItem(getHTMLcode(searchString));
			unitsCost = getCostForUnits(costs, units);
		}
		// Should not happen
		else {
			unitsCost = new double[] {-1, -1};
			return unitsCost;
		}
		/*
		Double A, B;
		A = unitsCost[0];
		B = unitsCost[1];
		System.out.println("Units: " + A.toString() + " Cost: " + B.toString() + "\n\n");
		 */	
		
		
		return unitsCost;
		
	}
	
	
	// This gets the price for an inventory item at all sources
	public Vector <String> getPartPrice(String itemName, int units){
		InventoryItem anitem = getInventoryItem(getInventoryItemIndexByName(itemName));
		String currentPartNumber = anitem.getPartNumber(0);
		String currentSource = anitem.getSource(0);

		//returns best source, part number, count, cost
		Vector <String> output = new Vector <String>();
		double[] unitsCost;
		Double bestCost = 0.0;
		Double bestCount = 0.0;
		String bestSource = "";
		String bestPartNumber = "";
		int i = 1; 
	
		
		while((currentPartNumber != null) && (currentSource != null)){
			unitsCost = checkPartPrice(currentSource, currentPartNumber, units);
			if(i == 1){
				bestCost = unitsCost[1]/unitsCost[0];
				bestCount = unitsCost[0];
				bestSource = currentSource;
				bestPartNumber = currentPartNumber;
			}
			else if(unitsCost[1]<bestCost){
				bestCost = unitsCost[1]/unitsCost[0];
				bestCount = unitsCost[0];
				bestSource = currentSource;
				bestPartNumber = currentPartNumber;
			}
			
			
			currentPartNumber = anitem.getPartNumber(i);
			currentSource = anitem.getSource(i);
			i++;			
		}
			
		output.add(bestSource);
		output.add(bestPartNumber);
		output.add(bestCount.toString());
		output.add(bestCost.toString());
		
		System.out.println("Best Source: " + bestSource + "\nPart Number: " + bestPartNumber + "\nCount: " + bestCount.toString() + "\nCost: " + bestCost.toString() + "\n");
		return output;
	}
	
	
	// This call restricts the list of allowed sources
	// returns 0 count, 0 cost, and blank strings on failure
	public Vector <String> getPartPrice(String itemName, int units, Vector <String> sources){
		InventoryItem anitem = getInventoryItem(getInventoryItemIndexByName(itemName));
		String currentPartNumber = anitem.getPartNumber(0);
		String currentSource = anitem.getSource(0);

		//returns best source, part number, count, cost
		Vector <String> output = new Vector <String>();
		double[] unitsCost;
		Double bestCost = 0.0;
		Double bestCount = 0.0;
		String bestSource = "";
		String bestPartNumber = "";
		int i = 1; 
		int k = 0;
		int h = 0;
		
		while((currentPartNumber != null) && (currentSource != null)){
			k = 0;
			while(k<sources.size()){
				if (currentSource.equalsIgnoreCase(sources.get(k))){
					unitsCost = checkPartPrice(currentSource, currentPartNumber, units);
					if(h == 0){
						bestCost = unitsCost[1];
						bestCount = unitsCost[0];
						bestSource = currentSource;
						bestPartNumber = currentPartNumber;
						h++;
					}
					else if(unitsCost[1]<bestCost){
						bestCost = unitsCost[1];
						bestCount = unitsCost[0];
						bestSource = currentSource;
						bestPartNumber = currentPartNumber;
					}
				}
				k++;
			}
		
			currentPartNumber = anitem.getPartNumber(i);
			currentSource = anitem.getSource(i);
			i++;			
		}
			
		output.add(bestSource);
		output.add(bestPartNumber);
		output.add(bestCount.toString());
		output.add(bestCost.toString());
		
		System.out.println("Best Source: " + bestSource + "\nPart Number: " + bestPartNumber + "\nCount: " + bestCount.toString() + "\nCost: " + bestCost.toString() + "\n");
		return output;
	}
	
	//returns vector of count/price for an item from Mouser
	private Vector <String> ParseMouserItem(String code){
		Vector <String> output = new Vector <String>();
		
		int startIndex, currentIndex, endIndex,  sectionStartIndex, sectionEndIndex, subTagCount;
		String sectionStartSearchString, sectionEndSearchString, quantitySearchString, priceSearchString;
		String price, quantity;
		sectionStartSearchString = "table class=\"PriceBreaks\"";
		quantitySearchString = "PriceBreakQuantity";
		priceSearchString = "PriceBreakPrice";
		sectionEndSearchString = "<!--Priceing-->";

		
		sectionStartIndex = code.indexOf(sectionStartSearchString);
		endIndex = sectionStartIndex;
		currentIndex = sectionStartIndex;
		
		//Find the end of where we should search to, fall back is end of document
		sectionEndIndex = code.indexOf(sectionEndSearchString, currentIndex);		
		if(sectionEndIndex == -1){
			sectionEndSearchString = "SearchResultsBuyColumn";
			sectionEndIndex = code.indexOf(sectionEndSearchString, currentIndex);
			if(sectionEndIndex == -1){
				sectionEndIndex = code.length();
			}
		}
		
		while(currentIndex < sectionEndIndex){
			startIndex = endIndex;
			currentIndex = code.indexOf(quantitySearchString, startIndex);
			if(currentIndex == -1)break;
			subTagCount = 2; // always at least 1
			for(int i=0; i<subTagCount; i++){
				currentIndex = code.indexOf(">", currentIndex+1);
			}
			startIndex = currentIndex;
			endIndex = code.indexOf("<", startIndex);
			
			quantity = code.substring(startIndex+1, endIndex);
			
			//System.out.println("quantity: " + quantity);
			output.add(quantity);
			
			startIndex = endIndex;
			currentIndex = code.indexOf(priceSearchString, startIndex);
			if(currentIndex == -1)break;			
			currentIndex = code.indexOf("$", currentIndex+1);
			startIndex = currentIndex;
			endIndex = code.indexOf("<", startIndex);
			
			price = code.substring(startIndex+1, endIndex);
			
			//System.out.println("price: " + price);
			output.add(price);
		}

		return output;
		
	}

	
	//finds the correct arrow item in the search, takes HTML <code> and the <partName> and count <units>
	private String resolveArrowItem(String code, String partName, int units){
		
		int startIndex, currentIndex, endIndex,  sectionStartIndex, sectionEndIndex;
		String sectionStartSearchString, sectionEndSearchString;
		Vector <String> partReference = new Vector <String>();
		Vector <String> partNumber = new Vector <String>();
		Vector <String> quantity = new Vector <String>();
		
		sectionStartSearchString = "<!-- BEGIN CONTENT -->";
		sectionEndSearchString = "<!-- END CONTENT -->";

		//System.out.println(code);
		sectionStartIndex = code.indexOf(sectionStartSearchString);
		endIndex = sectionStartIndex;
		currentIndex = sectionStartIndex;
		
		//Find the end of where we should search to, fall back is end of document
		sectionEndIndex = code.indexOf(sectionEndSearchString, currentIndex);		
		if(sectionEndIndex == -1){
			sectionEndIndex = code.length();
		}
		
		//skip to the first part
		currentIndex = code.indexOf("<tr class=\"row1\">", currentIndex);
		
		String currentSearchString = " ";
		
		while(currentIndex < sectionEndIndex){
			currentSearchString = "<td class=\"col_mfr_part_num\">";
			//Check if we have no more lines:
			if(code.indexOf(currentSearchString, currentIndex) == -1)break;
			currentIndex = code.indexOf(currentSearchString, currentIndex) + currentSearchString.length();
			
			currentSearchString = "<a href=\"/part/detail/";			
			startIndex = code.indexOf(currentSearchString, currentIndex) + currentSearchString.length();
			endIndex = code.indexOf("\">", startIndex);
			//System.out.println(code.substring(startIndex, endIndex));
			partReference.add(code.substring(startIndex, endIndex).trim());
			currentIndex = endIndex;

			startIndex = code.indexOf(">", currentIndex)+1;
			endIndex = code.indexOf("</a>", startIndex);
			//System.out.println(code.substring(startIndex, endIndex));
			partNumber.add(code.substring(startIndex, endIndex).trim());
			currentIndex = endIndex;
				
			currentSearchString = "<td align=\"left\" class=\"\">";
			startIndex = code.indexOf(currentSearchString, currentIndex) + currentSearchString.length();
			endIndex = code.indexOf("</td>", startIndex);
			//System.out.println(code.substring(startIndex, endIndex));
			quantity.add(code.substring(startIndex, endIndex).trim());
			currentIndex = endIndex;
			
		}
		
		//Find best match for units requested
		int bestIndex = 0;
		int currentBestQuantity = 0;
		int cQuantity;
		for(int k = 0; k < partReference.size(); k++){
			if(!quantity.get(k).isEmpty())
			{
				cQuantity = Integer.parseInt(quantity.get(k));
				if(partNumber.get(k).equals(partName) && cQuantity > currentBestQuantity && units >= cQuantity){
					bestIndex = k;
					currentBestQuantity = cQuantity;
				}
			}
			
		}
		//System.out.println("\n\n" + partReference.get(bestIndex).trim());
		return partReference.get(bestIndex).trim();
	}
	
	
	//returns vector of count/price for an item from Arrow
	private Vector <String> ParseArrowItem(String code){
		Vector <String> output = new Vector <String>();
		
		int startIndex, currentIndex, endIndex,  sectionStartIndex, sectionEndIndex, subTagCount;
		String sectionStartSearchString, sectionEndSearchString, quantitySearchString, priceSearchString;
		String price, quantity;
		sectionStartSearchString = "order_box_head";
		quantitySearchString = "span id=\"multiples\"";
		priceSearchString = "class=\"li_price\"";
		sectionEndSearchString = "<!-- END CONTENT -->";

		
		sectionStartIndex = code.indexOf(sectionStartSearchString);
		endIndex = sectionStartIndex;
		currentIndex = sectionStartIndex;
		
		//Find the end of where we should search to, fall back is end of document
		sectionEndIndex = code.indexOf(sectionEndSearchString, currentIndex);		
		if(sectionEndIndex == -1){
				sectionEndIndex = code.length();
		}
		
		while(currentIndex < sectionEndIndex){
			// This occurs when the method basically fails to parse the page
			if(currentIndex == -1){
				//This is a quick and dirty way to handle it
				//make 1 unit cost way too much
				output.clear();
				output.add("1");
				output.add("9999");			
				break;
			}
			startIndex = endIndex;
			currentIndex = code.indexOf(quantitySearchString, startIndex);
			//normal end of items to read break
			if(currentIndex == -1)break;
			subTagCount = 3; // always at least 1
			for(int i=0; i<subTagCount; i++){
				currentIndex = code.indexOf(">", currentIndex+1);
			}
			startIndex = currentIndex;
			endIndex = code.indexOf("<", startIndex);
			
			quantity = code.substring(startIndex+1, endIndex);
			
			//System.out.println("quantity: " + quantity);
			
			output.add(quantity);
			
			startIndex = endIndex;
			currentIndex = code.indexOf(priceSearchString, startIndex);
			if(currentIndex == -1)break;			
			currentIndex = code.indexOf("$", currentIndex+1);
			startIndex = currentIndex;
			endIndex = code.indexOf("<", startIndex);
			
			price = code.substring(startIndex+1, endIndex);
			
			//System.out.println("price: " + price);
			output.add(price);
		}

		return output;
		
	}
	
	//returns vector of count/price for an item from Digikey
	private Vector <String> ParseDigikeyItem(String code, String partNumber){
		Vector <String> output = new Vector <String>();
		
		String itemPageCode = new String();
		int startIndex, currentIndex, endIndex,  sectionStartIndex, sectionEndIndex;
		String sectionStartSearchString, sectionEndSearchString;
		String price, quantity;
						
		itemPageCode = code;
		sectionStartSearchString = "Price Break";
		sectionEndSearchString = "Quantity Available";
		sectionStartIndex = itemPageCode.indexOf(sectionStartSearchString);
		sectionEndIndex = itemPageCode.indexOf(sectionEndSearchString, sectionStartIndex);
		currentIndex = 	sectionStartIndex;	
		currentIndex = itemPageCode.indexOf("tr", currentIndex);
		currentIndex = itemPageCode.indexOf("td", currentIndex);
		
		while(currentIndex < sectionEndIndex){
			startIndex = itemPageCode.indexOf(">", currentIndex);
			endIndex = itemPageCode.indexOf("<", startIndex);
			quantity = itemPageCode.substring(startIndex+1, endIndex);
			System.out.println("quantity: " + quantity);
			output.add(quantity);
			
			currentIndex = itemPageCode.indexOf("td", currentIndex+1);
			currentIndex = itemPageCode.indexOf("td", currentIndex+1);
			startIndex = itemPageCode.indexOf(">", currentIndex);
			endIndex = itemPageCode.indexOf("<", startIndex);
			
			price = itemPageCode.substring(startIndex+1, endIndex);
			System.out.println("price: " + price);
			output.add(price);
			
			currentIndex = itemPageCode.indexOf("tr", currentIndex);
			currentIndex++;
			currentIndex = itemPageCode.indexOf("tr", currentIndex);
			currentIndex = itemPageCode.indexOf("td", currentIndex);
			
		}
			
		
		
		return output;
			
	}
	
	
	public String octoPartResolve(String keyword){
		
		int currentIndex, startIndex, endIndex;
		String code = getHTMLcode("http://octopart.com/api/v2/parts/search?q="+ keyword);
		String searchString = "hits\":";
		
		currentIndex = code.indexOf(searchString);
		currentIndex +=searchString.length();
		startIndex = currentIndex;
		endIndex = code.indexOf(",", currentIndex);
		if(Integer.parseInt(code.substring(startIndex, endIndex).trim()) == 1)
		{
			searchString = "mpn\": \"";
			currentIndex = code.indexOf(searchString, currentIndex);
			currentIndex +=searchString.length();
			startIndex = currentIndex;
			endIndex = code.indexOf("\"", currentIndex);
			return code.substring(startIndex, endIndex);
		}
		return new String();
		
	}
	
	//gets HTML from theURL and returns it as a string
	private String getHTMLcode(String theURL){

        String htmlCode = new String();
		try {
	        URL partSearch = new URL(theURL);
	        URLConnection partPage = partSearch.openConnection();
	        BufferedReader in = new BufferedReader(new InputStreamReader(partPage.getInputStream()));
	        String aline;        
	
	        while ((aline = in.readLine()) != null){
	        	htmlCode += aline;
	        	//System.out.println(aline);
	        }
	        in.close();
		}
		catch(IOException e){
			System.out.println("Part search errored");
		}
		
		return htmlCode;
	}
	
	
	//****** Console get functions
	
	
	private static String getConsoleString(){
		InputStreamReader istream = new InputStreamReader(System.in);
		BufferedReader bufRead = new BufferedReader(istream);
		
		try {
			String theInput = bufRead.readLine();		 
			 
			return theInput;
			 
			}
			catch (IOException err) {
			System.out.println("Error reading line");
			}
			
		return null;	
	}
}





