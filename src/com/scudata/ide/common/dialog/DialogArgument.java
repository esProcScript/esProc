package com.scudata.ide.common.dialog;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.Matrix;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.common.swing.JTableEx;
import com.scudata.ide.common.swing.VFlowLayout;

/**
 * 输入参数对话框
 */
public class DialogArgument extends DialogMaxmizable {
	private static final long serialVersionUID = 1L;
	/**
	 * Common资源管理器
	 */
	private MessageManager mm = IdeCommonMessage.get();

	/** 序号列 */
	private final byte COL_INDEX = 0;
	/** 名称列 */
	private final byte COL_NAME = 1;
	/** 值列 */
	private final byte COL_VALUE = 2;
	/** 备注列 */
	private final byte COL_REMARK = 3;

	/** 序号列标题 */
	private final String TITLE_INDEX = mm.getMessage("dialogargument.index");
	/** 名称列标题 */
	private final String TITLE_NAME = mm.getMessage("dialogargument.name");
	/** 值列标题 */
	private final String TITLE_VALUE = mm.getMessage("dialogargument.value");
	/** 备注列标题 */
	private final String TITLE_REMARK = mm.getMessage("dialogparameter.remark");

	/**
	 * 参数表控件。序号,名称,值
	 */
	public JTableEx paraTable = new JTableEx(TITLE_INDEX + "," + TITLE_NAME
			+ "," + TITLE_VALUE + "," + TITLE_REMARK) {
		private static final long serialVersionUID = 1L;

		public void doubleClicked(int xpos, int ypos, int row, int col,
				MouseEvent e) {
			if (col != COL_INDEX) {
				GM.dialogEditTableText(paraTable, row, col);
			}
		}

		public void setValueAt(Object aValue, int row, int column) {
			if (!isItemDataChanged(row, column, aValue)) {
				return;
			}
			super.data.setValueAt(aValue, row, column);
		}
	};

	/**
	 * 确认按钮
	 */
	private JButton jBOK = new JButton();
	/**
	 * 取消按钮
	 */
	private JButton jBCancel = new JButton();
	/**
	 * 增加按钮
	 */
	private JButton jBAdd = new JButton();
	/**
	 * 删除按钮
	 */
	private JButton jBDel = new JButton();
	/**
	 * 上移按钮
	 */
	private JButton jBUp = new JButton();
	/**
	 * 下移按钮
	 */
	private JButton jBDown = new JButton();
	/**
	 * 全选按钮
	 */
	private JButton buttonAll = new JButton(mm.getMessage("button.selectall"));
	/**
	 * 复制按钮
	 */
	private JButton buttonCopy = new JButton(
			mm.getMessage("dialogparameter.copy"));
	/**
	 * 粘贴按钮
	 */
	private JButton buttonPaste = new JButton(mm.getMessage("button.paste"));
	/**
	 * 每次运行前设置参数
	 */
	private JCheckBox jcbUserChange = new JCheckBox(
			mm.getMessage("dialogparameter.setbeforerun"));
	/**
	 * 窗口的关闭动作
	 */
	private int m_option = JOptionPane.CLOSED_OPTION;
	/**
	 * 参数列表
	 */
	private ParamList pl;

