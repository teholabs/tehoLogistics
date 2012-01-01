tehoLogistics
teho Labs/B. A. Bryce 2012

This program is released under the MIT Licence:
http://en.wikipedia.org/wiki/MIT_License

It currently makes use of the octopart API to resolve Digikey partnumbers

Use of the Octopart API requires the following statement:

Powered by Octopart http://octopart.com

Digikey currently has a CRC javascript execute onload() which posts to get the actual search data.
This was not used until sometime after Nov. 2011.

The sourcing function to pick the best price currently (0.10) does not consider shipping overhead.

This function will be added in a future release.

Change Log:

Current Version 0.10

0.10
Change digikey part name resolution to octopart API

0.09
Unrelased used direct Digikey searching to resolve part name

Features:
Imports invoices Mouser PDF, Digikey PDF, Arrow HTML (email)
Keeps current inventory and average unit cost of application identical parts in CSV file
Removes inventory via "building" lists (AKA making PCBs), lists defined externally via CSV file
Look up and modifiy inventory data amount of single parts
Check if parts are on hand for production of set of designs and create shopping list if needed
Create buy lists for lowest cost source importable at Arrow, Mouser, Digikey parts needed a production run
