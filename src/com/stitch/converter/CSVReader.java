package com.stitch.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import com.stitch.converter.model.StitchColor;

/**
 * Reads CSV file.
 * 
 * @author Reinvert
 */
final class CSVReader {
	/**
	 * Convert CSV {@link java.lang.String String} to a 2nd-dimensional
	 * {@link ArrayList}.
	 * 
	 * @param str - CSV {@link java.lang.String String}.
	 * @return 2nd-dimensional {@link ArrayList}.
	 */
	static ArrayList<ArrayList<String>> read(final String str) {
		final List<String> splitByRow = Arrays.asList(str.replace("\r", "").split("\n"));
		final ArrayList<ArrayList<String>> output = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < splitByRow.size(); i++) {
			output.add(new ArrayList<String>(Arrays.asList(splitByRow.get(i).split(","))));
		}
		return output;
	}

	/**
	 * Convert 2nd-dimensional CSV {@link String} {@link ArrayList} to a
	 * {@link StitchColor} {@link ArrayList}.
	 * 
	 * @param csv - 2nd-dimensional CSV {@link String} Array.
	 * @return {@link StitchColor} Array.
	 * @throws NoSuchElementException   occurs when one or more of R, G, or B values
	 *                                  is missing.
	 * @throws NumberFormatException    occurs when one or more of R, G, or B values
	 *                                  can not be read.
	 * @throws IllegalArgumentException occurs when one or more of the R, G, or B
	 *                                  values is not a value between 0 and 255.
	 */
	static ArrayList<StitchColor> readColorList(final ArrayList<ArrayList<String>> csv)
			throws NoSuchElementException, NumberFormatException, IllegalArgumentException {
		final ArrayList<StitchColor> output = new ArrayList<StitchColor>();
		int i = 0;
		try {
			for (; i < csv.size(); i++) {
				output.add(new StitchColor(Integer.parseInt(csv.get(i).get(1)), Integer.parseInt(csv.get(i).get(2)),
						Integer.parseInt(csv.get(i).get(3)), csv.get(i).get(0)));
			}
		} catch (final ArrayIndexOutOfBoundsException e) {
			throw new NoSuchElementException(Integer.toString(i + 1));
		} catch (final NumberFormatException e) {
			throw new NumberFormatException(Integer.toString(i + 1));
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException(Integer.toString(i + 1));
		}
		return output;
	}

	private CSVReader() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
