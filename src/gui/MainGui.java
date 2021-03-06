/*
 * Copyright (c) 2014, Dries007
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of the project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gui;

import JavaEasembly.Main;
import compiler.components.Symbol;
import upload.Uploader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import static gui.AsmDocumentListener.DOCUMENT_LISTENER;
import static gui.AsmParser.ASM_PARSER;
import static gui.FindAndReplace.FIND_AND_REPLACE;
import static util.Constants.*;

/**
 * @author Dries007
 */
public class MainGui
{
    public static final MainGui      MAIN_GUI      = new MainGui();
    public final        JFileChooser fileChooser   = new JFileChooser();
    public final        JFileChooser folderChooser = new JFileChooser();
    public final        JFontChooser fontChooser   = new JFontChooser();
    public final JFrame               frame;
    public       JTabbedPane          tabPane;
    public       JMenuItem            loadFile;
    public       JMenuItem            saveFile;
    public       JMenuItem            changeFont;
    public       JMenuItem            changeTabSize;
    public       JMenuItem            compile;
    public       JPanel               root;
    public       JMenuBar             menuBar;
    public       RSyntaxTextArea      asmContents;
    public       RSyntaxTextArea      preText;
    public       JTable               symbolsTable;
    public       JTable               componentsTable;
    public       JTable               hexTable;
    public       JTabbedPane          includeFiles;
    public       JLabel               status;
    public       JButton              compileButton;
    public       JButton              uploadButton;
    public       JButton              saveButton;
    public       RTextScrollPane      asmContentsScroll;
    public       RTextScrollPane      preTextScroll;
    public       JButton              findButton;
    public       JComboBox<String>    comPortBox;
    public       JComboBox<Integer>   baudRateBox;
    public       JComboBox<Uploader>  deviceTypeBox;
    public       JCheckBoxMenuItem    autoSave;
    public       JCheckBoxMenuItem    autoCompile;
    public       JMenuItem            includeFolder;
    public       JRadioButtonMenuItem encodingDefault;
    public       JRadioButtonMenuItem encodingUtf8;
    public       JRadioButtonMenuItem encodingAnsi;
    public       JMenuItem            uploadCommand;
    public       JMenuItem            newFile;
    public       JMenuItem            about;

    public HashMap<String, Symbol> symbolHashMap;

    //public UploadRunnable  uploadRunnable  = new UploadRunnable();
    public CompileRunnable compileRunnable = new CompileRunnable();

