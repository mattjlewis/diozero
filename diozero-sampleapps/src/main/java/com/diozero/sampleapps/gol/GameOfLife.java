package com.diozero.sampleapps.gol;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     GameOfLife.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Random;

public class GameOfLife {
	private final int width;
	private final int height;
	private final int size;
	private boolean[] cells;
	
	public GameOfLife(int width, int height) {
		this.width = width;
		this.height = height;
		this.size = width * height;
		
		cells = new boolean[size];
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void randomise() {
		Random random = new Random();
		for (int i=0; i<size; i++) {
			cells[i] = random.nextBoolean();
		}
	}

	public boolean isAlive(int x, int y) {
		return cells[x + y*width];
	}
	
	/**
	 * Checks if the cell is alive
	 * @param x
	 *            The x position
	 * @param y
	 *            The y position
	 * @param d
	 *            The grid data.
	 * @return Alive
	 */
	private boolean willSurvive(int x, int y) {
		int count = 0;
		int cell_pos = y * width + x;

		for (int nx=x-1; nx<=x+1; nx++) {
			for (int ny=y-1; ny<=y+1; ny++) {
				int neighbour_pos = nx + ny * width;
				if (neighbour_pos >= 0 && neighbour_pos < size - 1 && neighbour_pos != cell_pos) {
					if (cells[neighbour_pos]) {
						count++;
					}
				}
			}
		}

		// Dead
		if (! cells[cell_pos]) {
			// Becomes alive if 3 neighbours
			return count == 3;
		}
		
		// Alive
		return count >= 2 && count <= 3;
	}

	/**
	 * Iterates the game one step forward
	 */
	public void iterate() {
		boolean[] next = new boolean[size];
		for (int x=0; x<width; x++){
			for (int y=0; y<height; y++){
				next[x + y * width] = willSurvive(x, y);
			}
		}

		cells = next;
	}
}
