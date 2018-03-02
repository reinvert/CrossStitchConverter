package com.stitch.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

import com.stitch.converter.model.StitchColor;

/**
 * Reads CSV file.
 * @author Reinvert
 */
class CSVReader
{
	private CSVReader()
	{
		throw new AssertionError();
	}
	/**
	 * Convert CSV {@link java.lang.String String} to a 2nd-dimensional Array.
	 * @param str - CSV {@link java.lang.String String}.
	 * @return 2nd-dimensional Array.
	 */
	static ArrayList<ArrayList<String>> read(final String str)
	{
		final ArrayList<String> splitByRow = new ArrayList<String>(Arrays.asList(str.split("\r\n")));
		int maxRowSize = 0;
		for(int i=0; i<splitByRow.size(); i++)
		{
			final ArrayList<String> row = new ArrayList<String>(Arrays.asList(splitByRow.get(0).split(",")));
			final int rowSize = row.size();
			if(maxRowSize<rowSize)
			{
				maxRowSize = rowSize;
			}
		}
		final ArrayList<ArrayList<String>> output = new ArrayList<ArrayList<String>>();
		for(int i=0; i<splitByRow.size(); i++){
			output.add(new ArrayList<String>());
			final ArrayList<String> row = new ArrayList<String>(Arrays.asList(splitByRow.get(i).split(",")));
			for(int j=0; j<row.size(); j++){
				output.get(i).add(row.get(j));
			}
		}
		return output;
	}
	
	/**
	 * Convert 2nd-dimensional CSV {@link java.lang.String String} Array to a {@link java.awt.Color Color} Array.
	 * @param csv - 2nd-dimensional CSV {@link java.lang.String String} Array.
	 * @return {@link java.awt.Color Color} Array.
	 * @throws NoSuchElementException occurs when one or more of R, G, or B values is missing.
	 * @throws NumberFormatException occurs when one or more of R, G, or B values can not be read.
	 * @throws IllegalArgumentException occurs when one or more of the R, G, or B values is not a value between 0 and 255.
	 */
	static ArrayList<StitchColor> readColorList(final ArrayList<ArrayList<String>> csv) throws NoSuchElementException, NumberFormatException, IllegalArgumentException
	{
		final ArrayList<StitchColor> output = new ArrayList<StitchColor>();
		try{
			for(int i=0; i<csv.size(); i++)
			{
				output.add(new StitchColor(Integer.parseInt(csv.get(i).get(1)), Integer.parseInt(csv.get(i).get(2)), Integer.parseInt(csv.get(i).get(3)), csv.get(i).get(0)));
			}
		}
		catch(final ArrayIndexOutOfBoundsException e)
		{
			throw new NoSuchElementException();
		}
		return output;
	}
}
