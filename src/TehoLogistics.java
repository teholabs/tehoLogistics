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

import java.io.*;
import java.util.*;

public class TehoLogistics {

private static Logistics myLogistics = new Logistics();
private static Vector <String> mainMenu = new Vector <String>();
	
	public static void main(String[] args) {
		
		int menuChoice = 0;
		initalizeMenus();
	
		String consoleInput;
		Integer itemID, listID, listCount, itemCount;
		Double buildCost, unitBuildCost;
		
		PartList workingList;
		PartList neededOrder;
		Vector <PartList> orders;
	
		while(true){
			System.out.println(makeMenuString(mainMenu));
			System.out.println("Select Option:");
			menuChoice = getConsoleInt();
			
			switch(menuChoice){
			//Import invoices
			case 1:
				myLogistics.processInvoices();
				break;
				
			//Check inventory for build
			case 2:
				//select part list/count	
				workingList = new PartList();
				neededOrder = new PartList();
				do{
					do{
						listID = myLogistics.findPartList();					
					}while(listID == -1);
					System.out.println("Multplicity?");
					listCount = getConsoleInt();					
					workingList.aggregateList(myLogistics.getPartList(listID), listCount);
					//loop until all boards needed
					System.out.println("Add another list? (y/n)");
					consoleInput = getConsoleString();
				}while(consoleInput.compareToIgnoreCase("n") != 0);
				//display parts required
				System.out.println("List of required parts:");
				System.out.println(workingList.toString());				
				//find if parts are in stock
				neededOrder = myLogistics.buildableList(workingList);
				//display quantity needed if not in stock
				if(neededOrder.getLineItems() > 0)
				{
					System.out.println("Parts that need to be ordered:");
					System.out.println(neededOrder.toString());
					//output order part list
					System.out.println("Input filename for order CSV:");
					consoleInput = getConsoleString();
					neededOrder.setName(consoleInput);
					makeCSVlinesFromPartList(neededOrder);
				}
				else System.out.println("All needed parts on hand");				
				break;
				
			//Build Boards
			case 3:
				//select part list/count built
				workingList = new PartList();
				do{
					listID = myLogistics.findPartList();					
				}while(listID == -1);
				System.out.println("Multplicity?");
				listCount = getConsoleInt();
				workingList.aggregateList(myLogistics.getPartList(listID), listCount);
				
				//display parts used
				do{
					System.out.println("List of parts used:");
					System.out.println(workingList.toString());
					System.out.println("Adjust part count? (y/n)");
					consoleInput = getConsoleString();
					if(consoleInput.compareToIgnoreCase("y") == 0){
						System.out.println("Part Index?");
						itemID = getConsoleInt();
						System.out.println("Count?");
						itemCount = getConsoleInt();
						workingList.setCount(itemID, itemCount);
					}
					
				}while(consoleInput.compareToIgnoreCase("n") != 0);
			
				//withdraw parts and give price of build
				buildCost = myLogistics.removeAndCostPartList(workingList);
				unitBuildCost = buildCost/listCount;
				System.out.println("Build Cost: " + buildCost.toString() + " Cost per unit: " + unitBuildCost.toString());
				break;
				
			//Stock info
			case 4:
				//Find part via category listing
				do{
					itemID = myLogistics.findPart();					
				}while(itemID == -1);				
				System.out.println(myLogistics.getInventoryItem(itemID).toString());
				
				//decide if stock should be changed
				do
				{
					System.out.println("Change stock info? (y/n)");
					consoleInput = getConsoleString();
				}while(consoleInput.compareToIgnoreCase("y") != 0 && consoleInput.compareToIgnoreCase("n") != 0);
				
				if(consoleInput.compareToIgnoreCase("y") == 0){
					
					do
					{
						System.out.println("Remove Units? (y/n)");
						consoleInput = getConsoleString();
					}while(consoleInput.compareToIgnoreCase("y") != 0 && consoleInput.compareToIgnoreCase("n") != 0);
					
					//adjust stock
					if(consoleInput.compareToIgnoreCase("y") == 0){
						int unitCount;
						Double totalCost;
						System.out.println("Count to remove?");
						unitCount = getConsoleInt();
						if(unitCount != -1){
							totalCost = myLogistics.removeInventory(itemID, unitCount);
							System.out.println("Cost: " + totalCost.toString());
						}
						
					}
					else
					{
						int unitCount;
						double unitCost;
						System.out.println("Count to add?");
						unitCount = getConsoleInt();
						System.out.println("Unit cost?");
						unitCost = getConsoleDouble();
						if(unitCount != -1 && unitCost > 0){
							myLogistics.addInventory(itemID, unitCount, unitCost*unitCount);
						}
							
					}
							
					
				}
				
				break;
			//Source partlist
			case 5:
				workingList = new PartList();
				neededOrder = new PartList();
				
				do{
					//prompt for partlist file to use
					System.out.println("Order part list file to load?");
					consoleInput = getConsoleString();
					
					//loads the lists 1 at a time from file
					int i = 0;
					do{
						workingList = myLogistics.readSingleListFile(consoleInput + ".csv", i);
						neededOrder.aggregateList(workingList, 1);
						i++;
					}while(workingList.getLineItems() > 0);
					
					do
					{
						System.out.println("Load another file? (y/n)");
						consoleInput = getConsoleString();
					}while(consoleInput.compareToIgnoreCase("y") != 0 && consoleInput.compareToIgnoreCase("n") != 0);
					
				}while(consoleInput.compareToIgnoreCase("n") != 0);
				//loop until all lists loaded
								
				//search for best prices
				orders = myLogistics.sourcePartsList(neededOrder);				
				
				//save order files
				System.out.println("Order name?");
				consoleInput = getConsoleString();
				saveOrderFiles(consoleInput, orders);
				break;
			//Save inventory
			case 6:
				myLogistics.writeInventory(myLogistics.getMaxSources());
				break;
			//exit
			case 7:
				System.exit(0); 
				break;
				

			}
		}


	}
	

