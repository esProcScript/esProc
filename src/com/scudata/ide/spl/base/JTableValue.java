package com.scudata.ide.spl.base;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.DBConfig;
import com.scudata.common.DBInfo;
import com.scudata.common.IntArrayList;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Canvas;
import com.scudata.dm.DBObject;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.ide.common.AppendDataThread;
import com.scudata.ide.common.DBTypeEx;
import com.scudata.ide.common.EditListener;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.CellSelection;
import com.scudata.ide.common.control.TransferableObject;
import com.scudata.ide.common.dialog.DialogCellFormat;
import com.scudata.ide.common.swing.AllPurposeEditor;
import com.scudata.ide.common.swing.AllPurposeRenderer;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.JTextFieldReadOnly;
import com.scudata.ide.spl.GVSpl;
import com.scudata.ide.spl.dialog.DialogDisplayChart;
import com.scudata.ide.spl.dialog.DialogTextEditor;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.util.Variant;

/**
 * 值面板
 *
 */
public class JTableValue extends JTableEx {
	private static final long serialVersionUID = -4530154524747498116L;
	/**
	 * 集算器资源管理器
	 */
	private static MessageManager mm = IdeSplMessage.get();
	/**
	 * 序号列名
	 */
	private final String TITLE_INDEX = mm.getMessage("public.index");
	/**
	 * 成员列名
	 */
	private final String TITLE_SERIES = mm.getMessage("jtablevalue.menber");

	/**
	 * 第一列
	 */
	private final int COL_FIRST = 0;

	/** 缺省 */
	private final byte TYPE_DEFAULT = 0;
	/** 序表 */
	private final byte TYPE_TABLE = 1;
	/** 序列 */
	private final byte TYPE_SERIES = 2;
	/** 记录 */
	private final byte TYPE_RECORD = 3;
	/** 纯排列 */
	private final byte TYPE_PMT = 4;
	/** 排列，暂时弃用，按照普通序列进行显示 */
	private final byte TYPE_SERIESPMT = 5;
	/** DBInfo对象 */
	private final byte TYPE_DB = 6;
	/** FileObject对象 */
	private final byte TYPE_FILE = 7;

	/**
	 * 值类型
	 */
	private byte m_type = TYPE_DEFAULT;

	/**
	 * 值和原始值
	 */
	private Object value, originalValue;

	/**
	 * 画布，在画图时用到
	 */
	private Canvas canvas;

	/**
	 * 是否可编辑
	 */
	private boolean editable;

	/**
	 * 用于数据钻取和返回的堆栈
	 */
	private Stack<Object> undo = new Stack<Object>();
	private Stack<Object> redo = new Stack<Object>();

	/**
	 * 是否固定显示某个单元格值，不随着光标变化
	 */
	private boolean isLocked = false;
	/**
	 * 用来区分是否因为计算模式锁的。现在取消了计算格式，暂时没用了
	 */
	private boolean isLocked1 = false;

	/**
	 * 单元格名称
	 */
	private String cellId;
	/** 复制值 */
	public static final short iCOPY = 11;
	/** 复制列名 */
	public static final short iCOPY_COLNAMES = 12;
	/** 粘贴 */
	private final short iPASTE = 13;
	/** 设置列格式 */
	private final short iFORMAT = 17;

	/**
	 * 行高
	 */
	private final int ROW_HEIGHT = 20;

	/**
	 * 行数
	 */
	private int rowCount = 0;

	/**
	 * 编辑监听器
	 */
	private EditListener editListener = null;

	/**
	 * 选择的行号
	 */
	private IntArrayList selectedRows = new IntArrayList();

	/**
	 * 之前选择的行号
	 */
	private int lastRow = -1;

	/**
	 * 构造函数
	 */
	public JTableValue() {
		DragGestureListener dgl = new DragGestureListener() {
			public void dragGestureRecognized(DragGestureEvent dge) {
				try {
					Transferable tf = new TransferableObject(value);
					if (tf != null) {
						dge.startDrag(GM.getDndCursor(), tf);
					}
				} catch (Exception x) {
				}
			}
		};
		DragSource ds = DragSource.getDefaultDragSource();
		ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, dgl);

