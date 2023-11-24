import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.Font.PLAIN;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class SummarizerGUI extends JFrame {

    public SummarizerGUI(){
        super("Summarizer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 700);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(true);
        setMinimumSize(new Dimension(400, 300));
        setLayout(new BorderLayout());


        initUI();
    }

    JTextArea textArea = new JTextArea();

    private void initUI() {
        //JTextArea textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBounds(50, 50, 700, 500);
        textArea.setFont(new Font("Dialog", PLAIN, 16));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(380, 200));
        scrollPane.setBounds(50, 50, 700, 500);

        JButton openButton = new JButton("Open PDF");
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });

        textArea.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    Transferable transferable = support.getTransferable();
                    java.util.List<File> files = (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        displayPDFContent(file);
                        return true;
                    }
                } catch (UnsupportedFlavorException | IOException ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });

        /*JButton save = new JButton("Save");
        save.setBounds(0,400,200, 50);
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //FileWriter write = null;

                try{
                    saveToPDF();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }

            }
        });

        add(save);

         */

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBounds(0,0,200, 50);
        buttonPanel.add(openButton);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".pdf") || f.isDirectory();
            }

            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            displayPDFContent(selectedFile);
        }
    }

    private void displayPDFContent(File file) {
        // Add logic to read and display PDF content
        try {
            PDDocument document = Loader.loadPDF(file);
            PDFTextStripper textStripper = new PDFTextStripper();

            // Set the range of pages to extract text from (1 to the end)
            textStripper.setStartPage(1);
            textStripper.setEndPage(document.getNumberOfPages());

            // Get the text content of the PDF
            String pdfContent = textStripper.getText(document);

            // Summarize the text content
            String summary = summarizeText(pdfContent);

            // Display or do something with the summarized text
            textArea.setText(summary);

            document.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message to the user)
        }
    }

    private void saveToPDF() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().toLowerCase().endsWith(".pdf") || f.isDirectory();
            }

            public String getDescription() {
                return "PDF Files (*.pdf)";
            }
        });

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (!selectedFile.getAbsolutePath().endsWith(".pdf")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".pdf");
            }

            try {
                createPDF(selectedFile, textArea.getText());
                JOptionPane.showMessageDialog(this, "PDF saved successfully!");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error saving PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void createPDF(File file, String content) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Use a TrueType font for better compatibility
            InputStream fontStream = getClass().getResourceAsStream("/COURIER.ttf");
            PDType0Font font = PDType0Font.load(document, fontStream);

            // Set the font and font size
            contentStream.setFont(font, 12);

            // Begin the text block
            contentStream.beginText();

            // Set the position to start writing
            contentStream.newLineAtOffset(20, page.getMediaBox().getHeight() - 20);

            content = content.replaceAll("\n", " ");

            // Write the content to the PDF
            contentStream.showText(content);

            // End the text block
            contentStream.endText();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message to the user)
        } finally {
            document.save(file);
            document.close();
        }
    }


    private String summarizeText(String fullText) {
        // Split the text into sentences
        String[] sentences = fullText.split("[.!?]");

        // Calculate the importance score for each sentence (simple example: sentence length)
        List<Sentence> sentenceList = Arrays.stream(sentences)
                .map(sentence -> new Sentence(sentence, sentence.split("\\s+").length))
                .collect(Collectors.toList());

        // Sort sentences by importance (length in this example)
        sentenceList.sort(Comparator.comparingInt(Sentence::getLength).reversed());

        // Select the top N sentences for the summary (adjust N based on your preference)
        int numSentencesInSummary = 3;
        List<String> summarySentences = sentenceList.stream()
                .limit(numSentencesInSummary)
                .map(Sentence::getText)
                .collect(Collectors.toList());

        // Join the selected sentences to form the summary
        return String.join(" ", summarySentences);
    }

    private static class Sentence {
        private final String text;
        private final int length;

        public Sentence(String text, int length) {
            this.text = text;
            this.length = length;
        }

        public String getText() {
            return text;
        }

        public int getLength() {
            return length;
        }
    }

}
