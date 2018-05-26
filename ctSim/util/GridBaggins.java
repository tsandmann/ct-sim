/*
 * c't-Sim - Robotersimulator für den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */

package ctSim.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * Provides a <a href="http://en.wikipedia.org/wiki/Fluent_interface">fluent interface</a> for
 * {@link GridBagConstraints} objects. Allows to work with GridBagConstraints in a sane way, and provides
 * a few naming clarifications. Example:
 *
 * <pre>
 * GridBagConstraints gbc = new GridBaggins().row(4).col(3).northwest().fillH();
 * </pre>
 *
 * is equivalent to:
 *
 * <pre>
 * GridBagConstraints gbc = new GridBagConstraints();
 * gbc.gridy = 4;
 * gbc.gridx = 3;
 * gbc.anchor = GridBagConstraints.NORTHWEST;
 * gbc.fill = GridBagConstraints.HORIZONTAL;
 * </pre>
 *
 * Note: Documentation of most methods is copied from the corresponding {@link GridBagConstraints} field
 * or constant.
 *
 * @author Hendrik Krauß
 */
public class GridBaggins extends GridBagConstraints {
	/** UID */
	private static final long serialVersionUID = - 1427751556874174611L;

	/**
	 * Put the component in the center of its display area.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins center() {
		anchor = CENTER;
		return this;
	}

	/**
	 * Put the component on the right side of its display area, centered vertically.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins east() {
		anchor = EAST;
		return this;
	}

	/**
	 * Put the component at the top of its display area, centered horizontally.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins north() {
		anchor = NORTH;
		return this;
	}

	/**
	 * Put the component at the top-right corner of its display area.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins northeast() {
		anchor = NORTHEAST;
		return this;
	}

	/**
	 * Put the component at the top-left corner of its display area.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins northwest() {
		anchor = NORTHWEST;
		return this;
	}

	/**
	 * Put the component at the bottom-right corner of its display area.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins southeast() {
		anchor = SOUTHEAST;
		return this;
	}

	/**
	 * Put the component at the bottom of its display area, centered horizontally.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins south() {
		anchor = SOUTH;
		return this;
	}

	/**
	 * Put the component at the bottom-left corner of its display area.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins southwest() {
		anchor = SOUTHWEST;
		return this;
	}

	/**
	 * Put the component on the left side of its display area, centered vertically.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins west() {
		anchor = WEST;
		return this;
	}

	/**
	 * Do not resize the component.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins nofill() {
		fill = NONE;
		return this;
	}

	/**
	 * Resize the component horizontally but not vertically.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins fillH() {
		fill = HORIZONTAL;
		return this;
	}

	/**
	 * Resize the component vertically but not horizontally.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins fillV() {
		fill = VERTICAL;
		return this;
	}

	/**
	 * Resize the component both horizontally and vertically.
	 *
	 * @return GridBaggin
	 */
	public GridBaggins fillHV() {
		fill = BOTH;
		return this;
	}

	/**
	 * Specifies the number of cells in a column for the component's display area.
	 * <p>
	 * Use <code>REMAINDER</code> to specify that the component's display area will be from <code>gridy</code>
	 * to the last cell in the column.
	 * Use <code>RELATIVE</code> to specify that the component's display area will be from <code>gridy</code>
	 * to the next to the last one in its column.
	 * <p>
	 * <code>gridheight</code> should be a non-negative value and the default value is 1.
	 *
	 * @param numRows
	 * @return GridBaggin
	 */
	public GridBaggins gridheight(int numRows) {
		gridheight = numRows;
		return this;
	}

	/**
	 * Alias for {@link #gridheight(int)}. HTML people will find this name more intuitive.
	 *
	 * @param numRows
	 * @return GridBaggin
	 */
	public GridBaggins rowspan(int numRows) {
		return gridheight(numRows);
	}

	/**
	 * Specifies the number of cells in a row for the component's display area.
	 * <p>
	 * Use <code>REMAINDER</code> to specify that the component's display area will be from <code>gridx</code>
	 * to the last cell in the row.
	 * Use <code>RELATIVE</code> to specify that the component's display area will be from <code>gridx</code>
	 * to the next to the last one in its row.
	 * <p>
	 * <code>gridwidth</code> should be non-negative and the default value is 1.
	 *
	 * @param numCols
	 * @return GridBaggin
	 */
	public GridBaggins gridwidth(int numCols) {
		gridwidth = numCols;
		return this;
	}

	/**
	 * Alias for {@link #gridwidth(int)}. HTML people will find this name more intuitive.
	 *
	 * @param numCols
	 * @return GridBaggin
	 */
	public GridBaggins colspan(int numCols) {
		return gridwidth(numCols);
	}

