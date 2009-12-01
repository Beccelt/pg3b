
package pg3b.tools;

import static java.awt.GridBagConstraints.*;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pg3b.tools.Config.Input;
import pg3b.tools.util.DirectoryMonitor;
import pg3b.tools.util.UI;

public class ConfigurationTab extends JPanel {
	private CardLayout configTabLayout;
	private ConfigCard configCard;
	private InputCard inputCard;

	public ConfigurationTab () {
		setLayout(configTabLayout = new CardLayout());
		add(configCard = new ConfigCard(), "configCard");
		add(inputCard = new InputCard(), "inputCard");
	}

	static private class ConfigCard extends JPanel {
		JList configsList;
		DefaultComboBoxModel configsListModel;
		JTextField configNameText;
		JTable inputsTable;
		DefaultTableModel inputsTableModel;
		JButton newConfigButton, deleteConfigButton, newInputButton, deleteInputButton;
		JTextArea configDescriptionText;

		public ConfigCard () {
			initializeLayout();
			initializeEvents();

			new DirectoryMonitor<Config>(".config") {
				protected Config load (File file) {
					Config config = new Config();
					config.setFile(file);
					return config;
				}

				protected void updated () {
					configsListModel.removeAllElements();
					for (Config config : getItems())
						configsListModel.addElement(config);
				}
			}.scan(new File("config"), 3000);
		}

		private void initializeEvents () {
			configsList.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged (ListSelectionEvent event) {
					if (event.getValueIsAdjusting()) return;
					Config config = (Config)configsList.getSelectedValue();
					if (config == null) config = new Config();
					configNameText.setText(config.getName());
					configDescriptionText.setText(config.getDescription());
					inputsTableModel.setRowCount(0);
					for (Input input : config.getInputs())
						inputsTableModel.addRow(new Object[] {input.getDescription()});
				}
			});
		}

		private void initializeLayout () {
			setLayout(new GridBagLayout());
			{
				JScrollPane scroll = new JScrollPane();
				scroll.setMinimumSize(new Dimension(150, 3));
				scroll.setMaximumSize(new Dimension(150, 3));
				scroll.setPreferredSize(new Dimension(150, 3));
				add(scroll, new GridBagConstraints(1, 1, 1, 2, 0.0, 1.0, CENTER, BOTH, new Insets(6, 6, 0, 6), 0, 0));
				{
					scroll.setViewportView(configsList = new JList());
					configsList.setModel(configsListModel = new DefaultComboBoxModel());
				}
			}
			{
				JScrollPane scroll = new JScrollPane();
				add(scroll, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0, CENTER, BOTH, new Insets(6, 0, 0, 6), 0, 0));
				{
					scroll.setViewportView(inputsTable = new JTable());
					inputsTable.setModel(inputsTableModel = new DefaultTableModel(new String[][] {}, new String[] {"Description",
						"Input", "Action"}));
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				add(panel, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 0, 0, 0), 0, 0));
				{
					panel.add(newConfigButton = new JButton("New"));
				}
				{
					panel.add(deleteConfigButton = new JButton("Delete"));
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 0, 0, 0), 0, 0));
				{
					panel.add(newInputButton = new JButton("New"));
				}
				{
					panel.add(deleteInputButton = new JButton("Delete"));
				}
			}
			{
				JPanel panel = new JPanel(new GridBagLayout());
				add(panel, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
				{
					panel.add(new JLabel("Name:"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(6, 6, 0, 6), 0,
						0));
				}
				{
					panel.add(configNameText = new JTextField(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL,
						new Insets(6, 0, 0, 6), 0, 0));
				}
				{
					panel.add(new JLabel("Description:"), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, NORTHEAST, NONE, new Insets(6,
						6, 0, 6), 0, 0));
				}
				{
					JScrollPane scroll = new JScrollPane();
					panel.add(scroll, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
					scroll.setMinimumSize(new Dimension(3, 50));
					scroll.setMaximumSize(new Dimension(3, 50));
					scroll.setPreferredSize(new Dimension(3, 50));
					{
						scroll.setViewportView(configDescriptionText = new JTextArea());
					}
				}
			}

			UI.enableWhenListHasSelection(configsList, deleteConfigButton, inputsTable, newInputButton, deleteInputButton,
				configNameText, configDescriptionText);
		}
	}

	static private class InputCard extends JPanel {
		private JPanel inputPanel;
		private JRadioButton inputPg3bRadio, inputScriptRadio;
		private JButton inputSaveButton, inputCancelButton;
		private JTextField inputText, inputDescriptionText;
		private JComboBox inputPg3bCombo, inputScriptCombo;
		private DefaultComboBoxModel inputScriptComboModel, inputPg3bComboModel;

		public InputCard () {
			setLayout(new GridBagLayout());
			add(inputPanel = new JPanel(new GridBagLayout()), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, CENTER, NONE, new Insets(
				0, 0, 0, 0), 0, 0));
			inputPanel.setBorder(BorderFactory.createTitledBorder("New Input"));
			{
				inputPanel.add(new JLabel("Description:"), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(3, 6,
					6, 0), 0, 0));
			}
			{
				inputPanel.add(inputDescriptionText = new JTextField(), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, CENTER,
					HORIZONTAL, new Insets(0, 6, 6, 6), 0, 0));
			}
			{
				inputPanel.add(new JLabel("Input:"), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, EAST, NONE, new Insets(3, 6, 6, 0),
					0, 0));
			}
			{
				inputPanel.add(new JLabel("Action:"), new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, NORTHEAST, NONE, new Insets(4, 6,
					6, 0), 0, 0));
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, WEST, NONE, new Insets(0, 0, 6, 6), 0, 0));
				{
					panel.add(inputScriptRadio = new JRadioButton("Script"));
				}
				{
					panel.add(inputScriptCombo = new JComboBox());
					inputScriptCombo.setModel(inputScriptComboModel = new DefaultComboBoxModel(new String[] {"Item One", "Item Two"}));
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, WEST, NONE, new Insets(0, 0, 6, 6), 0, 0));
				{
					panel.add(inputPg3bRadio = new JRadioButton("PG3B"));
				}
				{
					panel.add(inputPg3bCombo = new JComboBox());
					inputPg3bCombo.setModel(inputPg3bComboModel = new DefaultComboBoxModel(new String[] {"Item One", "Item Two"}));
				}
			}
			{
				JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
				inputPanel.add(panel, new GridBagConstraints(1, 4, 2, 1, 0.0, 0.0, EAST, NONE, new Insets(0, 0, 0, 0), 0, 0));
				{
					panel.add(inputCancelButton = new JButton("Cancel"));
				}
				{
					panel.add(inputSaveButton = new JButton("Save"));
				}
			}
			{
				inputPanel.add(inputText = new JTextField(), new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, CENTER, HORIZONTAL,
					new Insets(0, 6, 6, 6), 0, 0));
				inputText.setEditable(false);
				inputText.setBackground(new Color(192, 192, 192));
				inputText.setFocusable(false);
			}
			{
				JPanel spacer = new JPanel();
				inputPanel.add(spacer, new GridBagConstraints(1, 5, 2, 1, 0.0, 0.0, CENTER, NONE, new Insets(0, 0, 0, 0), 0, 0));
				spacer.setMinimumSize(new Dimension(350, 0));
				spacer.setMaximumSize(new Dimension(350, 0));
				spacer.setPreferredSize(new Dimension(350, 0));
			}
		}
	}
}