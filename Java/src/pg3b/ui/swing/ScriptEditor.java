
package pg3b.ui.swing;

import static com.esotericsoftware.minlog.Log.*;
import static org.fife.ui.rsyntaxtextarea.Token.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.VariableCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import pg3b.ui.Script;
import pg3b.util.UI;

import com.esotericsoftware.minlog.Log;

public class ScriptEditor extends EditorPanel<Script> {
	private int lastCaretPosition;
	private HashMap<File, Integer> fileToPosition = new HashMap();

	private RSyntaxTextArea codeText;
	private RTextScrollPane codeScroll;
	private JButton recordButton;

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
		// provider.addCompletion(new ShorthandCompletion(provider, "main", "int main(int argc, char **argv)"));
		AutoCompletion autoCompletion = new AutoCompletion(provider);
		autoCompletion.setListCellRenderer(new CellRenderer());
		autoCompletion.setShowDescWindow(true);
		autoCompletion.setParameterAssistanceEnabled(true);
		autoCompletion.install(codeText);
	}

	protected void updateFieldsFromItem (Script script) {
		if (script == null)
			codeText.setText("");
		else {
			Integer position = fileToPosition.get(getSelectedItem().getFile());
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
			codeText.setCaretColor(Color.black);
			codeText.setBackground(Color.white);
			codeText.setSelectionColor(new Color(0xb8ddff));
			{
				codeScroll = new RTextScrollPane(codeText);
				getContentPanel().add(
					codeScroll,
					new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0,
						0, 0), 0, 0));
			}
		}
		{
			JPanel panel = new JPanel(new GridLayout(1, 1, 6, 6));
			getContentPanel().add(
				panel,
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6, 6, 6, 0), 0, 0));
			{
				recordButton = new JButton("Record");
				panel.add(recordButton);
			}
		}

		UI.enableWhenModelHasSelection(getSelectionModel(), recordButton, codeText);
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
