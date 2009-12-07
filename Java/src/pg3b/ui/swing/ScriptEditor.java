
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;
import static org.fife.ui.rsyntaxtextarea.Token.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaHighlighter;
import org.fife.ui.rsyntaxtextarea.SquiggleUnderlineHighlightPainter;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import pg3b.ui.Config;
import pg3b.ui.Script;
import pg3b.ui.ScriptAction;
import pg3b.ui.Trigger;
import pg3b.util.JMultilineTooltip;
import pg3b.util.UI;
import pnuts.lang.Context;
import pnuts.lang.ParseException;
import pnuts.lang.Pnuts;
import pnuts.lang.PnutsException;

import com.esotericsoftware.minlog.Log;

public class ScriptEditor extends EditorPanel<Script> {
	private int lastCaretPosition;
	private HashMap<File, Integer> fileToPosition = new HashMap();
	private TimerTask compileTask;

	private RSyntaxTextArea codeText;
	private RTextScrollPane codeScroll;
	private RSyntaxTextAreaHighlighter highlighter;
	private SquiggleUnderlineHighlightPainter errorPainter = new SquiggleUnderlineHighlightPainter(Color.red);
	private JButton executeButton, recordButton;
	private JLabel errorLabel;

	public ScriptEditor (PG3BUI owner) {
		super(owner, Script.class, new File("scripts"), ".script");

		initializeLayout();
		initializeEvents();

		setFontSize(10f);

		DefaultCompletionProvider provider = new DefaultCompletionProvider();
		try {
			provider.loadFromXML(getClass().getResourceAsStream("/completion.xml"));
		} catch (IOException ex) {
			if (Log.WARN) warn("Error loading autocompletion.", ex);
		}
		provider.addCompletion(new ShorthandCompletion(provider, "A", "pg3b.set(\"a\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "B", "pg3b.set(\"b\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "X", "pg3b.set(\"x\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "Y", "pg3b.set(\"y\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "U", "pg3b.set(\"up\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "D", "pg3b.set(\"down\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "L", "pg3b.set(\"left\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "R", "pg3b.set(\"right\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "RT", "pg3b.set(\"rightTrigger\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "LT", "pg3b.set(\"leftTrigger\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "RS", "pg3b.set(\"rightStick\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "LS", "pg3b.set(\"leftStick\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "RSX", "pg3b.set(\"rightStickX\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "RSY", "pg3b.set(\"rightStickY\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "RSXY", "pg3b.set(\"rightStick\", 1, 1)"));
		provider.addCompletion(new ShorthandCompletion(provider, "LSX", "pg3b.set(\"leftStickX\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "LSY", "pg3b.set(\"leftStickY\", getPayload())"));
		provider.addCompletion(new ShorthandCompletion(provider, "LSXY", "pg3b.set(\"leftStick\", 1, 1)"));
		AutoCompletion autoCompletion = new AutoCompletion(provider);
		autoCompletion.setListCellRenderer(new CellRenderer());
		autoCompletion.setShowDescWindow(true);
		autoCompletion.setParameterAssistanceEnabled(true);
		autoCompletion.setDescriptionWindowSize(300, 300);
		autoCompletion.setAutoCompleteSingleChoices(false);
		autoCompletion.install(codeText);
	}

	protected void updateFieldsFromItem (Script script) {
		if (script == null)
			codeText.setText("");
		else {
			Integer position = fileToPosition.get(script.getFile());
			codeText.setText(script.getCode());
			try {
				codeText.setCaretPosition(position == null ? 0 : position);
			} catch (Exception ignored) {
			}
		}
	}

	protected void updateItemFromFields (Script script) {
		script.setCode(codeText.getText());
	}

	protected void clearItemSpecificState () {
		lastCaretPosition = 0;
		codeText.discardAllEdits();
	}

	protected void itemRenamed (Script oldScript, Script newScript) {
		// Update any ScriptActions that reference the old item.
		ConfigEditor configEditor = owner.getConfigTab().getConfigEditor();
		Config selectedConfig = configEditor.getSelectedItem();
		for (Config config : configEditor.getItems()) {
			boolean needsSave = false;
			for (Trigger trigger : config.getTriggers()) {
				if (trigger.getAction() instanceof ScriptAction) {
					ScriptAction action = (ScriptAction)trigger.getAction();
					if (action.getScriptName().equals(oldScript.getName())) {
						action.setScriptName(newScript.getName());
						needsSave = true;
					}
				}
			}
			if (needsSave) configEditor.saveItem(config, true);
		}
		configEditor.setSelectedItem(selectedConfig);
		System.out.println(selectedConfig);
	}

	private void initializeEvents () {
		recordButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				UI.errorDialog(ScriptEditor.this, "Error", "Not implemented yet.");
			}
		});

		codeText.addFocusListener(new FocusAdapter() {
			public void focusLost (FocusEvent event) {
				saveItem(false);
			}
		});

		codeText.addCaretListener(new CaretListener() {
			public void caretUpdate (CaretEvent event) {
				if (codeText.getText().length() > 0) fileToPosition.put(getSelectedItem().getFile(), event.getDot());
			}
		});

		codeText.getDocument().addDocumentListener(new DocumentListener() {
			public void removeUpdate (DocumentEvent event) {
				changedUpdate(event);
			}

			public void insertUpdate (DocumentEvent event) {
				changedUpdate(event);
			}

			public void changedUpdate (DocumentEvent event) {
				if (compileTask != null) compileTask.cancel();

				UI.timer.schedule(compileTask = new TimerTask() {
					public void run () {
						EventQueue.invokeLater(new Runnable() {
							public void run () {
								Caret caret = codeText.getCaret();
								int dot = caret.getDot();
								int mark = caret.getMark();
								highlighter.removeAllHighlights();
								if (getSelectedItem() == null) return;
								try {
									Pnuts.parse(codeText.getText());
									errorLabel.setText("");
								} catch (ParseException ex) {
									if (DEBUG) debug("Error during script compilation.", ex);
									highlightError(ex.getMessage(), ex.getErrorLine(), ex.getErrorColumn() - 1);
								}
								caret.setDot(mark);
								caret.moveDot(dot);
							}
						});
					}
				}, 1000);
			}
		});

		executeButton.addActionListener(new ActionListener() {
			public void actionPerformed (ActionEvent event) {
				errorLabel.setForeground(Color.black);
				errorLabel.setText("Executing script...");
				new Thread("Execute") {
					public void run () {
						try {
							Context context = new ScriptAction("<temp>").getContext(null, 1);
							Pnuts.load(new StringReader(codeText.getText()), context);
							EventQueue.invokeLater(new Runnable() {
								public void run () {
									errorLabel.setForeground(Color.black);
									errorLabel.setText("Script completed successfully.");
								}
							});
						} catch (final PnutsException ex) {
							if (DEBUG) debug("Error during script execution.", ex);
							final String message;
							if (ex.getThrowable() != null)
								message = ex.getThrowable().getClass().getSimpleName() + ": " + ex.getThrowable().getMessage();
							else
								message = ex.getMessage();
							EventQueue.invokeLater(new Runnable() {
								public void run () {
									highlightError(message, ex.getLine(), ex.getColumn());
								}
							});
						}
					}
				}.start();
			}
		});

		codeText.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK), "execute");
		codeText.getActionMap().put("execute", new AbstractAction() {
			public void actionPerformed (ActionEvent event) {
				executeButton.doClick();
			}
		});
	}

	private void highlightError (String message, int line, int column) {
		errorLabel.setForeground(Color.red);
		errorLabel.setText(message);
		errorLabel.setToolTipText(message);
		try {
			int start = codeText.getLineStartOffset(line - 1) + column;
			int end = codeText.getLineEndOffset(line - 1);
			highlighter.addHighlight(start, end, errorPainter);
		} catch (BadLocationException ignored) {
		}
	}

	private void setFontSize (float size) {
		try {
			Font mono = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VeraMono.ttf")).deriveFont(size);
			Font monoBold = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/VeraMonoBold.ttf"))
				.deriveFont(size);
			Font plain = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Vera.ttf")).deriveFont(size);

			Style black = new Style(new Color(0, 0, 0), null);
			Style blackBold = new Style(new Color(0, 0, 0), null, monoBold);
			Style greenPlain = new Style(new Color(0, 164, 82), null, plain);
			Style darkBlueBold = new Style(new Color(0, 0, 128), null, monoBold);
			Style darkBlue = new Style(new Color(0, 0, 128), null);
			Style blue = new Style(new Color(0, 0, 255), null);
			Style red = new Style(new Color(255, 0, 0), null);
			Style darkGreen = new Style(new Color(64, 128, 128), null);

			Style[] styles = new Style[Token.NUM_TOKEN_TYPES];
			styles[COMMENT_DOCUMENTATION] = greenPlain;
			styles[COMMENT_MULTILINE] = greenPlain;
			styles[COMMENT_EOL] = greenPlain;
			styles[RESERVED_WORD] = darkBlueBold;
			styles[FUNCTION] = darkBlueBold;
			styles[LITERAL_BOOLEAN] = darkBlueBold;
			styles[LITERAL_NUMBER_DECIMAL_INT] = blue;
			styles[LITERAL_NUMBER_FLOAT] = blue;
			styles[LITERAL_NUMBER_HEXADECIMAL] = blue;
			styles[LITERAL_STRING_DOUBLE_QUOTE] = darkGreen;
			styles[LITERAL_CHAR] = darkGreen;
			styles[LITERAL_BACKQUOTE] = darkGreen;
			styles[DATA_TYPE] = darkBlueBold;
			styles[VARIABLE] = darkBlue;
			styles[IDENTIFIER] = black;
			styles[WHITESPACE] = black;
			styles[SEPARATOR] = blue;
			styles[OPERATOR] = blue;
			styles[PREPROCESSOR] = blue;
			styles[MARKUP_TAG_DELIMITER] = black;
			styles[MARKUP_TAG_NAME] = black;
			styles[MARKUP_TAG_ATTRIBUTE] = black;
			styles[ERROR_IDENTIFIER] = red;
			styles[ERROR_NUMBER_FORMAT] = red;
			styles[ERROR_STRING_DOUBLE] = red;
			styles[ERROR_CHAR] = red;

			SyntaxScheme scheme = new SyntaxScheme(false);
			scheme.styles = styles;
			codeText.setSyntaxScheme(scheme);
			codeText.setFont(mono);
			codeScroll.getGutter().setLineNumberFont(mono);
		} catch (Exception ex) {
			if (WARN) warn("Error setting fonts.", ex);
		}
	}

	private void initializeLayout () {
		{
			codeText = new RSyntaxTextArea();
			codeText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			codeText.setHighlightCurrentLine(false);
			codeText.setCloseCurlyBraces(false);
			codeText.setCaretColor(Color.black);
			codeText.setBackground(Color.white);
			codeText.setSelectionColor(new Color(0xb8ddff));
			highlighter = new RSyntaxTextAreaHighlighter();
			codeText.setHighlighter(highlighter);
			{
				codeScroll = new RTextScrollPane(codeText);
				getContentPanel().add(
					codeScroll,
					new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
						0, 0), 0, 0));
			}
		}
		{
			errorLabel = new JLabel() {
				public JToolTip createToolTip () {
					return new JMultilineTooltip(640);
				}
			};
			getContentPanel().add(
				errorLabel,
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(0, 6, 6, 0), 0, 0));
		}
		{
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
			getContentPanel().add(
				panel,
				new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(0, 6, 0, 0), 0, 0));
			{
				recordButton = new JButton("Record");
				// panel.add(recordButton);
			}
			{
				executeButton = new JButton("Execute");
				panel.add(executeButton);
			}
		}

		UI.enableWhenModelHasSelection(getSelectionModel(), codeText, recordButton, executeButton);
	}

	static class CellRenderer extends CompletionCellRenderer {
		private Icon variableIcon, functionIcon, emptyIcon;

		public CellRenderer () {
			variableIcon = new ImageIcon(getClass().getResource("/variable.png"));
			functionIcon = new ImageIcon(getClass().getResource("/function.png"));
			emptyIcon = new EmptyIcon(16);
		}

		protected void prepareForOtherCompletion (JList list, Completion c, int index, boolean selected, boolean hasFocus) {
			super.prepareForOtherCompletion(list, c, index, selected, hasFocus);
			setIcon(emptyIcon);
		}

		protected void prepareForVariableCompletion (JList list, VariableCompletion vc, int index, boolean selected,
			boolean hasFocus) {
			super.prepareForVariableCompletion(list, vc, index, selected, hasFocus);
			setIcon(variableIcon);
		}

		protected void prepareForFunctionCompletion (JList list, FunctionCompletion fc, int index, boolean selected,
			boolean hasFocus) {
			super.prepareForFunctionCompletion(list, fc, index, selected, hasFocus);
			setIcon(functionIcon);
		}
	}

	static class EmptyIcon implements Icon, Serializable {
		private int size;

		public EmptyIcon (int size) {
			this.size = size;
		}

		public int getIconHeight () {
			return size;
		}

		public int getIconWidth () {
			return size;
		}

		public void paintIcon (Component c, Graphics g, int x, int y) {
		}
	}
}