		DropTargetListener dtl = new DropTargetListener() {
			public void dragEnter(DropTargetDragEvent dtde) {
				acceptText();
			}

			public void dragOver(DropTargetDragEvent dtde) {
				if (!editable) {
					return;
				}
				Point p = dtde.getLocation();
				int row = rowAtPoint(p);
				int col = columnAtPoint(p);
				if (row < 0 || col < 0) {
					return;
				}
				setRowSelectionInterval(row, row);
				setColumnSelectionInterval(col, col);
			}

			public void dropActionChanged(DropTargetDragEvent dtde) {
			}

			public void dragExit(DropTargetEvent dte) {
			}

			public void drop(DropTargetDropEvent dtde) {
				if (!editable) {
					return;
				}
				Point p = dtde.getLocation();
				int row = rowAtPoint(p);
				int col = columnAtPoint(p);
				if (row < 0 || col < 0) {
					return;
				}
				if (!isCellEditable(row, col)) {
					return;
				}
				Object value = null;
				try {
					value = dtde.getTransferable().getTransferData(TransferableObject.objectFlavor);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (value == null) {
					return;
				}
				setValueAt(value, row, col);
			}
		};
		DropTarget dt = new DropTarget(this, dtl);
		setDropTarget(dt);
		setRowHeight(ROW_HEIGHT);
		addMWListener(this);

		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int row = lastRow;
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP:
					row--;
					if (row < 0)
						row = 0;
					rowSelected(e, row);
					break;
				case KeyEvent.VK_DOWN:
					row++;
					if (row > rowCount - 1)
						row = rowCount - 1;
					rowSelected(e, row);
					break;
				case KeyEvent.VK_PAGE_UP:
					row -= getPageRows();
					if (row < 0)
						row = 0;
					rowSelected(e, row);
					break;
				case KeyEvent.VK_PAGE_DOWN:
					row += getPageRows();
					if (row > rowCount - 1)
						row = rowCount - 1;
					rowSelected(e, row);
					break;
				case KeyEvent.VK_A:
					if (e.isControlDown()) {
						selectedRows.clear();
						for (int i = 0; i < rowCount; i++)
							selectedRows.addInt(i);
						resetSelection();
						lastRow = rowCount - 1;
					}
					break;
				case KeyEvent.VK_C:
					if (e.isControlDown()) {
						copyValue();
					}
					e.consume();
					break;
				}
			}
		});
	}

	/**
	 * 设置编辑监听器
	 * 
	 * @param el
	 */
	public void setEditListener(EditListener el) {
		this.editListener = el;
	}

	/**
	 * 取每页显示的行数
	 * 
	 * @return
	 */
	private int getPageRows() {
		int height = GVSpl.panelValue.spValue.getPreferredSize().height;
		return height / ROW_HEIGHT + 1;
	}

	/**
	 * 选择了行
	 * 
	 * @param e
	 * @param row
	 */
	private void rowSelected(InputEvent e, int row) {
		if (e.isControlDown()) {
			if (selectedRows.containsInt(row))
				selectedRows.removeInt(row);
			else
				selectedRows.addInt(row);
			resetSelection();
			lastRow = row;
		} else if (e.isShiftDown()) {
			selectedRows.clear();
			int min = Math.min(row, lastRow);
			int max = Math.max(row, lastRow);
			for (int i = min; i <= max; i++) {
				selectedRows.addInt(i);
			}
			resetSelection();
		} else {
			selectedRows.clear();
			selectedRows.addInt(row);
			resetSelection();
			lastRow = row;
		}
		if (e instanceof KeyEvent) {
			e.consume();
		}
	}

	/**
	 * 增加鼠标滚轮监听
	 * 
	 * @param com
	 */
	public void addMWListener(JComponent com) {
		com.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					int amount = e.getScrollAmount();
					int rotation = e.getWheelRotation();
					if (rotation < 0) {
						amount = -amount;
					}
					JScrollBar sbValue = GVSpl.panelValue.sbValue;
					sbValue.setValue(sbValue.getValue() + amount);
					resetData(sbValue.getValue());
				}
			}
		});
	}

	/**
	 * 鼠标双击时钻取数据
	 */
	public void doubleClicked(int xpos, int ypos, int row, int col, MouseEvent e) {
		drillValue(row, col);
	}

	/**
	 * 鼠标右键菜单
	 */
	public void rightClicked(int xpos, int ypos, final int row, final int col, MouseEvent e) {
		JPopupMenu pm = new JPopupMenu();
		JMenuItem mItem;
		int selectedCol = getSelectedColumn();
		if (selectedCol > -1 && (m_type == TYPE_TABLE || m_type == TYPE_PMT || m_type == TYPE_SERIESPMT)) {
			mItem = new JMenuItem(mm.getMessage("jtablevalue.editformat")); // 列格式编辑
			mItem.setIcon(GM.getMenuImageIcon("blank"));
			mItem.setName(String.valueOf(iFORMAT));
			mItem.addActionListener(popAction);
			pm.add(mItem);
		}
		mItem = new JMenuItem(LABEL_COPY_COLUMN); // 复制列名
		mItem.setIcon(GM.getMenuImageIcon("blank"));
		mItem.setName(String.valueOf(iCOPY_COLNAMES));
		mItem.addActionListener(popAction);
		pm.add(mItem);

		if (row > -1 && col > -1) {
			final Object cellVal = data.getValueAt(row, col);
			if (cellVal != null && cellVal instanceof String) {
				mItem = new JMenuItem(LABEL_VIEW_TEXT);
				mItem.setIcon(GM.getImageIcon(GC.IMAGES_PATH + "b_showtext.gif"));
				mItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						showText(row, col, cellVal);
					}
				});
				pm.add(mItem);
			}
		}
		pm.show(e.getComponent(), e.getX(), e.getY());
	}

	/** 复制列名 */
	public static final String LABEL_COPY_COLUMN = mm.getMessage("jtablevalue.copycolnames");
	/** 查看长文本 */
	public static final String LABEL_VIEW_TEXT = mm.getMessage("dialogtexteditor.title1");

	/**
	 * 右键菜单事件监听
	 */
	private ActionListener popAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			JMenuItem mItem = (JMenuItem) e.getSource();
			short cmd = Short.parseShort(mItem.getName());
			switch (cmd) {
			case iCOPY:
				copyValue();
				break;
			case iCOPY_COLNAMES:
				copyColumnNames();
				break;
			case iPASTE:
				pasteValue();
				break;
			case iFORMAT:
				colFormat();
				break;
			}
		}
	};

	/**
	 * 列格式编辑
	 */
	private void colFormat() {
		int col = getSelectedColumn();
		if (col < 0) {
			return;
		}
		String colName = null;
		if (!StringUtils.isValidString(colName)) {
			colName = getColumnName(col);
		}
		String format = GM.getColumnFormat(colName);
		DialogCellFormat dcf = new DialogCellFormat();
		if (format != null) {
			dcf.setFormat(format);
		}
		dcf.setVisible(true);
		if (dcf.getOption() == JOptionPane.OK_OPTION) {
			format = dcf.getFormat();
			GM.saveFormat(colName, format);
			setColFormat(col, format);
		}

	}

	/**
	 * 设置列格式
	 * 
	 * @param col    列号
	 * @param format 格式
	 */
	private void setColFormat(int col, String format) {
		TableColumn tc = getColumn(col);
		tc.setCellEditor(getAllPurposeEditor());
		tc.setCellRenderer(new AllPurposeRenderer(format));
		this.repaint();
	}

	/**
	 * 刷新
	 */
	public void refresh() {
		forceSetValue(value);
	}

	/**
	 * 值是否null
	 * 
	 * @return
	 */
	public boolean valueIsNull() {
		return value == null;
	}

	/**
	 * 设置是否计算模式锁
	 * 
	 * @param locked1 是否计算模式锁
	 */
	public void setLocked1(boolean locked1) {
		this.isLocked1 = locked1;
		setLocked(locked1);
	}

	/**
	 * 是否计算模式锁
	 * 
	 * @return
	 */
	public boolean isLocked1() {
		return this.isLocked1;
	}

	/**
	 * 设置是否锁定单元格
	 * 
	 * @param locked 是否锁定单元格
	 */
	public void setLocked(boolean locked) {
		this.isLocked = locked;
		GVSpl.panelValue.valueBar.setLocked(locked);
	}

	/**
	 * 取是否锁定单元格
	 * 
	 * @return
	 */
	public boolean isLocked() {
		return isLocked;
	}

	/**
	 * 鼠标按键事件
	 */
	public void mousePressed(MouseEvent e) {
		refreshValueButton();
		int row = rowAtPoint(e.getPoint());
		if (row < 0) {
			return;
		}
		row += GVSpl.panelValue.sbValue.getValue() - 1;
		if (selectedRows.isEmpty()) {
			selectedRows.addInt(row);
			resetSelection();
			lastRow = row;
			return;
		}
		rowSelected(e, row);
	}

	/**
	 * 刷新按钮状态
	 */
	private void refreshValueButton() {
		GVSpl.panelValue.valueBar.refresh();
	}

	/**
	 * 重置选择状态
	 */
	private void resetSelection() {
		ListSelectionModel selectModel = getSelectionModel();
		selectModel.clearSelection();
		if (!selectedRows.isEmpty()) {
			int r;
			for (int i = 0; i < selectedRows.size(); i++) {
				r = selectedRows.getInt(i);
				r = r - GVSpl.panelValue.sbValue.getValue() + 1;
				if (r > -1 && r < getRowCount()) {
					selectModel.addSelectionInterval(r, r);
				}
			}
		}
		this.setSelectionModel(selectModel);
	}

	/**
	 * 重置数据
	 * 
	 * @param index 开始行号
	 */
	public void resetData(int index) {
		dispStartIndex = index;
		if (resetThread != null) {
			resetThread.stopThread();
			try {
				resetThread.join();
			} catch (Exception e) {
			}
		}
		resetThread = null;
		Sequence s;
		switch (m_type) {
		case TYPE_DB:
			s = dbTable;
			break;
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIESPMT:
		case TYPE_SERIES:
			if (!(value instanceof Sequence))
				return;
			s = (Sequence) value;
			break;
		default:
			return;
		}
		resetThread = new ResetDataThread(s, index, m_type);
		SwingUtilities.invokeLater(resetThread);
	}

	/**
	 * 线程实例
	 */
	private ResetDataThread resetThread = null;

	/**
	 * 设置序列(排列)数据的线程
	 *
	 */
	class ResetDataThread extends Thread {
		/**
		 * 序列对象
		 */
		Sequence seq;
		/**
		 * 开始行
		 */
		int index;
		/**
		 * 数据类型
		 */
		byte dataType;
		/**
		 * 是否停止了，是否完成了
		 */
		boolean isStoped = false, isFinished = false;

		/**
		 * 构造函数
		 * 
		 * @param s        序列
		 * @param index    开始行
		 * @param dataType 类型
		 */
		ResetDataThread(Sequence s, int index, byte dataType) {
			this.seq = s;
			this.index = index;
			this.dataType = dataType;
		}

		/**
		 * 执行
		 */
		public void run() {
			try {
				if (seq == null) {
					removeAllRows();
					return;
				}
				boolean isSeq = false;
				switch (m_type) {
				case TYPE_TABLE:
				case TYPE_PMT:
				case TYPE_SERIESPMT:
				case TYPE_SERIES:
					isSeq = true;
					break;
				}
				int height = GVSpl.panelValue.spValue.getPreferredSize().height;
				int startRow = index;
				int count = height / ROW_HEIGHT + 1;
				count = Math.max(50, count);
				int endRow = Math.min(rowCount, startRow + count);

				int oldRowCount = getRowCount();
				int dispRowCount = endRow - startRow + 1;

				if (isStoped)
					return;
				if (dispRowCount > oldRowCount) {
					for (int i = 0, size = dispRowCount - oldRowCount; i < size; i++) {
						addRow();
					}
				} else if (dispRowCount < oldRowCount) {
					for (int i = 0, size = oldRowCount - dispRowCount; i < size; i++) {
						removeRow(0);
					}
				}
				if (isStoped)
					return;
				Object rowData;
				for (int i = startRow; i <= endRow; i++) {
					if (isStoped)
						return;
					rowData = seq.get(i);
					if (rowData instanceof Record && (dataType == TYPE_PMT || dataType == TYPE_TABLE
							|| dataType == TYPE_SERIESPMT || dataType == TYPE_DB)) {
						setRecordRow((Record) seq.get(i), i - startRow, isSeq, i);
					} else {
						if (isSeq) {
							data.setValueAt(new Integer(i), i - startRow, COL_FIRST);
							data.setValueAt(seq.get(i), i - startRow, COL_FIRST + 1);
						} else {
							data.setValueAt(seq.get(i), i - startRow, COL_FIRST);
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				resetSelection();
				isFinished = true;
			}
		}

		/**
		 * 停止线程
		 */
		void stopThread() {
			seq = null;
			isStoped = true;
		}

		/**
		 * 是否已经完成
		 * 
		 * @return
		 */
		boolean isFinished() {
			return isFinished;
		}
	}

	/**
	 * 设置记录到表格的一行
	 * 
	 * @param record
	 * @param r
	 * @param isSeq
	 * @param index
	 */
	private void setRecordRow(Record record, int r, boolean isSeq, int index) {
		if (record == null || r < 0)
			return;
		if (m_type == TYPE_TABLE) {
			int colCount = this.getColumnCount();
			if (isSeq) {
				data.setValueAt(new Integer(index), r, COL_FIRST);
			}
			for (int j = isSeq ? 1 : 0; j < colCount; j++) {
				data.setValueAt(record.getFieldValue(isSeq ? j - 1 : j), r, j);
			}
		} else {
			if (isSeq) {
				data.setValueAt(new Integer(index), r, COL_FIRST);
			}
			DataStruct ds = record.dataStruct();
			String nNames[] = ds.getFieldNames();
			if (nNames != null) {
				Object val;
				for (int j = 0; j < nNames.length; j++) {
					try {
						val = record.getFieldValue(nNames[j]);
					} catch (Exception e) {
						// 取不到的显示空
						val = null;
					}
					int col = getColumnIndex(nNames[j], isSeq ? 1 : 0);
					if (col > -1) {
						data.setValueAt(val, r, col);
					}
				}
			}
		}
	}

	/**
	 * 根据列名取列序号
	 * 
	 * @param colName
	 * @param startIndex
	 * @return
	 */
	public int getColumnIndex(String colName, int startIndex) {
		for (int i = startIndex; i < this.getColumnCount(); i++) {
			String name = getColumnName(i);
			if (name.equals(colName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 单元格是否可以编辑
	 */
	public boolean isCellEditable(int row, int column) {
		switch (m_type) {
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIESPMT:
			Sequence s = (Sequence) value;
			if (s.dataStruct() != null) {
				int count = s.dataStruct().getFieldCount();
				if (column > count) {
					return false;
				}
			}
		}
		TableColumn tc = getColumn(column);
		TableCellEditor tce = tc.getCellEditor();
		boolean readOnly = true;
		if (tce instanceof AllPurposeEditor) {
			AllPurposeEditor ape = (AllPurposeEditor) tce;
			readOnly = !ape.isCellEditable(null);
		}
		return editable && !readOnly;
	}

	/**
	 * 清理
	 */
	public void clear() {
		undo.clear();
		redo.clear();
		isLocked = false;
		value = null;
		initJTable();
	}

	/**
	 * 设置单元格名称
	 * 
	 * @param id
	 */
	public void setCellId(String id) {
		if (this.isLocked) {
			return;
		}
		this.cellId = id;
	}

	/**
	 * 取单元格名称
	 * 
	 * @return
	 */
	public String getCellId() {
		return cellId;
	}

	/**
	 * 设置单元格值
	 * 
	 * @param value 单元格值
	 */
	public void setValue(Object value) {
		setValue(value, false);
	}

	/**
	 * 立即设置单元格值，不考虑锁定状态
	 * 
	 * @param value 单元格值
	 * @param id    单元格名称
	 */
	public void setValue1(Object value, String id) {
		this.originalValue = value;
		setValue(value, false, true);
		this.cellId = id;
	}

	/**
	 * 设置单元格值
	 * 
	 * @param value    单元格值
	 * @param editable 是否可以编辑
	 */
	public void setValue(Object value, boolean editable) {
		setValue(value, editable, false);
	}

	/**
	 * 设置单元格值
	 * 
	 * @param value         单元格值
	 * @param editable      是否可以编辑
	 * @param forceSetValue 是否无视锁定状态，强行设置格值
	 */
	private synchronized void setValue(Object value, boolean editable, final boolean forceSetValue) {
		setValue(value, editable, forceSetValue, null);
	}

	/**
	 * 
	 * 设置单元格值
	 * 
	 * @param value         单元格值
	 * @param editable      是否可以编辑
	 * @param forceSetValue 是否无视锁定状态，强行设置格值
	 * @param dispStartRow  显示开始行
	 */
	private synchronized void setValue(Object value, boolean editable, final boolean forceSetValue, UndoObject uo) {
		if (isLocked && !forceSetValue) {
			return;
		}
		if (value != null) {
			if (value instanceof Canvas) {
				this.canvas = (Canvas) value;
				value = ((Canvas) value).getChartElements();
			} else {
				this.canvas = null;
			}
		} else {
			this.canvas = null;
		}
		this.value = value;
		if (!forceSetValue) {
			this.originalValue = value;
		}
		this.editable = editable;
		rowCount = 0;
		dbTable = null;
		final int dispStartIndex;
		if (uo != null) {
			this.selectedRows = uo.selectedRows;
			dispStartIndex = uo.dispStartIndex;
		} else {
			selectedRows.clear();
			dispStartIndex = 1;
		}
		final Object aValue = value;
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				resetValue(forceSetValue, aValue, dispStartIndex);
			}
		});
	}

	/**
	 * 取原始值
	 * 
	 * @return
	 */
	public Object getOriginalValue() {
		return originalValue;
	}

	/**
	 * 显示的起始行
	 */
	private int dispStartIndex = 1;

	/**
	 * 重设格值
	 * 
	 * @param forceSetValue 强行设置格值
	 * @param aValue        值
	 */
	private synchronized void resetValue(boolean forceSetValue, Object aValue, int dispStartIndex) {
		try {
			initJTable();
			if (!forceSetValue) {
				undo.clear();
				redo.clear();
			}
			refreshValueButton();
			// null格子显示成(null)，只有没打开页面时才不显示
			if (GV.appSheet == null) {
				if (aValue == null) {
					// 不是null的会在resetData前关闭
					if (resetThread != null) {
						resetThread.stopThread();
						try {
							resetThread.join();
						} catch (Exception e) {
						}
					}
					resetThread = null;
					return;
				}
			}
			setColumnModel();
			m_type = getValueType(aValue);
			switch (m_type) {
			case TYPE_TABLE:
				rowCount = initTable((Table) aValue);
				break;
			case TYPE_PMT:
				rowCount = initPmt((Sequence) aValue);
				break;
			case TYPE_RECORD:
				rowCount = initRecord((Record) aValue);
				break;
			case TYPE_SERIES:
				rowCount = initSeries((Sequence) aValue);
				break;
			case TYPE_SERIESPMT:
				rowCount = initSeriesPmt((Sequence) aValue);
				break;
			case TYPE_DB:
				rowCount = initDB((DBObject) aValue);
				break;
			case TYPE_FILE:
				rowCount = initFile((FileObject) aValue);
				break;
			default:
				rowCount = initDefault(aValue);
				break;
			}
		} finally {
			try {
				GVSpl.panelValue.preventChange = true;
				GVSpl.panelValue.sbValue.setMinimum(1);
				GVSpl.panelValue.sbValue.setMaximum(rowCount);
				GVSpl.panelValue.sbValue.setValue(dispStartIndex);
				GVSpl.panelValue.spValue.getHorizontalScrollBar().setValue(1);
				resetData(dispStartIndex);
			} finally {
				GVSpl.panelValue.preventChange = false;
			}
		}
	}

	/**
	 * 初始化表格控件
	 */
	private void initJTable() {
		removeAllRows();
		data.setColumnCount(0);
		data.getDataVector().clear();
	}

	/**
	 * 取值的类型
	 * 
	 * @param value 值
	 * @return
	 */
	private byte getValueType(Object value) {
		if (value instanceof Table) {
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			return TYPE_TABLE;
		} else if (value instanceof Sequence) {
			if (((Sequence) value).isPurePmt()) {
				setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				return TYPE_PMT;
			}
			// 排列（非纯排列）按照普通序列进行显示
			// else if (((Sequence) value).isPmt()) {
			// setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			// return TYPE_SERIESPMT;
			// }
			setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			return TYPE_SERIES;
		} else if (value instanceof Record) {
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			return TYPE_RECORD;
		} else if (value instanceof DBObject) {
			setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			return TYPE_DB;
		} else if (value instanceof FileObject) {
			setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			return TYPE_FILE;
		} else {
			setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			return TYPE_DEFAULT;
		}
	}

	/**
	 * 初始化序表
	 * 
	 * @param table 序表
	 * @return
	 */
	private int initTable(Table table) {
		DataStruct ds = table.dataStruct();
		setTableColumns(ds, table == null ? 0 : table.length(), true);
		setEditStyle(ds, true);
		return table.length();
	}

	/**
	 * 初始化纯排列
	 * 
	 * @param pmt 纯排列
	 * @return
	 */
	private int initPmt(Sequence pmt) {
		DataStruct ds = pmt.dataStruct();
		setTableColumns(ds, pmt == null ? 0 : pmt.length(), true);
		setEditStyle(ds, true);
		return pmt.length();
	}

	/**
	 * 初始化排列
	 * 
	 * @param pmt
	 * @return
	 */
	private int initSeriesPmt(Sequence pmt) {
		DataStruct ds = getFirstDataStruct(pmt);
		setTableColumns(ds, pmt == null ? 0 : pmt.length(), true);
		setEditStyle(ds, true);
		return pmt.length();
	}

	/**
	 * 取第一个数据结构。这里不能用ifn，要找到第一个有结构的记录
	 * 
	 * @return
	 */
	private DataStruct getFirstDataStruct(Sequence pmt) {
		int size = pmt.length();
		DataStruct ds;
		for (int i = 1; i <= size; ++i) {
			Object obj = pmt.get(i);
			if (obj != null && obj instanceof Record) {
				ds = ((Record) obj).dataStruct();
				if (ds != null && ds.getFieldCount() > 0)
					return ds;
			}
		}
		return null;
	}

	/**
	 * 初始化序列
	 * 
	 * @param series
	 * @return
	 */
	private int initSeries(Sequence series) {
		addColumn(TITLE_INDEX);
		addColumn(TITLE_SERIES);
		TableColumn tc;
		tc = getColumn(TITLE_INDEX);
		tc.setCellEditor(getAllPurposeEditor());
		tc.setCellRenderer(new AllPurposeRenderer(true));
		final int INDEX_WIDTH = getIndexColWidth(series == null ? 0 : series.length());
		tc.setMinWidth(INDEX_WIDTH);
		tc.setWidth(INDEX_WIDTH);
		tc.setPreferredWidth(INDEX_WIDTH);
		tc = getColumn(TITLE_SERIES);
		tc.setPreferredWidth(9999);
		tc.setCellEditor(getAllPurposeEditor());
		tc.setCellRenderer(new AllPurposeRenderer(true));
		return series.length();
	}

	/**
	 * 初始化记录
	 * 
	 * @param record 记录
	 * @return
	 */
	private int initRecord(Record record) {
		DataStruct ds = record.dataStruct();
		setTableColumns(ds, 1, false);
		try {
			AppendDataThread.addRecordRow(this, record);
		} catch (Exception ex) {
			GM.showException(ex);
		}
		setEditStyle(ds, false);
		return 1;
	}

	/**
	 * 是否主键
	 * 
	 * @param primaries 主键数组
	 * @param colName   字段名
	 * @return
	 */
	private boolean isPrimary(String[] primaries, String colName) {
		if (primaries == null) {
			return false;
		}
		for (int i = 0; i < primaries.length; i++) {
			if (colName.equals(primaries[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 设置编辑风格
	 * 
	 * @param ds       数据结构
	 * @param hasIndex 是否有序号列
	 */
	private void setEditStyle(DataStruct ds, boolean hasIndex) {
		if (ds == null)
			return;
		TableColumn tc;
		if (hasIndex) {
			tc = getColumn(COL_FIRST);
			tc.setCellRenderer(new AllPurposeRenderer(hasIndex));
		}
		String cols[] = ds.getFieldNames();
		String[] primaries = ds.getPrimary();
		for (int i = 0; i < cols.length; i++) {
			if (hasIndex) {
				tc = this.getColumn(i + 1);
			} else {
				tc = getColumn(i);
			}
			// 主键不能编辑
			boolean isPrimary = isPrimary(primaries, cols[i]);
			boolean editable = !isPrimary;
			tc.setCellEditor(getAllPurposeEditor(editable));

			String format = GM.getColumnFormat(cols[i]);
			if (StringUtils.isValidString(format)) {
				tc.setCellRenderer(new AllPurposeRenderer(format));
			} else {
				tc.setCellRenderer(new AllPurposeRenderer(hasIndex));
			}
		}
	}

	/**
	 * 设置表格的列
	 * 
	 * @param ds    数据结构
	 * @param len   数据长度，用于计算序号列宽度
	 * @param isSeq 是否序列（也包含排列、序表）
	 */
	private void setTableColumns(DataStruct ds, int len, boolean isSeq) {
		if (ds == null)
			return;
		String nNames[] = ds.getFieldNames();
		if (nNames != null) {
			Vector<String> cols = new Vector<String>();
			if (isSeq) {
				cols.add(TITLE_INDEX);
			}
			for (int i = 0; i < nNames.length; i++) {
				cols.add(nNames[i]);
			}
			data.setDataVector(null, cols);
		}
		int cc = getColumnCount();
		TableColumn tc;
		final int INDEX_WIDTH;
		if (isSeq) {
			INDEX_WIDTH = getIndexColWidth(len);
			tc = getColumn(TITLE_INDEX);
			tc.setMinWidth(INDEX_WIDTH);
			tc.setWidth(INDEX_WIDTH);
			tc.setPreferredWidth(INDEX_WIDTH);
		} else {
			INDEX_WIDTH = 0;
		}
		int colWidth = 0;
		for (int i = 0; i < cc; i++) {
			colWidth += getColumn(i).getWidth();
		}
		int width = getParent().getWidth();
		if (colWidth < width && cc > 0) {
			int aveWidth;
			if (isSeq) {
				aveWidth = (width - INDEX_WIDTH) / (cc - 1);
			} else {
				aveWidth = width / cc;
			}
			for (int i = 1; i < cc; i++) {
				tc = getColumn(i);
				tc.setMinWidth(aveWidth);
				tc.setWidth(aveWidth);
				tc.setPreferredWidth(aveWidth);
				tc.setMinWidth(0);
			}
		}
		if (isSeq) {
			int[] pkIndex = ds.getPKIndex();
			for (int i = 1; i < cc; i++) {
				if (isPK(pkIndex, i - 1)) {
					tc = getColumn(i);
					tc.setHeaderRenderer(new PKRenderer());
					int titleWidth = getFontMetrics(getFont()).stringWidth(getColumnName(i));
					colWidth = tc.getWidth();
					final int IMAGE_WIDTH = 35;
					if (titleWidth + IMAGE_WIDTH > colWidth) {
						int newWidth = titleWidth + IMAGE_WIDTH;
						tc.setMinWidth(newWidth);
						tc.setWidth(newWidth);
						tc.setPreferredWidth(newWidth);
						tc.setMinWidth(0);
					}
				}
			}
		}
	}

	/**
	 * 主键列的渲染器
	 */
	class PKRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		public PKRenderer() {
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			c.setBackground(table.getBackground());
			c.setForeground(table.getForeground());
			if (c instanceof JLabel) {
				((JLabel) c).setText((String) value);
				((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
				((JLabel) c).setIcon(GM.getImageIcon(GC.IMAGES_PATH + "key.png"));
			}
			return c;
		}
	}

	/**
	 * 列是否主键
	 * 
	 * @param pkIndex 主键序号
	 * @param index   列序号
	 * @return
	 */
	private boolean isPK(int[] pkIndex, int index) {
		if (pkIndex == null || pkIndex.length == 0)
			return false;
		for (int pk : pkIndex) {
			if (pk == index)
				return true;
		}
		return false;
	}

	/**
	 * 取序号列宽
	 * 
	 * @param len
	 * @return
	 */
	private int getIndexColWidth(int len) {
		if (len <= 9999) {
			return 40;
		}
		if (len <= 999999) {
			return 60;
		}
		return 80;
	}

	/**
	 * 总是输出异常但是不影响显示，特殊处理一下
	 */
	private void setColumnModel() {
		this.setColumnModel(new DefaultTableColumnModel() {
			private static final long serialVersionUID = 1L;

			public TableColumn getColumn(int col) {
				try {
					return super.getColumn(col);
				} catch (Exception ex) {
					return new TableColumn();
				}
			}
		});
	}

	/**
	 * DBInfo对象对应的序表
	 */
	private Table dbTable = null;
	/** 属性名 */
	private final String TITLE_NAME = mm.getMessage("jtablevalue.name");
	/** 属性值 */
	private final String TITLE_PROP = mm.getMessage("jtablevalue.property");

	/**
	 * 初始化DBInfo对象
	 * 
	 * @param db DBInfo对象
	 * @return
	 */
	private int initDB(DBObject db) {
		dbTable = new Table(new String[] { TITLE_NAME, TITLE_PROP });
		addColumn(TITLE_NAME); // 名称
		addColumn(TITLE_PROP); // 属性
		for (int i = 0; i < this.getColumnCount(); i++) {
			setColumnEditable(i, false);
		}
		if (db == null || db.getDbSession() == null) {
			return 0;
		}
		initDBTable(db);
		return dbTable.length();
	}

	/** 数据源名称 */
	private final String DB_NAME = mm.getMessage("jtablevalue.dbname");
	/** 用户名 */
	private final String USER = mm.getMessage("jtablevalue.user");
	/** 密码 */
	private final String PASSWORD = mm.getMessage("jtablevalue.password");
	/** 数据库类型 */
	private final String DB_TYPE = mm.getMessage("jtablevalue.dbtype");
	/** 驱动程序 */
	private final String DRIVER = mm.getMessage("jtablevalue.driver");
	/** 数据源URL */
	private final String URL = mm.getMessage("jtablevalue.url");
	/** 对象名带模式 */
	private final String USE_SCHEMA = mm.getMessage("jtablevalue.useschema");
	/** 对象名带限定符 */
	private final String ADD_TILDE = mm.getMessage("jtablevalue.addtilde");

	/**
	 * 设置DBInfo对象到表格
	 * 
	 * @param db DBInfo对象
	 */
	private void initDBTable(DBObject db) {
		DBInfo info = db.getDbSession().getInfo();
		if (info == null) {
			return;
		}
		dbTable.newLast(new Object[] { DB_NAME, info.getName() });
		if (info instanceof DBConfig) {
			int type = info.getDBType();
			dbTable.newLast(new Object[] { DB_TYPE, DBTypeEx.getDBTypeName(type) });

			DBConfig dc = (DBConfig) info;
			dbTable.newLast(new Object[] { DRIVER, dc.getDriver() });
			dbTable.newLast(new Object[] { URL, dc.getUrl() });
			dbTable.newLast(new Object[] { USER, dc.getUser() });
			String pwd = dc.getPassword();
			dbTable.newLast(new Object[] { PASSWORD, pwd });
			dbTable.newLast(new Object[] { USE_SCHEMA, Boolean.toString(dc.isUseSchema()) });
			dbTable.newLast(new Object[] { ADD_TILDE, Boolean.toString(dc.isAddTilde()) });
		}
	}

	/**
	 * 初始化FileObject对象
	 * 
	 * @param file FileObject对象
	 * @return
	 */
	private int initFile(FileObject file) {
		addColumn(mm.getMessage("jtablevalue.file"));
		return initSingleValue(file);
	}

	/**
	 * 初始化普通值
	 * 
	 * @param value 普通值
	 * @return
	 */
	private int initDefault(Object value) {
		addColumn(mm.getMessage("jtablevalue.value"));
		return initSingleValue(value);
	}

	/**
	 * 初始化单独的值
	 * 
	 * @param value
	 * @return
	 */
	private int initSingleValue(final Object value) {
		TableColumn tc = getColumn(COL_FIRST);
		tc.setCellEditor(getAllPurposeEditor());
		tc.setCellRenderer(new AllPurposeRenderer());
		if (getRowCount() == 0) {
			addRow();
		}
		data.setValueAt(value, 0, COL_FIRST);
		return 1;
	}

	/**
	 * 取多类型值的编辑器
	 * 
	 * @return
	 */
	public AllPurposeEditor getAllPurposeEditor() {
		return getAllPurposeEditor(false);
	}

	/**
	 * 取多类型值的编辑器
	 * 
	 * @param editable 是否可以编辑
	 * @return
	 */
	public AllPurposeEditor getAllPurposeEditor(boolean editable) {
		JTextFieldReadOnly jtf;
		if (editable) {
			jtf = new JTextFieldReadOnly(new String(), 0, editListener);
		} else {
			jtf = new JTextFieldReadOnly();
		}
		return new AllPurposeEditor(jtf, this);
	}

	/**
	 * 显示格值
	 */
	public void dispCellValue() {
		int r = getSelectedRow();
		int c = getSelectedColumn();
		drillValue(r, c);
	}

	/**
	 * 钻取成员值
	 * 
	 * @param row 行号
	 * @param col 列号
	 */
	private void drillValue(int row, int col) {
		if (editable) {
			return;
		}
		JScrollBar sbValue = GVSpl.panelValue.sbValue;
		int scrollVal = Math.max(sbValue.getValue(), 1);
		int realRow = scrollVal + row;
		Object newValue = null;
		switch (m_type) {
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIES:
		case TYPE_SERIESPMT:
			if (!(value instanceof Sequence))
				return;
			Sequence s = (Sequence) value;
			Object temp = s.get(realRow);
			if (temp instanceof Record) {
				Record r = (Record) temp;
				if (r.dataStruct() != null && s.dataStruct() != null && !r.dataStruct().isCompatible(s.dataStruct())) { // 异构排列
					newValue = temp;
				} else if ((TYPE_SERIES == m_type || TYPE_SERIESPMT == m_type)) {
					newValue = temp;
				}
			}
			break;
		default:
			break;
		}
		if (newValue == null) {
			newValue = data.getValueAt(row, col);
			if (newValue == null) {
				return;
			}
		}
		if (newValue.equals(value)) { // 钻取的元素是本身时
			return;
		}
		redo.clear();
		undo.push(getUndoObject());
		value = newValue;
		forceSetValue(value);
	}

	/**
	 * 强行设置值，不考虑锁的状态
	 * 
	 * @param newValue
	 */
	private void forceSetValue(Object newValue) {
		setValue(newValue, editable, true);
	}

	/**
	 * 是否可以撤回
	 * 
	 * @return
	 */
	public boolean canUndo() {
		return !undo.empty() && !editable;
	}

	/**
	 * 是否可以重做
	 * 
	 * @return
	 */
	public boolean canRedo() {
		return !redo.empty() && !editable;
	}

	/**
	 * 撤回
	 */
	public void undo() {
		UndoObject uo = getUndoObject();
		redo.push(uo);
		uo = (UndoObject) undo.pop();
		this.selectedRows = uo.selectedRows;
		setValue(uo.value, editable, true, uo);
	}

	/**
	 * 重做
	 */
	public void redo() {
		UndoObject uo = getUndoObject();
		undo.push(uo);
		uo = (UndoObject) redo.pop();
		this.selectedRows = uo.selectedRows;
		setValue(uo.value, editable, true, uo);
	}

	private UndoObject getUndoObject() {
		UndoObject uo = new UndoObject();
		uo.value = value;
		if (value != null && value instanceof Sequence) {
			uo.dispStartIndex = dispStartIndex;
			uo.selectedRows.addAll(selectedRows);
		} else {
			uo.dispStartIndex = 1;
			if (selectedRows.size() == 1 && selectedRows.getInt(0) == 0) {
				uo.selectedRows.addInt(0);
			}
		}
		return uo;
	}

	class UndoObject {
		Object value;
		int dispStartIndex = 1;
		IntArrayList selectedRows = new IntArrayList();
	}

	/**
	 * 可以绘制统计图
	 * 
	 * @return
	 */
	public boolean canDrawChart() {
		if (canvas != null && canvas instanceof Canvas)
			return true;

		if (value != null) {
			if (value instanceof byte[])
				return true;
			if (value instanceof Table)
				return true;
			if (value instanceof Sequence) {
				Sequence seq = (Sequence) value;
				if (seq.isPurePmt())
					return true;
			}
		}

		return false;
	}

	/**
	 * 是否可以显示长文本
	 * 
	 * @return
	 */
	public boolean canShowText() {
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if (row == -1 || col == -1) {
			return false;
		}
		final Object cellVal = data.getValueAt(row, col);
		return StringUtils.isValidString(cellVal);
	}

	/**
	 * 取复制数据的起始列
	 * 
	 * @return
	 */
	private int getCopyStartCol() {
		boolean isSeq = false;
		switch (m_type) {
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIESPMT:
		case TYPE_SERIES:
			isSeq = true;
			break;
		}
		int startCol = 0;
		if (isSeq) {
			startCol = 1; // 序号
		}
		return startCol;
	}

	/**
	 * 复制列标题
	 */
	public void copyColumnNames() {
		StringBuffer buf = new StringBuffer();
		for (int c = getCopyStartCol(); c < getColumnCount(); c++) {
			if (buf.length() > 0)
				buf.append(",");
			buf.append(getColumnName(c));
		}
		GM.clipBoard(buf.toString());
	}

	/**
	 * 复制数据
	 * 
	 * @return
	 */
	public boolean copyValue() {
		return copyValue(false);
	}

	/**
	 * 复制数据
	 * 
	 * @param copyTitle 是否复制标题
	 * @return
	 */
	public boolean copyValue(boolean copyTitle) {
		IntArrayList selectedRows = this.selectedRows;
		if (selectedRows.isEmpty()) {
			int count = 1;
			if (m_type == TYPE_DB) {
				count = dbTable.length();
			} else if (value instanceof Sequence) {
				count = ((Sequence) value).length();
			}
			for (int i = 0; i < count; i++) {
				selectedRows.add(i);
			}
		}
		int startCol = getCopyStartCol();
		int cc = getColumnCount() - startCol;
		int rowCount = selectedRows.size();
		if (copyTitle) {
			rowCount += 1;
		}
		Matrix matrix;
		CellRect cr = new CellRect(0, (short) 0, selectedRows.size() - 1, (short) (cc - 1));
		Sequence seq = null;
		switch (m_type) {
		case TYPE_DB:
			matrix = new Matrix(rowCount, cc);
			seq = dbTable;
			break;
		case TYPE_TABLE:
		case TYPE_PMT:
		case TYPE_SERIESPMT:
		case TYPE_SERIES:
			matrix = new Matrix(rowCount, cc);
			seq = (Sequence) value;
			break;
		default:
			matrix = new Matrix(selectedRows.size(), cc);
			for (int r = 0; r < selectedRows.size(); r++) {
				for (int c = startCol; c < getColumnCount(); c++) {
					Object value = data.getValueAt(selectedRows.getInt(r), c);
					PgmNormalCell pnc = new PgmNormalCell();
					pnc.setValue(value);
					try {
						pnc.setExpString(Variant.toExportString(value));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					matrix.set(r, c, pnc);
				}
			}
			break;
		}
		if (seq != null) {
			if (copyTitle) {
				for (int i = startCol; i < getColumnCount(); i++) {
					PgmNormalCell pnc = new PgmNormalCell();
					pnc.setExpString(getColumnName(i));
					matrix.set(0, i - startCol, pnc);
				}
			}
			Object rowData;
			List<String> messages = new ArrayList<String>();
			for (int i = 0; i < selectedRows.size(); i++) {
				rowData = seq.get(selectedRows.getInt(i) + 1);
				int row = i;
				if (copyTitle)
					row = i + 1;
				if (rowData instanceof Record) {
					Record rec = (Record) rowData;
					Object[] values = rec.getFieldValues();
					for (int c = 0; c < cc; c++) {
						if (c >= values.length)
							break;
						PgmNormalCell pnc = new PgmNormalCell();
						pnc.setValue(values[c]);
						try {
							pnc.setExpString(Variant.toExportString(values[c]));
						} catch (Exception ex) {
							if (StringUtils.isValidString(ex.getMessage()))
								if (!messages.contains(ex.getMessage()))
									messages.add(ex.getMessage());
						}

						matrix.set(row, c, pnc);
					}
				} else {
					PgmNormalCell pnc = new PgmNormalCell();
					pnc.setValue(rowData);
					try {
						pnc.setExpString(Variant.toExportString(rowData));
					} catch (Exception ex) {
						if (StringUtils.isValidString(ex.getMessage()))
							if (!messages.contains(ex.getMessage()))
								messages.add(ex.getMessage());
					}
					matrix.set(row, 0, pnc);
				}
			}
			if (!messages.isEmpty()) {
				for (int i = 0; i < messages.size(); i++) {
					System.out.println(messages.get(i));
				}
			}
		}
		GV.cellSelection = new CellSelection(matrix, cr, null);
		Clipboard cb = null;
		try {
			cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		} catch (HeadlessException e) {
			cb = null;
		}
		String strCS = GM.getCellSelectionString(matrix, false);
		if (cb != null) {
			cb.setContents(new StringSelection(strCS), null);
		}
		GV.cellSelection.systemClip = strCS;
		return true;
	}

	/**
	 * 粘贴数据
	 */
	public void pasteValue() {
		if (!editable) {
			return;
		}
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if (row == -1 || col == -1) {
			return;
		}
		acceptText();
		CellSelection cs = GV.cellSelection;
		if (cs != null) {
			setMatrix2Table(cs.matrix, row, col);
			acceptText();
			return;
		}
		String clip = GM.clipBoard();
		if (StringUtils.isValidString(clip)) {
			Matrix matrix = GM.string2Matrix(clip);
			setMatrix2Table(matrix, row, col);
			acceptText();
			return;
		}
	}

	/**
	 * 绘制图形
	 */
	public void drawChart() {
		if (!canDrawChart())
			return;
		DialogDisplayChart ddc = null;
		if (canvas != null) {
			ddc = new DialogDisplayChart(canvas);
		} else if (value instanceof byte[]) {
			ddc = new DialogDisplayChart((byte[]) value);
		} else if (value instanceof Table) {
			ddc = new DialogDisplayChart((Table) value);
		} else if (value instanceof Sequence) {
			Sequence seq = (Sequence) value;
			Table t = seq.derive("o");
			ddc = new DialogDisplayChart(t);
		} else {
			return;
		}
		ddc.setVisible(true);
	}

	/**
	 * 查看长文本
	 */
	public void showText() {
		int row = getSelectedRow();
		int col = getSelectedColumn();
		if (row == -1 || col == -1) {
			return;
		}
		final Object cellVal = data.getValueAt(row, col);
		if (!StringUtils.isValidString(cellVal))
			return;
		showText(row, col, cellVal);
	}

	private void showText(int row, int col, Object cellVal) {
		DialogTextEditor dte = new DialogTextEditor(false);
		dte.setText((String) cellVal);
		dte.setVisible(true);
	}

	/**
	 * 设置格子或数据矩阵到表格
	 * 
	 * @param matrix 数据矩阵
	 * @param row    起始行
	 * @param col    起始列
	 */
	private void setMatrix2Table(Matrix matrix, int row, int col) {
		int rowCount = matrix.getRowSize();
		int colCount = matrix.getColSize();
		int endRow = row + rowCount;
		int endCol = Math.min(col + colCount, getColumnCount());
		for (int i = row; i < endRow; i++) {
			if (i > getRowCount() - 1) {
				addRow();
			}
			for (int j = col; j < endCol; j++) {
				Object temp = matrix.get(i - row, j - col);
				if (temp == null) {
					setValueAt(null, i, j);
					continue;
				}
				if (temp instanceof NormalCell) {
					NormalCell nc = (NormalCell) temp;
					if (nc.getValue() != null) {
						setValueAt(nc.getValue(), i, j);
					} else if (StringUtils.isValidString(nc.getExpString())) {
						setValueAt(nc.getExpString(), i, j);
					}
				} else {
					setValueAt(temp, i, j);
				}
			}
		}
	}

	/**
	 * 取单元格值
	 */
	public Object getValueAt(int row, int col) {
		try {
			return super.getValueAt(row, col);
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * 取尺寸大小
	 */
	public Dimension getPreferredSize() {
		try {
			Dimension size = super.getPreferredSize();
			return size;
		} catch (Exception ex) {
			return new Dimension(1, 1);
		}
	}
}
