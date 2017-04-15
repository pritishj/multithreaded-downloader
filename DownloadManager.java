import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;


public class DownloadManager extends JFrame implements Observer {
    
    private JTextField addTextField = new JTextField(30);
    private DownloadsTableModel tableModel = new DownloadsTableModel();
    private JTable table;
    private JButton pauseButton = new JButton("Pause");
    private JButton resumeButton = new JButton("Resume");
    private JButton cancelButton, clearButton;
    private JLabel saveFileLabel = new JLabel();
    private Download selectedDownload;
    private boolean clearing;

    public DownloadManager() {
	
	setTitle("Download Manager");
	setSize(640, 480);
	
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    });
	
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	fileMenu.setMnemonic(KeyEvent.VK_F);
	JMenuItem fileExitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
	fileExitMenuItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    System.exit(0);
		}
	    });
	fileMenu.add(fileExitMenuItem);
	menuBar.add(fileMenu);
	setJMenuBar(menuBar);

        // Set up add panel.
	JPanel addPanel = new JPanel(new BorderLayout());

	JPanel targetPanel = new JPanel(new BorderLayout());
	targetPanel.add(addTextField, BorderLayout.WEST);
	JButton addButton = new JButton("Add Download");
	addButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionAdd();
		}
	    });

	targetPanel.add(addButton, BorderLayout.EAST);

	JPanel destinationPanel = new JPanel(new BorderLayout());
	saveFileLabel.setText("File:");
	destinationPanel.add(saveFileLabel, BorderLayout.WEST);

	JButton saveFileButton = new JButton("Download To");
	saveFileButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionSaveTo();
		}
	    });
	destinationPanel.add(saveFileButton, BorderLayout.EAST);
	addPanel.add(destinationPanel, BorderLayout.NORTH);
	addPanel.add(targetPanel, BorderLayout.SOUTH);

        // Set up Downloads table.

	table = new JTable(tableModel);
	table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
		    tableSelectionChanged();
		}
	    });
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	ProgressRenderer renderer = new ProgressRenderer(0, 100);
	renderer.setStringPainted(true); // show progress text
	table.setDefaultRenderer(JProgressBar.class, renderer);

	table.setRowHeight((int) renderer.getPreferredSize().getHeight());

	JPanel downloadsPanel = new JPanel();
	downloadsPanel.setBorder(BorderFactory.createTitledBorder("Downloads"));
	downloadsPanel.setLayout(new BorderLayout());
	downloadsPanel.add(new JScrollPane(table), BorderLayout.CENTER);

	JPanel buttonsPanel = new JPanel();

	pauseButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionPause();
		}
	    });
	pauseButton.setEnabled(false);
	buttonsPanel.add(pauseButton);

	resumeButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionResume();
		}
	    });
	resumeButton.setEnabled(false);
	buttonsPanel.add(resumeButton);
	cancelButton = new JButton("Cancel");
	cancelButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionCancel();
		}
	    });
	cancelButton.setEnabled(false);
	buttonsPanel.add(cancelButton);
	clearButton = new JButton("Clear");
	clearButton.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    actionClear();
		}
	    });
	clearButton.setEnabled(false);
	buttonsPanel.add(clearButton);

	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(addPanel, BorderLayout.NORTH);
	getContentPane().add(downloadsPanel, BorderLayout.CENTER);
	getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    private void actionSaveTo()
	{

	    JFileChooser jfchooser = new JFileChooser();

	    jfchooser.setApproveButtonText("OK");
	    jfchooser.setDialogTitle("Save To");
	    jfchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

	    int result = jfchooser.showOpenDialog(this);
	    File newZipFile = jfchooser.getSelectedFile();
	    System.out.println("importProfile:" + newZipFile);
	    this.saveFileLabel.setText(newZipFile.getPath());

	}

    private void actionAdd() {
	URL verifiedUrl = verifyUrl(addTextField.getText());
	if (verifiedUrl != null) {
	    tableModel.addDownload(new Download(verifiedUrl, saveFileLabel.getText()));
	    addTextField.setText(""); // reset add text field
	} else {
	    JOptionPane.showMessageDialog(this, "Invalid Download URL", "Error",
					  JOptionPane.ERROR_MESSAGE);
	}
    }

    private URL verifyUrl(String url) {
	if (!url.toLowerCase().matches("(http://|https://).*"))
	    return null;

	URL verifiedUrl = null;
	try {
	    verifiedUrl = new URL(url);
	} catch (Exception e) {
	    return null;
	}

	if (verifiedUrl.getFile().length() < 2)
	    return null;

	return verifiedUrl;
    }

    private void tableSelectionChanged() {
	if (selectedDownload != null)
	    selectedDownload.deleteObserver(DownloadManager.this);

	if (!clearing && table.getSelectedRow() > -1) {
	    selectedDownload = tableModel.getDownload(table.getSelectedRow());
	    selectedDownload.addObserver(DownloadManager.this);
	    updateButtons();
	}
    }

    private void actionPause() {
	selectedDownload.pause();
	updateButtons();
    }

    private void actionResume() {
	selectedDownload.resume();
	updateButtons();
    }

    private void actionCancel() {
	selectedDownload.cancel();
	updateButtons();
    }

    private void actionClear() {
	clearing = true;
	tableModel.clearDownload(table.getSelectedRow());
	clearing = false;
	selectedDownload = null;
	updateButtons();
    }

    private void updateButtons() {
	if (selectedDownload != null) {
	    int status = selectedDownload.getStatus();
	    switch (status) {
	    case Download.DOWNLOADING:
		pauseButton.setEnabled(true);
		resumeButton.setEnabled(false);
		cancelButton.setEnabled(true);
		clearButton.setEnabled(false);
		break;
	    case Download.PAUSED:
		pauseButton.setEnabled(false);
		resumeButton.setEnabled(true);
		cancelButton.setEnabled(true);
		clearButton.setEnabled(false);
		break;
	    case Download.ERROR:
		pauseButton.setEnabled(false);
		resumeButton.setEnabled(true);
		cancelButton.setEnabled(false);
		clearButton.setEnabled(true);
		break;
	    default: // COMPLETE or CANCELLED
		pauseButton.setEnabled(false);
		resumeButton.setEnabled(false);
		cancelButton.setEnabled(false);
		clearButton.setEnabled(true);
	    }
	} else {
	    pauseButton.setEnabled(false);
	    resumeButton.setEnabled(false);
	    cancelButton.setEnabled(false);
	    clearButton.setEnabled(false);
	}
    }

    public void update(Observable o, Object arg) {
// Update buttons if the selected download has changed.
	if (selectedDownload != null && selectedDownload.equals(o))
	    updateButtons();
    }

// Run the Download Manager.
    public static void main(String[] args) {
	DownloadManager manager = new DownloadManager();
	manager.setVisible(true);
    }
}