	private static void initalizeMenus(){
		mainMenu.add("\n\nMain Menu:");
		mainMenu.add("Import invoices");
		mainMenu.add("Check stock/make part list for board build");
		mainMenu.add("Build boards");
		mainMenu.add("View modify stock info");
		mainMenu.add("Source parts from list");
		mainMenu.add("Save inventory changes");
		mainMenu.add("Exit");

	}
	
	private static String makeMenuString(Vector <String> amenu){
		String completeMenu = new String();
		completeMenu += amenu.get(0) + "\n";
		for(Integer i=1; i<amenu.size(); i++){
			completeMenu += "(" + i.toString() + ")" + amenu.get(i) + "\n";
		}		
		return completeMenu;
	}
	
	
	private static int getConsoleInt(){
		InputStreamReader istream = new InputStreamReader(System.in);
		BufferedReader bufRead = new BufferedReader(istream);
		
		try {
			String theInput = bufRead.readLine();
			 
			int avalue = Integer.parseInt(theInput);
			
			return avalue;
			 
			}
			catch (IOException err) {
			System.out.println("Error reading line");
			}
			catch(NumberFormatException err) {
				System.out.println("Error Converting Number");
			} 
			
		return -1;	
	}
	
	
	private static Double getConsoleDouble(){
		InputStreamReader istream = new InputStreamReader(System.in);
		BufferedReader bufRead = new BufferedReader(istream);
		
		try {
			String theInput = bufRead.readLine();		 
			 
			Double avalue = Double.parseDouble(theInput);
			
			return avalue;
			 
			}
			catch (IOException err) {
			System.out.println("Error reading line");
			}
			catch(NumberFormatException err) {
				System.out.println("Error Converting Number");
			} 
			
		return (double) -1;	
	}
	
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
	
	public static void makeCSVlinesFromPartList(PartList alist){
		Vector <String> lines = new Vector <String>();	
		lines.add(alist.getName() + ",");	
		
		for(int i = 0; i<alist.getLineItems(); i++){
			lines.add(alist.toCSVline(i));
		}
		lines.add("endList");
		writeCSV(alist.getName(), lines);
		
	}
	
	//no backup just writes a CSV file
	private static void writeCSV(String fileName, Vector <String> lines){
		writeTextFile(fileName + ".csv", lines);		
	}
	
	private static void writeTextFile(String fileName, Vector <String> lines){
		FileWriter inventoryFile;		
		try {		
			inventoryFile = new FileWriter(fileName);			
			for(int i = 0; i<lines.size(); i++){
				inventoryFile.write(lines.get(i));
				inventoryFile.write("\r\n");
			}
			try {
				inventoryFile.close();
			} catch (IOException e) {
				System.out.println("Warning: Could not close file");
			}
			
		} catch (IOException e) {
			System.out.println("File write error");
		} 
	}

	
	private static void saveOrderFiles(String orderName, Vector <PartList> partListVector){
		Vector <String> lines = new Vector <String>();
		
		for(int i=0; i<partListVector.size(); i++){
			
			if(partListVector.get(i).getName().compareToIgnoreCase("arrow") == 0)
			{
				lines.clear();
				for(int k=0; k<partListVector.get(i).getLineItems(); k++){
					lines.add(partListVector.get(i).toArrowLine(k));
				}
				writeTextFile(orderName + "arrow.csv", lines);
			}
			else if(partListVector.get(i).getName().compareToIgnoreCase("digikey") == 0  || partListVector.get(i).getName().compareToIgnoreCase("digi-key") == 0)
			{
				lines.clear();
				for(int k=0; k<partListVector.get(i).getLineItems(); k++){
					lines.add(partListVector.get(i).toDigikeyLine(k));
				}
				writeTextFile(orderName + "digikey.txt", lines);
			}
			else if(partListVector.get(i).getName().compareToIgnoreCase("mouser") == 0)
			{
				lines.clear();
				for(int k=0; k<partListVector.get(i).getLineItems(); k++){
					lines.add(partListVector.get(i).toMouserLine(k));
				}
				writeTextFile(orderName + "mouser.txt", lines);
			}
		}
		
	}

}