    private MainGui()
    {
        JFrame.setDefaultLookAndFeelDecorated(true);
        $$$setupUI$$$();
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping(SYNTAX_NAME, Assembler8051TokenMaker.class.getName());

        fontChooser.setSelectedFontFamily(PROPERTIES.getProperty(FONT_NAME, "Courier New"));
        fontChooser.setSelectedFontStyle(Integer.parseInt(PROPERTIES.getProperty(FONT_STYLE, Integer.toString(Font.PLAIN))));
        fontChooser.setSelectedFontSize(Integer.parseInt(PROPERTIES.getProperty(FONT_SIZE, "12")));

        asmContents.setTabSize(Integer.parseInt(PROPERTIES.getProperty(TABSIZE, "4")));
        asmContents.setFont(fontChooser.getSelectedFont());
        asmContents.setSyntaxEditingStyle(SYNTAX_NAME);
        TextLineNumber tln = new TextLineNumber(asmContents);
        asmContentsScroll.setRowHeaderView(tln);
        asmContents.addParser(ASM_PARSER);

        preText.setTabSize(Integer.parseInt(PROPERTIES.getProperty(TABSIZE, "4")));
        preText.setFont(fontChooser.getSelectedFont());
        preText.setSyntaxEditingStyle(SYNTAX_NAME);
        tln = new TextLineNumber(preText);
        preTextScroll.setRowHeaderView(tln);

        componentsTable.setDefaultRenderer(Object.class, new FluoCellRenderer()
        {
            @Override
            public boolean highlight(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                return value.equals("UnsolvedComponent");
            }
        });
        hexTable.setDefaultRenderer(Object.class, new FluoCellRenderer()
        {
            @Override
            public boolean highlight(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                return value.toString().length() > 2 && column != 0;
            }
        });
        symbolsTable.setDefaultRenderer(Object.class, new FluoCellRenderer()
        {
            @Override
            public boolean highlight(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                return value.toString().equals("_UNDEFINED_");
            }
        });

        // Main gui init
        frame = new JFrame("j8051");
        frame.setContentPane(this.$$$getRootComponent$$$());
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setJMenuBar(menuBar);
        frame.pack();
        frame.setLocation(parseInt(PROPERTIES.getProperty(WINDOW_X, "0")), parseInt(PROPERTIES.getProperty(WINDOW_Y, "0")));
        frame.setSize(parseInt(PROPERTIES.getProperty(WINDOW_W, "740")), parseInt(PROPERTIES.getProperty(WINDOW_H, "760")));
        frame.setVisible(true);
        try
        {
            for (String res : new String[]{"1024", "512", "256", "128"})
            {
                ArrayList<Image> imageList = new ArrayList<>();
                URL url = getClass().getResource("/icon/j8051-" + res + ".png");
                if (url == null) continue;
                imageList.add(Toolkit.getDefaultToolkit().getImage(url));
                frame.setIconImages(imageList);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        frame.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                PROPERTIES.setProperty(WINDOW_W, Integer.toString(e.getComponent().getWidth()));
                PROPERTIES.setProperty(WINDOW_H, Integer.toString(e.getComponent().getHeight()));
                saveProperties();
            }

            @Override
            public void componentMoved(ComponentEvent e)
            {
                PROPERTIES.setProperty(WINDOW_X, Integer.toString(e.getComponent().getX()));
                PROPERTIES.setProperty(WINDOW_Y, Integer.toString(e.getComponent().getY()));
                saveProperties();
            }
        });
        newFile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                {
                    File file = fileChooser.getSelectedFile();
                    if (FilenameUtils.getExtension(file.getName()).isEmpty()) file = new File(file.getParentFile(), file.getName() + ".asm");
                    if (!file.exists()) try
                    {
                        file.createNewFile();
                    }
                    catch (IOException e1)
                    {
                        status.setText(e1.getLocalizedMessage());
                        e1.printStackTrace();
                    }
                    Main.setSrcFile(file);
                    changeFile();
                }
            }
        });
        loadFile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                {
                    File file = fileChooser.getSelectedFile();
                    Main.setSrcFile(file);
                    changeFile();
                }
            }
        });
        compile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MAIN_GUI.compile();
            }
        });
        compileButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                MAIN_GUI.compile();
            }
        });
        saveButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveChanges();
            }
        });
        saveFile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveChanges();
            }
        });
        uploadButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                upload();
            }
        });
        changeFont.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (JFontChooser.OK_OPTION == fontChooser.showDialog(frame))
                {
                    asmContents.setFont(fontChooser.getSelectedFont());
                    PROPERTIES.setProperty(FONT_NAME, fontChooser.getSelectedFontFamily());
                    PROPERTIES.setProperty(FONT_SIZE, Integer.toString(fontChooser.getSelectedFontSize()));
                    PROPERTIES.setProperty(FONT_STYLE, Integer.toString(fontChooser.getSelectedFontStyle()));
                    saveProperties();
                }
            }
        });
        autoCompile.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PROPERTIES.setProperty(AUTO_COMPILE, Boolean.toString(autoCompile.getState()));
                saveProperties();
            }
        });
        autoSave.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PROPERTIES.setProperty(AUTO_SAVE, Boolean.toString(autoSave.getState()));
                saveProperties();
            }
        });
        changeTabSize.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    int i = Integer.parseInt(JOptionPane.showInputDialog(frame, "Tab size?", "Tab size", JOptionPane.QUESTION_MESSAGE));
                    PROPERTIES.setProperty(TABSIZE, Integer.toString(i));
                    asmContents.setTabSize(i);
                    saveProperties();
                }
                catch (NumberFormatException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        includeFolder.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (folderChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
                {
                    Main.setIncludeFolder(folderChooser.getSelectedFile());
                }
            }
        });
        encodingDefault.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PROPERTIES.setProperty(ENCODING, ENCODING_DEFAULT);
                changeFile();
                saveProperties();
            }
        });
        encodingAnsi.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PROPERTIES.setProperty(ENCODING, ENCODING_ANSI);
                changeFile();
                saveProperties();
            }
        });
        encodingUtf8.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                PROPERTIES.setProperty(ENCODING, ENCODING_UTF8);
                changeFile();
                saveProperties();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "compile");
        root.getActionMap().put("compile", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                compile();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "upload");
        root.getActionMap().put("upload", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                upload();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "save");
        root.getActionMap().put("save", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                saveChanges();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK), "find");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "find");
        root.getActionMap().put("find", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                FIND_AND_REPLACE.setVisible(true);
                System.out.println(tabPane.getSelectedIndex() == 0 ? asmContents.getSelectedText() : ((RTextScrollPane) includeFiles.getSelectedComponent()).getTextArea().getSelectedText());
                FIND_AND_REPLACE.findBox.setSelectedItem(tabPane.getSelectedIndex() == 0 ? asmContents.getSelectedText() : ((RTextScrollPane) includeFiles.getSelectedComponent()).getTextArea().getSelectedText());
            }
        });
        findButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                FIND_AND_REPLACE.setVisible(true);
            }
        });
        frame.addWindowFocusListener(new WindowAdapter()
        {
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                if (FIND_AND_REPLACE.isVisible() && e.getOppositeWindow() != FIND_AND_REPLACE)
                {
                    FIND_AND_REPLACE.requestFocus();
                    frame.requestFocus();
                }
            }
        });
        about.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    new About();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
        });
        comPortBox.setModel(new DefaultComboBoxModel<>(Uploader.getAvailableComPorts()));
        baudRateBox.setModel(new DefaultComboBoxModel<>(new Integer[]{9600, 19200, 57600, 115200}));
        deviceTypeBox.setModel(new DefaultComboBoxModel<>(Uploader.getAvailableTypes()));
    }

    public void init()
    {
        changeFile();
        compile();
        asmContents.getDocument().addDocumentListener(DOCUMENT_LISTENER);

        if (PROPERTIES.containsKey(SRC_FILE))
        {
            File file = new File(PROPERTIES.getProperty(SRC_FILE));
            if (file.exists()) fileChooser.setSelectedFile(file);
        }
    }

    public void changeFile()
    {
        if (Main.srcFile != null) frame.setTitle(String.format("j8051 - %s", Main.srcFile.getAbsolutePath()));
        asmContents.setEditable(Main.srcFile != null);
        setAsmContents();
    }

    public void setAsmContents()
    {
        AsmDocumentListener.DOCUMENT_LISTENER.active = false;
        Document document = asmContents.getDocument();
        try
        {
            document.remove(0, document.getLength());
            if (Main.srcFile != null)
            {
                document.insertString(0, FileUtils.readFileToString(Main.srcFile, PROPERTIES.getProperty(ENCODING, ENCODING_DEFAULT)).replace("\r", ""), null);
            }
        }
        catch (BadLocationException | IOException e1)
        {
            e1.printStackTrace();
        }
        AsmDocumentListener.DOCUMENT_LISTENER.active = true;
    }

    public void upload()
    {
        final ProgressMonitor pm = new ProgressMonitor(MainGui.MAIN_GUI.frame, "Uploading to µC", "Initializing COM", 0, 100);
        pm.setMillisToPopup(1);
        pm.setMillisToDecideToPopup(1);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    ((Uploader) deviceTypeBox.getSelectedItem()).upload(((String) comPortBox.getSelectedItem()), ((Integer) baudRateBox.getSelectedItem()), pm);
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                    StringBuilder message = new StringBuilder();
                    do
                    {
                        message.append('\n').append(e.toString());
                    } while ((e = e.getCause()) != null);
                    JOptionPane.showMessageDialog(frame, message.substring(1), "Error while uploading", JOptionPane.ERROR_MESSAGE);
                }
                finally
                {
                    pm.close();
                }
            }
        }).start();
    }

    public void compile()
    {
        if (!compileRunnable.running) new Thread(compileRunnable).start();
    }

    public void resizeColumnWidth(JTable table)
    {
        final TableColumnModel columnModel = table.getColumnModel();
        for (int column = 0; column < table.getColumnCount(); column++)
        {
            int width = 50; // Min width
            for (int row = 0; row < table.getRowCount(); row++)
            {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);
                width = Math.max(comp.getPreferredSize().width, width);
            }
            columnModel.getColumn(column).setPreferredWidth(width + 5);
        }
    }

    private void createUIComponents()
    {
        // Filechooser specifications
        fileChooser.addChoosableFileFilter(ASM_FILE_FILTER);
        fileChooser.setFileHidingEnabled(true);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(ASM_FILE_FILTER);

        // Folderchooser specifications
        folderChooser.addChoosableFileFilter(FOLDER_FILTER);
        folderChooser.setFileHidingEnabled(true);
        folderChooser.setMultiSelectionEnabled(false);
        folderChooser.setFileSelectionMode(DIRECTORIES_ONLY);
        folderChooser.setFileFilter(FOLDER_FILTER);

        //Menubar
        menuBar = new JMenuBar();
        //  Filemenu
        JMenu fileMenu = new JMenu("File");

        newFile = new JMenuItem("New...");
        fileMenu.add(newFile);

        loadFile = new JMenuItem("Open...");
        fileMenu.add(loadFile);

        saveFile = new JMenuItem("Save");
        fileMenu.add(saveFile);

        fileMenu.addSeparator();

        compile = new JMenuItem("Compile");
        fileMenu.add(compile);

        // Encoding menu, under file menu
        JMenu encoding = new JMenu("Encoding");
        fileMenu.add(encoding);

        ButtonGroup buttonGroup = new ButtonGroup();

        encodingDefault = new JRadioButtonMenuItem("Platform/Java default");
        if (String.valueOf(PROPERTIES.getProperty(ENCODING)).equals(ENCODING_DEFAULT)) encodingDefault.setSelected(true);
        buttonGroup.add(encodingDefault);
        encoding.add(encodingDefault);

        encodingUtf8 = new JRadioButtonMenuItem("UTF-8");
        if (String.valueOf(PROPERTIES.getProperty(ENCODING)).equals(ENCODING_UTF8)) encodingUtf8.setSelected(true);
        buttonGroup.add(encodingUtf8);
        encoding.add(encodingUtf8);

        encodingAnsi = new JRadioButtonMenuItem("ANSI");
        if (String.valueOf(PROPERTIES.getProperty(ENCODING)).equals(ENCODING_ANSI)) encodingAnsi.setSelected(true);
        buttonGroup.add(encodingAnsi);
        encoding.add(encodingAnsi);

        menuBar.add(fileMenu);
        //  Viewmenu
        JMenu viewMenu = new JMenu("View");

        changeFont = new JMenuItem("Font...");
        viewMenu.add(changeFont);

        changeTabSize = new JMenuItem("Tab size...");
        viewMenu.add(changeTabSize);

        menuBar.add(viewMenu);

        //  optionsmenu
        JMenu optionsMenu = new JMenu("Options");

        includeFolder = new JMenuItem("Include folder...");
        optionsMenu.add(includeFolder);

        optionsMenu.addSeparator();

        autoSave = new JCheckBoxMenuItem("Auto save");
        autoSave.setState(parseBoolean(PROPERTIES.getProperty(AUTO_SAVE, "true")));
        optionsMenu.add(autoSave);

        autoCompile = new JCheckBoxMenuItem("Auto compile");
        autoCompile.setState(parseBoolean(PROPERTIES.getProperty(AUTO_COMPILE, "true")));
        optionsMenu.add(autoCompile);

        menuBar.add(optionsMenu);

        // Helpmenu
        JMenu helpMenu = new JMenu("Help");

        about = new JMenuItem("About...");
        helpMenu.add(about);

        menuBar.add(helpMenu);
    }

    public boolean isAutoCompiling()
    {
        return autoCompile.getState();
    }

    public boolean isAutoSaving()
    {
        return autoSave.getState();
    }

    public void saveChanges()
    {
        try
        {
            FileUtils.writeStringToFile(Main.srcFile, asmContents.getDocument().getText(0, asmContents.getDocument().getLength()), PROPERTIES.getProperty(ENCODING, ENCODING_DEFAULT));
        }
        catch (IOException | BadLocationException e1)
        {
            e1.printStackTrace();
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        createUIComponents();
        root = new JPanel();
        root.setLayout(new GridBagLayout());
        tabPane = new JTabbedPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(tabPane, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        tabPane.addTab("ASM files", panel1);
        asmContentsScroll = new RTextScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(asmContentsScroll, gbc);
        asmContentsScroll.setBorder(BorderFactory.createTitledBorder("Source"));
        asmContents = new RSyntaxTextArea();
        asmContents.setEOLMarkersVisible(false);
        asmContents.setCodeFoldingEnabled(false);
        asmContents.setFadeCurrentLineHighlight(true);
        asmContents.setFractionalFontMetricsEnabled(false);
        asmContents.setPaintTabLines(true);
        asmContents.setRoundedSelectionEdges(false);
        asmContents.setTabsEmulated(true);
        asmContents.setText("");
        asmContents.setUseSelectedTextColor(false);
        asmContents.setWhitespaceVisible(false);
        asmContentsScroll.setViewportView(asmContents);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        tabPane.addTab("Include Files", panel2);
        includeFiles = new JTabbedPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(includeFiles, gbc);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        tabPane.addTab("Pre-processed", panel3);
        preTextScroll = new RTextScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel3.add(preTextScroll, gbc);
        preText = new RSyntaxTextArea();
        preText.setEditable(false);
        preText.setEnabled(true);
        preTextScroll.setViewportView(preText);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridBagLayout());
        tabPane.addTab("Components", panel4);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setVerticalScrollBarPolicy(22);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel4.add(scrollPane1, gbc);
        componentsTable = new JTable();
        componentsTable.setAutoCreateRowSorter(false);
        componentsTable.setAutoResizeMode(3);
        scrollPane1.setViewportView(componentsTable);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridBagLayout());
        tabPane.addTab("Symbols", panel5);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setVerticalScrollBarPolicy(22);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel5.add(scrollPane2, gbc);
        symbolsTable = new JTable();
        symbolsTable.setAutoCreateRowSorter(true);
        symbolsTable.setAutoResizeMode(4);
        scrollPane2.setViewportView(symbolsTable);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridBagLayout());
        tabPane.addTab("Hex", panel6);
        final JScrollPane scrollPane3 = new JScrollPane();
        scrollPane3.setVerticalScrollBarPolicy(22);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel6.add(scrollPane3, gbc);
        hexTable = new JTable();
        hexTable.setAutoResizeMode(0);
        scrollPane3.setViewportView(hexTable);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        root.add(menuBar, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Status:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.ipadx = 10;
        gbc.insets = new Insets(2, 10, 2, 0);
        root.add(label1, gbc);
        status = new JLabel();
        status.setText("No file loaded.");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        root.add(status, gbc);
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.BOTH;
        root.add(toolBar1, gbc);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridBagLayout());
        toolBar1.add(panel7);
        saveButton = new JButton();
        saveButton.setIcon(new ImageIcon(getClass().getResource("/icon/disk.png")));
        saveButton.setText("");
        saveButton.setToolTipText("Save (Ctrl + S)");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(saveButton, gbc);
        compileButton = new JButton();
        compileButton.setIcon(new ImageIcon(getClass().getResource("/icon/compile.png")));
        compileButton.setText("");
        compileButton.setToolTipText("Compile (F5)");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(compileButton, gbc);
        findButton = new JButton();
        findButton.setIcon(new ImageIcon(getClass().getResource("/icon/search.png")));
        findButton.setText("");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel7.add(findButton, gbc);
        uploadButton = new JButton();
        uploadButton.setIcon(new ImageIcon(getClass().getResource("/icon/upload.png")));
        uploadButton.setText("");
        uploadButton.setToolTipText("Upload (F6)");
        gbc = new GridBagConstraints();
        gbc.gridx = 10;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(uploadButton, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel7.add(spacer1, gbc);
        comPortBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comPortBox.setModel(defaultComboBoxModel1);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 0);
        panel7.add(comPortBox, gbc);
        baudRateBox = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 7;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel7.add(baudRateBox, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Upload to");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(label2, gbc);
        final JLabel label3 = new JLabel();
        label3.setText(" at ");
        gbc = new GridBagConstraints();
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(label3, gbc);
        final JLabel label4 = new JLabel();
        label4.setText("baud for device");
        gbc = new GridBagConstraints();
        gbc.gridx = 8;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel7.add(label4, gbc);
        deviceTypeBox = new JComboBox();
        gbc = new GridBagConstraints();
        gbc.gridx = 9;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        panel7.add(deviceTypeBox, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return root;
    }
}