	/**
	 * 构造函数
	 */
	public DialogArgument() {
		super(GV.appFrame, "参数编辑", true);
		try {
			initUI();
			init();
			resetLangText();
			setSize(450, 350);
			GM.setDialogDefaultButton(this, jBOK, jBCancel);
			this.setResizable(true);
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * 初始化
	 */
	private void init() {
		paraTable.setIndexCol(COL_INDEX);
		paraTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		paraTable.setRowHeight(20);

		paraTable.setColumnWidth(COL_NAME, 100);
		paraTable.setClickCountToStart(1);

	}

	/**
	 * 返回窗口关闭动作
	 * 
	 * @return
	 */
	public int getOption() {
		return m_option;
	}

	/**
	 * 设置参数列表
	 * 
	 * @param pl
	 *            参数列表
	 */
	public void setParameter(ParamList pl) {
		if (pl == null) {
			return;
		}
		this.pl = pl;
		jcbUserChange.setSelected(pl.isUserChangeable());
		ParamList argList = new ParamList();
		pl.getAllVarParams(argList);
		if (argList.count() == 0)
			pl.getAllArguments(argList);
		Param p;
		for (int i = 0; i < argList.count(); i++) {
			p = argList.get(i);
			if (p == null) {
				continue;
			}
			int row = paraTable.addRow();
			paraTable.data.setValueAt(p.getName(), row, COL_NAME);
			paraTable.data.setValueAt(p.getEditValue(), row, COL_VALUE);
			paraTable.data.setValueAt(p.getRemark(), row, COL_REMARK);
		}
		paraTable.resetIndex();
	}

	/**
	 * 取参数列表
	 * 
	 * @return 参数列表
	 */
	public ParamList getParameter() {
		if (paraTable.getRowCount() < 1) {
			return null;
		}
		ParamList plist = new ParamList();
		ParamList otherList = new ParamList();
		if (pl != null) {
			pl.getAllConsts(otherList);
		}
		plist.setUserChangeable(jcbUserChange.isSelected());
		Object o;
		for (int i = 0; i < paraTable.getRowCount(); i++) {
			String name = (String) paraTable.getValueAt(i, COL_NAME);
			if (!StringUtils.isValidString(name)) {
				continue;
			}
			Param v = new Param();
			v.setKind(Param.VAR);
			v.setName(name);
			o = paraTable.data.getValueAt(i, COL_VALUE);
			Object editValue = o;
			if (!StringUtils.isValidString(o))
				editValue = null;
			v.setEditValue(editValue);
			if (editValue == null) {
				v.setValue(null);
			} else {
				v.setValue(PgmNormalCell.parseConstValue((String) editValue));
			}
			o = paraTable.data.getValueAt(i, COL_REMARK);
			if (!StringUtils.isValidString(o)) {
				v.setRemark(null);
			} else {
				v.setRemark((String) o);
			}
			plist.add(v);
		}
		int count = otherList.count();
		for (int i = 0; i < count; i++) {
			plist.add(otherList.get(i));
		}
		return plist;
	}

	/**
	 * 根据语言设置显示文本
	 */
	private void resetLangText() {
		setTitle(mm.getMessage("dialogparameter.title"));
		jBOK.setText(mm.getMessage("button.ok"));
		jBCancel.setText(mm.getMessage("button.cancel"));
		jBAdd.setText(mm.getMessage("button.add"));
		jBDel.setText(mm.getMessage("button.delete"));
		jBUp.setText(mm.getMessage("button.shiftup"));
		jBDown.setText(mm.getMessage("button.shiftdown"));
	}

	/**
	 * 初始化控件
	 * 
	 * @throws Exception
	 */
	private void initUI() throws Exception {
		this.addWindowListener(new DialogArgument_this_windowAdapter(this));
		this.getContentPane().setLayout(new BorderLayout());
		JPanel jPanel1 = new JPanel();
		jPanel1.setLayout(new VFlowLayout());
		jBOK.setText("确定(O)");
		jBOK.addActionListener(new DialogArgument_jBOK_actionAdapter(this));
		jBOK.setMnemonic('O');
		jBCancel.setMnemonic('C');
		jBCancel.setText("取消(C)");
		jBCancel.addFocusListener(new DialogArgument_jBCancel_focusAdapter(this));
		jBCancel.addActionListener(new DialogArgument_jBCancel_actionAdapter(
				this));
		jBAdd.setAlignmentX((float) 0.0);
		jBAdd.setAlignmentY((float) 5.0);
		jBAdd.setMnemonic('A');
		jBAdd.setText("增加(A)");
		jBAdd.addActionListener(new DialogArgument_jBAdd_actionAdapter(this));
		jBDel.setMnemonic('D');
		jBDel.setText("删除(D)");
		jBDel.addActionListener(new DialogArgument_jBDel_actionAdapter(this));
		jBUp.setActionCommand("");
		jBUp.setMnemonic('U');
		jBUp.setText("上移(U)");
		jBUp.addActionListener(new DialogArgument_jBUp_actionAdapter(this));
		jBDown.setToolTipText("");
		jBDown.setMnemonic('W');
		jBDown.setText("下移(W)");
		jBDown.addActionListener(new DialogArgument_jBDown_actionAdapter(this));
		buttonAll.setMnemonic('A');
		buttonCopy.setMnemonic('X');
		buttonPaste.setMnemonic('P');
		buttonAll.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				paraTable.selectAll();
			}

		});
		buttonCopy.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				int[] rows = paraTable.getSelectedRows();
				if (rows == null || rows.length == 0) {
					JOptionPane.showMessageDialog(GV.appFrame,
							mm.getMessage("dialogparameter.selectrow"));
					return;
				}
				paraTable.acceptText();
				StringBuffer buf = new StringBuffer();
				String rowStr;
				for (int i = 0; i < rows.length; i++) {
					if (i != 0)
						buf.append('\n');
					rowStr = paraTable.getRowData(rows[i]);
					buf.append(rowStr == null ? "" : rowStr);
				}
				GM.clipBoard(buf.toString());
			}

		});
		buttonPaste.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String str = GM.clipBoard();
				if (!StringUtils.isValidString(str)) {
					JOptionPane.showMessageDialog(GV.appFrame,
							mm.getMessage("dialogparameter.copyrow"));
					return;
				}
				try {
					paraTable.acceptText();
					Matrix m = GM.string2Matrix(str, false);
					if (m.getColSize() != paraTable.getColumnCount()) {
						JOptionPane.showMessageDialog(GV.appFrame,
								mm.getMessage("dialogparameter.copyrow"));
						return;
					}
					int count = m.getRowSize();
					for (int i = 0; i < count; i++) {
						paraTable.addRow(m.getRow(i));
					}
				} catch (Exception ex) {
					GM.showException(ex);
				}
			}

		});
		JLabel jLabel1 = new JLabel();
		jLabel1.setText(" ");
		jPanel1.add(jBOK, null);
		jPanel1.add(jBCancel, null);
		jPanel1.add(jLabel1, null);
		jPanel1.add(jBAdd, null);
		jPanel1.add(jBDel, null);
		jPanel1.add(jBUp, null);
		jPanel1.add(jBDown, null);
		jPanel1.add(new JLabel(), null);
		jPanel1.add(buttonAll, null);
		jPanel1.add(buttonCopy, null);
		jPanel1.add(buttonPaste, null);
		JPanel panelMain = new JPanel();
		panelMain.setLayout(new GridBagLayout());
		panelMain.add(jcbUserChange, GM.getGBC(1, 1, true));
		panelMain.add(new JScrollPane(paraTable), GM.getGBC(2, 1, true, true));
		this.getContentPane().add(panelMain, BorderLayout.CENTER);
		this.getContentPane().add(jPanel1, BorderLayout.EAST);
	}

	/**
	 * 删除命令
	 * 
	 * @param e
	 */
	void jBDel_actionPerformed(ActionEvent e) {
		paraTable.deleteSelectedRows();
	}

	/**
	 * 增加命令
	 * 
	 * @param e
	 */
	void jBAdd_actionPerformed(ActionEvent e) {
		String name = GM.getTableUniqueName(paraTable, COL_NAME, "arg");
		int r = paraTable.addRow();
		paraTable.clearSelection();

		paraTable.selectRow(r);
		paraTable.data.setValueAt(name, r, COL_NAME);
	}

	/**
	 * 确定命令
	 * 
	 * @param e
	 */
	void jBOK_actionPerformed(ActionEvent e) {
		if (!paraTable.verifyColumnData(COL_NAME, TITLE_NAME)) {
			return;
		}
		GM.setWindowDimension(this);
		m_option = JOptionPane.OK_OPTION;
		dispose();
	}

	/**
	 * 取消命令
	 * 
	 * @param e
	 */
	void jBCancel_actionPerformed(ActionEvent e) {
		GM.setWindowDimension(this);
		m_option = JOptionPane.CANCEL_OPTION;
		dispose();
	}

	/**
	 * 上移命令
	 * 
	 * @param e
	 */
	void jBUp_actionPerformed(ActionEvent e) {
		paraTable.shiftRowUp(-1);
	}

	/**
	 * 下移命令
	 * 
	 * @param e
	 */
	void jBDown_actionPerformed(ActionEvent e) {
		paraTable.shiftRowDown(-1);
	}

	/**
	 * 窗口关闭命令
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		jBCancel_actionPerformed(null);
	}

	/**
	 * 取消命令
	 * 
	 * @param e
	 */
	void jBCancel_focusGained(FocusEvent e) {
		jBCancel.requestFocus();
	}
}

