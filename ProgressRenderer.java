import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;

class ProgressRenderer extends JProgressBar implements TableCellRenderer {
    public ProgressRenderer(int min, int max) {
	super(min, max);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
						   boolean hasFocus, int row, int column) {
	setValue((int) ((Float) value).floatValue());
	return this;
    }

}
