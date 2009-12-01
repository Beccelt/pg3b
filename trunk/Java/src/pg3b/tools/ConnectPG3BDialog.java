
package pg3b.tools;

import static com.esotericsoftware.minlog.Log.*;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pg3b.PG3B;
import pg3b.tools.util.LoaderDialog;
import pg3b.tools.util.UI;

public class ConnectPG3BDialog extends JDialog {
	PG3BTool owner;
	JList portList;
	DefaultComboBoxModel portListModel;
	PG3B pg3b;
	private JButton connectButton, cancelButton;

	public ConnectPG3BDialog (final PG3BTool owner) {
		super(owner, "Connect to PG3b", true);
		this.owner = owner;

		initializeLayout();
		initializeEvents();

		Enumeration ports = CommPortIdentifier.getPortIdentifiers();
		while (ports.hasMoreElements()) {
			CommPortIdentifier identifier = (CommPortIdentifier)ports.nextElement();
			switch (identifier.getPortType()) {
			case CommPortIdentifier.PORT_SERIAL:
				String name = identifier.getName();
				try {
					identifier.open("PG3B", 50).close();
					portListModel.addElement(name);
				} catch (PortInUseException ex) {
					if (DEBUG) debug("Port is in use: " + name);
				} catch (Exception ex) {
					if (WARN) warn("Failed to open port: " + name, ex);
				}
			}
		}
	}

	private void initializeEvents () {
		connectButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				new LoaderDialog("Connecting to hardware") {
					public void load () throws Exception {
						setMessage("Opening PG3B...");
						owner.setPg3b(new PG3B((String)portList.getSelectedValue()));
					}

					public void complete () {
						if (!failed()) {
							ConnectPG3BDialog.this.dispose();
							return;
						}
						UI.errorDialog(ConnectPG3BDialog.this, "Connect Error",
							"An error occurred while attempting to connect to the PG3B.");
					}
				}.start("Pg3bConnect");
			}
		});

		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				dispose();
			}
		});

		UI.enableWhenListHasSelection(portList, connectButton);
	}

	private void initializeLayout () {
		setSize(320, 250);
		setLocationRelativeTo(getOwner());

		getContentPane().setLayout(new GridBagLayout());
		{
			JScrollPane scroll = new JScrollPane();
			getContentPane().add(
				scroll,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
			{
				portList = new JList();
				scroll.setViewportView(portList);
				portListModel = new DefaultComboBoxModel();
				portList.setModel(portListModel);
			}
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			getContentPane().add(
				panel,
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 0, 0, 0), 0, 0));
			{
				cancelButton = new JButton("Cancel");
				panel.add(cancelButton);
			}
			{
				connectButton = new JButton("Connect");
				panel.add(connectButton);
			}
		}
		{
			JLabel label = new JLabel("<html>Please choose a port to connect to the PG3B.");
			getContentPane().add(
				label,
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 0,
					6), 0, 0));
		}
	}
}