class DialogArgument_jBDel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBDel_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDel_actionPerformed(e);
	}
}

class DialogArgument_jBAdd_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBAdd_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBAdd_actionPerformed(e);
	}
}

class DialogArgument_jBOK_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBOK_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBOK_actionPerformed(e);
	}
}

class DialogArgument_jBCancel_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBCancel_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBCancel_actionPerformed(e);
	}
}

class DialogArgument_jBUp_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBUp_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBUp_actionPerformed(e);
	}
}

class DialogArgument_jBDown_actionAdapter implements
		java.awt.event.ActionListener {
	DialogArgument adaptee;

	DialogArgument_jBDown_actionAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void actionPerformed(ActionEvent e) {
		adaptee.jBDown_actionPerformed(e);
	}
}

class DialogArgument_this_windowAdapter extends java.awt.event.WindowAdapter {
	DialogArgument adaptee;

	DialogArgument_this_windowAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}

class DialogArgument_jBCancel_focusAdapter extends java.awt.event.FocusAdapter {
	DialogArgument adaptee;

	DialogArgument_jBCancel_focusAdapter(DialogArgument adaptee) {
		this.adaptee = adaptee;
	}

	public void focusGained(FocusEvent e) {
		adaptee.jBCancel_focusGained(e);
	}
}
