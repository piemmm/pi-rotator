package org.prowl.pirotator.ui.ansi;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.pirotator.ui.ansi.parser.Command;
import org.prowl.pirotator.ui.ansi.parser.CommandParser;
import org.prowl.pirotator.ui.ansi.parser.ScreenWriter;
import org.prowl.pirotator.PiRotator;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.InputFilter;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;

public class ANSIClient extends Thread implements ScreenWriter {

   private static final Log   LOG = LogFactory.getLog("ANSIClient");

   private TelnetTerminal     terminal;
   private TerminalScreen     screen;
   private MultiWindowTextGUI gui;
   private BasicWindow        desktop;
   private TextBox            inputPanel;
   private TextBox            outputPanel;

   private CommandParser      commandParser;

   public ANSIClient(TelnetTerminal terminal) {
      this.terminal = terminal;
      commandParser = new CommandParser(this);
   }

   public void run() {

      try {

         screen = new TerminalScreen(terminal);

         screen.startScreen();
         terminal.clearScreen();

         gui = new MultiWindowTextGUI(screen);

         gui.setTheme(LanternaThemes.getRegisteredTheme("blaster"));

         buildDesktop();

      } catch (Throwable e) {
         LOG.error(e.getMessage(), e);
      }

      try {
         screen.close();
      } catch (Throwable e) {
      }
   }

   public void terminate() {
      try {
         screen.clear();
         screen.close();
      } catch (Throwable e) {
      }
   }

   public void buildDesktop() {

      Panel content = new Panel();
      content.setLayoutManager(new BorderLayout());

      inputPanel = new TextBox("", TextBox.Style.SINGLE_LINE);
      inputPanel.setPreferredSize(new TerminalSize(80, 1));
      inputPanel.setInputFilter(new InputFilter() {

         @Override
         public boolean onInput(Interactable interactable, KeyStroke keyStroke) {
            if (keyStroke.getKeyType() == KeyType.Enter) {
               // Submit text
               processInput(inputPanel.getText());
               inputPanel.setText("");
            } else if (keyStroke.getKeyType() == KeyType.ArrowUp) {
               // History
               outputPanel.handleKeyStroke(keyStroke);
               return false;
            } else if (keyStroke.getKeyType() == KeyType.ArrowDown) {
               // History
               outputPanel.handleKeyStroke(keyStroke);
               return false;
            }
            return true;
         }
      });

      outputPanel = new TextBox("", TextBox.Style.MULTI_LINE);
      outputPanel.setEnabled(false);
      outputPanel.withBorder(Borders.singleLine());

      content.addComponent(outputPanel, BorderLayout.Location.CENTER);
      content.addComponent(inputPanel, BorderLayout.Location.BOTTOM);

      desktop = new BasicWindow();
      desktop.setComponent(content);
      desktop.setHints(Arrays.asList(Window.Hint.FULL_SCREEN));

      inputPanel.takeFocus();

      showGreeting();

      gui.addWindowAndWait(desktop);

   }

   public void processInput(String input) {
      commandParser.parse(input.trim());
   }

   @Override
   public void write(String s) {
      outputPanel.addLine(s);
      outputPanel.handleKeyStroke(new KeyStroke(KeyType.ArrowDown));
   }

   /**
    * Show a greeting about the system when a user connects
    */
   public void showGreeting() {
      write("*** " + PiRotator.VERSION_STRING + " build " + PiRotator.BUILD);
      write("");
      write("Software CC-BY-SA ");
      write("");
      write("Type 'HELP' for a list of commands");
      commandParser.doCommand(Command.PORTS, new String[] {});

   }

}