	/**
	 * Specifies the cell containing the leading edge of the component's display area, where the first
	 * cell in a row has <code>gridx=0</code>. The leading edge of a component's display area is its
	 * left edge for a horizontal, left-to-right container and its right edge for a horizontal,
	 * right-to-left container. The value <code>RELATIVE</code> specifies that the component be placed
	 * immediately following the component that was added to the container just before this component
	 * was added.
	 * <p>
	 * The default value is <code>RELATIVE</code>. <code>gridx</code> should be a non-negative value.
	 *
	 * @param col
	 * @return GridBaggin
	 */
	public GridBaggins gridx(int col) {
		gridx = col;
		return this;
	}

	/**
	 * Alias for {@link #gridx(int)} ("column"). Seems more intuitive to me.
	 *
	 * @param col
	 * @return GridBaggin
	 */
	public GridBaggins col(int col) {
		return gridx(col);
	}

	/**
	 * <p>
	 * Specifies the cell at the top of the component's display area, where the topmost cell has
	 * <code>gridy=0</code>. The value <code>RELATIVE</code> specifies that the component be placed
	 * just below the component that was added to the container just before this component was added.
	 * </p>
	 * <p>
	 * The default value is <code>RELATIVE</code>. <code>gridy</code> should be a non-negative value.
	 * </p>
	 *
	 * @param row
	 * @return GridBaggin
	 */
	public GridBaggins gridy(int row) {
		gridy = row;
		return this;
	}

	/**
	 * Alias for {@link #gridy(int)}. Seems more intuitive to me.
	 *
	 * @param row
	 * @return GridBaggin
	 */
	public GridBaggins row(int row) {
		return gridy(row);
	}

   /**
	 * <p>
	 * This field specifies the external padding of the component, the minimum amount of space between
	 * the component and the edges of its display area.
	 * </p>
	 * <p>
	 * The default value is <code>new Insets(0, 0, 0, 0)</code>.
	 * </p>
	 *
	 * @param is
	 * @return GridBaggin
	 */
	public GridBaggins insets(Insets is) {
		insets = is;
		return this;
	}

	/**
	 * @param padding
	 * @return GridBaggin
	 */
	public GridBaggins epadx(int padding) {
		return insets(new Insets(insets.top, padding, insets.bottom, padding));
	}

	/**
	 * @param padding
	 * @return GridBaggin
	 */
	public GridBaggins epady(int padding) {
		return insets(new Insets(padding, insets.left, padding, insets.right));
	}

	/**
	 * <p>
	 * This field specifies the internal padding of the component, how much space to add to the minimum
	 * width of the component. The width of the component is at least its minimum width plus <code>ipadx</code>
	 * pixels.
	 * </p>
	 * <p>
	 * The default value is <code>0</code>.
	 * </p>
	 *
	 * @param padding
	 * @return GridBaggin
	 */
	public GridBaggins ipadx(int padding) {
		ipadx = padding;
		return this;
	}

	/**
	 * <p>
	 * This field specifies the internal padding, that is, how much space to add to the minimum height
	 * of the component. The height of the component is at least its minimum height plus <code>ipady</code>
	 * pixels.
	 * </p>
	 * <p>
	 * The default value is 0.
	 * </p>
	 *
	 * @param padding
	 * @return GridBaggin
	 */
	public GridBaggins ipady(int padding) {
		ipady = padding;
		return this;
	}

	/**
	 * Specifies how to distribute extra horizontal space.
	 * <p>
	 * The grid bag layout manager calculates the weight of a column to be the maximum <code>weightx</code>
	 * of all the components in a column. If the resulting layout is smaller horizontally than the area it
	 * needs to fill, the extra space is distributed to each column in proportion to its weight. A column
	 * that has a weight of zero receives no extra space.
	 * </p>
	 * <p>
	 * If all the weights are zero, all the extra space appears between the grids of the cell and the left
	 * and right edges.
	 * </p>
	 * <p>
	 * The default value of this field is <code>0</code>. <code>weightx</code> should be a non-negative value.
	 * </p>
	 *
	 * @param weight
	 * @return GridBaggin
	 */
	public GridBaggins weightx(double weight) {
		weightx = weight;
		return this;
	}

	/**
	 * Specifies how to distribute extra vertical space.
	 * <p>
	 * The grid bag layout manager calculates the weight of a row to be the maximum <code>weighty</code>
	 * of all the components in a row. If the resulting layout is smaller vertically than the area it
	 * needs to fill, the extra space is distributed to each row in proportion to its weight. A row that
	 * has a weight of zero receives no extra space.
	 * </p>
	 * <p>
	 * If all the weights are zero, all the extra space appears between the grids of the cell and the top
	 * and bottom edges.
	 * </p>
	 * <p>
	 * The default value of this field is <code>0</code>. <code>weighty</code> should be a non-negative value.
	 * </p>
	 *
	 * @param weight
	 * @return GridBaggin
	 */
	public GridBaggins weighty(double weight) {
		weighty = weight;
		return this;
	}
}
