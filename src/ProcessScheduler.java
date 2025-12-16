import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class ProcessScheduler extends JFrame {

    // --- Renk Paleti ---
    private final Color PRIMARY_COLOR = new Color(52, 152, 219); // Mavi
    private final Color ACCENT_COLOR = new Color(46, 204, 113);  // Yeşil
    private final Color BG_COLOR = new Color(236, 240, 241);     // Açık Gri
    private final Color TEXT_COLOR = new Color(44, 62, 80);      // Koyu Lacivert

    // Job Class
    static class Job implements Comparable<Job> {
        String name;
        int arrival;
        int duration;
        int level;

        int start;
        int end;
        int turnaround;
        int wait;
        int left;

        public Job(String name, int arrival, int duration, int level) {
            this.name = name;
            this.arrival = arrival;
            this.duration = duration;
            this.level = level;
            this.left = duration;
        }

        public Job duplicate() {
            return new Job(this.name, this.arrival, this.duration, this.level);
        }

        @Override
        public int compareTo(Job other) {
            return Integer.compare(this.arrival, other.arrival);
        }
    }

    // Chart Segment
    static class ChartSegment {
        String owner;
        int in;
        int out;

        public ChartSegment(String owner, int in, int out) {
            this.owner = owner;
            this.in = in;
            this.out = out;
        }
    }

    // Read-Only Table Model
    class NonEditableTableModel extends DefaultTableModel {
        NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    // GUI Components
    private JTextArea txtInput;
    private JTable tblStats;
    private NonEditableTableModel modelStats;
    private VisualizationPanel pnlChart;
    private JComboBox<String> cmbStrategy;
    private JTextField txtQuantum;
    private JLabel lblAvgWait, lblAvgTurn, lblUtil;

    private List<Job> rawData = new ArrayList<>();

    public ProcessScheduler() {
        setTitle("Gokberk Ceviker OS_HOMEWORK");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG_COLOR);

        initUI();

        // --- OTOMATİK YÜKLEME ---
        // Program açıldığında src/processes.txt dosyasını kontrol et ve yükle
        autoLoadDefaultFile();
    }

    private void initUI() {
        // --- Top Panel ---
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(189, 195, 199)));

        JButton btnLoad = createStyledButton("Load File", PRIMARY_COLOR);
        JButton btnRun = createStyledButton("Run Simulation", PRIMARY_COLOR);

        cmbStrategy = new JComboBox<>(new String[]{"FCFS", "SJF (Non-Preemptive)", "Priority (Non-Preemptive)", "Round Robin"});
        cmbStrategy.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbStrategy.setBackground(Color.WHITE);

        txtQuantum = new JTextField("3", 4);
        txtQuantum.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtQuantum.setHorizontalAlignment(JTextField.CENTER);
        txtQuantum.setEnabled(false);

        cmbStrategy.addActionListener(e -> {
            boolean isRR = ((String) cmbStrategy.getSelectedItem()).startsWith("Round");
            txtQuantum.setEnabled(isRR);
            txtQuantum.setBackground(isRR ? Color.WHITE : new Color(220, 220, 220));
        });

        pnlTop.add(createLabel("Input File:"));
        pnlTop.add(btnLoad);
        pnlTop.add(Box.createHorizontalStrut(20));
        pnlTop.add(createLabel("Algorithm:"));
        pnlTop.add(cmbStrategy);
        pnlTop.add(createLabel("Time Quantum:"));
        pnlTop.add(txtQuantum);
        pnlTop.add(Box.createHorizontalStrut(20));
        pnlTop.add(btnRun);

        add(pnlTop, BorderLayout.NORTH);

        // --- Center Panel ---
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setDividerSize(10);
        split.setBorder(null);
        split.setBackground(BG_COLOR);

        txtInput = new JTextArea(8, 40);
        txtInput.setEditable(false);
        txtInput.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtInput.setBorder(new EmptyBorder(5,5,5,5));

        JScrollPane scrollInput = new JScrollPane(txtInput);
        scrollInput.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR), " File Contents ",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), PRIMARY_COLOR));
        scrollInput.setBackground(Color.WHITE);

        String[] cols = {"ID", "Arrival", "Burst", "Priority", "Finish", "Turnaround", "Waiting"};
        modelStats = new NonEditableTableModel(cols, 0);
        tblStats = new JTable(modelStats);

        tblStats.setRowHeight(25);
        tblStats.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblStats.setGridColor(new Color(224, 224, 224));
        tblStats.setSelectionBackground(new Color(232, 240, 254));
        tblStats.setSelectionForeground(Color.BLACK);

        JTableHeader header = tblStats.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(TEXT_COLOR);

        JScrollPane scrollTable = new JScrollPane(tblStats);
        scrollTable.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR), " Simulation Results ",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), ACCENT_COLOR));
        scrollTable.getViewport().setBackground(Color.WHITE);

        JPanel pnlStats = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        pnlStats.setBackground(Color.WHITE);
        pnlStats.setBorder(new EmptyBorder(5,0,5,0));

        lblAvgTurn = createStatLabel("Avg Turnaround: 0.0");
        lblAvgWait = createStatLabel("Avg Waiting: 0.0");
        lblUtil = createStatLabel("CPU Utilization: 0%");

        pnlStats.add(lblAvgTurn);
        pnlStats.add(lblAvgWait);
        pnlStats.add(lblUtil);

        JPanel pnlCenterBottom = new JPanel(new BorderLayout());
        pnlCenterBottom.add(scrollTable, BorderLayout.CENTER);
        pnlCenterBottom.add(pnlStats, BorderLayout.SOUTH);

        split.setTopComponent(scrollInput);
        split.setBottomComponent(pnlCenterBottom);
        split.setDividerLocation(180);

        JPanel pnlWrapper = new JPanel(new BorderLayout());
        pnlWrapper.add(split, BorderLayout.CENTER);
        pnlWrapper.setBorder(new EmptyBorder(0, 15, 0, 15));
        pnlWrapper.setBackground(BG_COLOR);

        add(pnlWrapper, BorderLayout.CENTER);

        // --- Bottom Panel ---
        pnlChart = new VisualizationPanel();
        pnlChart.setPreferredSize(new Dimension(1000, 140));
        pnlChart.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), " Gantt Visualization ",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.DARK_GRAY));
        pnlChart.setBackground(Color.WHITE);

        JPanel pnlBottomWrapper = new JPanel(new BorderLayout());
        pnlBottomWrapper.add(pnlChart, BorderLayout.CENTER);
        pnlBottomWrapper.setBorder(new EmptyBorder(10, 15, 15, 15));
        pnlBottomWrapper.setBackground(BG_COLOR);

        add(pnlBottomWrapper, BorderLayout.SOUTH);

        // Actions
        btnLoad.addActionListener(e -> browseFile());
        btnRun.addActionListener(e -> startSimulation());
    }

    // --- Helper Methods ---

    // Varsayılan dosyayı otomatik yükleyen metot
    private void autoLoadDefaultFile() {
        // Öncelik: src/processes.txt
        File defaultFile = new File("src/processes.txt");

        // Eğer src içinde bulamazsa proje kök dizininde ara
        if (!defaultFile.exists()) {
            defaultFile = new File("processes.txt");
        }

        if (defaultFile.exists()) {
            loadDataFromFile(defaultFile);
            // Otomatik yüklemede mesaj kutusu çıkarmıyoruz, sadece statü barına yazabiliriz veya sessiz kalırız.
        } else {
            System.out.println("Default file not found: processes.txt");
        }
    }

    // Dosya seçme diyaloğunu açan metot
    private void browseFile() {
        JFileChooser fc = new JFileChooser(new File("."));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadDataFromFile(fc.getSelectedFile());
        }
    }

    // Gerçek dosya okuma işlemini yapan ayrıştırılmış metot
    private void loadDataFromFile(File file) {
        rawData.clear();
        txtInput.setText("");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                txtInput.append(line + "\n");
                String[] token = line.split(",");
                if (token.length >= 4) {
                    rawData.add(new Job(token[0].trim(),
                            Integer.parseInt(token[1].trim()),
                            Integer.parseInt(token[2].trim()),
                            Integer.parseInt(token[3].trim())));
                }
            }
            // Başarılı yükleme mesajı (İsteğe bağlı, otomatik yüklemede kaldırılabilir ama kullanıcı görsün diye bırakıyorum)
            // JOptionPane.showMessageDialog(this, "Loaded " + rawData.size() + " tasks.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Read Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        return btn;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_COLOR);
        return lbl;
    }

    private JLabel createStatLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(50, 50, 50));
        return lbl;
    }

    // --- Simulation Logic ---

    private void startSimulation() {
        if (rawData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load a file first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Job> activeJobs = new ArrayList<>();
        for (Job j : rawData) activeJobs.add(j.duplicate());

        String mode = (String) cmbStrategy.getSelectedItem();
        List<ChartSegment> timeline = new ArrayList<>();

        try {
            if (mode.startsWith("Round")) {
                int q = Integer.parseInt(txtQuantum.getText().trim());
                calcRoundRobin(activeJobs, q, timeline);
            } else if (mode.startsWith("Priority")) {
                calcPriority(activeJobs, timeline);
            } else if (mode.startsWith("SJF")) {
                calcSJF(activeJobs, timeline);
            } else {
                calcFCFS(activeJobs, timeline);
            }
            renderMetrics(activeJobs, timeline);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Time Quantum value.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calcRoundRobin(List<Job> jobs, int q, List<ChartSegment> timeline) {
        jobs.sort(Comparator.comparingInt(j -> j.arrival));
        Queue<Job> readyQ = new LinkedList<>();

        int now = 0, doneCount = 0, idx = 0;
        int total = jobs.size();

        while(idx < total && jobs.get(idx).arrival <= now) {
            readyQ.add(jobs.get(idx++));
        }

        while(doneCount < total) {
            if (readyQ.isEmpty()) {
                if (idx < total) {
                    int nextArr = jobs.get(idx).arrival;
                    timeline.add(new ChartSegment("IDLE", now, nextArr));
                    now = nextArr;
                    while(idx < total && jobs.get(idx).arrival <= now) {
                        readyQ.add(jobs.get(idx++));
                    }
                }
                continue;
            }

            Job current = readyQ.poll();
            int exec = Math.min(current.left, q);

            timeline.add(new ChartSegment(current.name, now, now + exec));
            now += exec;
            current.left -= exec;

            while(idx < total && jobs.get(idx).arrival <= now) {
                readyQ.add(jobs.get(idx++));
            }

            if (current.left > 0) {
                readyQ.add(current);
            } else {
                current.end = now;
                doneCount++;
            }
        }
    }

    private void calcPriority(List<Job> jobs, List<ChartSegment> timeline) {
        int now = 0, done = 0;
        Set<String> finished = new HashSet<>();

        while (done < jobs.size()) {
            List<Job> ready = new ArrayList<>();
            for (Job j : jobs) {
                if (j.arrival <= now && !finished.contains(j.name)) ready.add(j);
            }

            if (ready.isEmpty()) {
                timeline.add(new ChartSegment("IDLE", now, ++now));
            } else {
                ready.sort((a, b) -> a.level != b.level ? Integer.compare(a.level, b.level) : Integer.compare(a.arrival, b.arrival));
                Job picked = ready.get(0);
                int start = now;
                now += picked.duration;
                picked.end = now;
                finished.add(picked.name);
                done++;
                timeline.add(new ChartSegment(picked.name, start, now));
            }
        }
    }

    private void calcSJF(List<Job> jobs, List<ChartSegment> timeline) {
        int now = 0, done = 0;
        Set<String> finished = new HashSet<>();

        while (done < jobs.size()) {
            List<Job> ready = new ArrayList<>();
            for (Job j : jobs) {
                if (j.arrival <= now && !finished.contains(j.name)) ready.add(j);
            }

            if (ready.isEmpty()) {
                timeline.add(new ChartSegment("IDLE", now, ++now));
            } else {
                ready.sort((a, b) -> a.duration != b.duration ? Integer.compare(a.duration, b.duration) : Integer.compare(a.arrival, b.arrival));
                Job picked = ready.get(0);
                int start = now;
                now += picked.duration;
                picked.end = now;
                finished.add(picked.name);
                done++;
                timeline.add(new ChartSegment(picked.name, start, now));
            }
        }
    }

    private void calcFCFS(List<Job> jobs, List<ChartSegment> timeline) {
        jobs.sort(Comparator.comparingInt(j -> j.arrival));
        int now = 0;

        for (Job j : jobs) {
            if (now < j.arrival) {
                timeline.add(new ChartSegment("IDLE", now, j.arrival));
                now = j.arrival;
            }
            int start = now;
            now += j.duration;
            j.end = now;
            timeline.add(new ChartSegment(j.name, start, now));
        }
    }

    private void renderMetrics(List<Job> jobs, List<ChartSegment> timeline) {
        modelStats.setRowCount(0);
        double sumWait = 0, sumTurn = 0, sumBurst = 0;
        int maxEnd = 0;

        jobs.sort(Comparator.comparing(j -> j.name));

        for (Job j : jobs) {
            j.turnaround = j.end - j.arrival;
            j.wait = j.turnaround - j.duration;

            sumWait += j.wait;
            sumTurn += j.turnaround;
            sumBurst += j.duration;
            if (j.end > maxEnd) maxEnd = j.end;

            modelStats.addRow(new Object[]{j.name, j.arrival, j.duration, j.level, j.end, j.turnaround, j.wait});
        }

        lblAvgWait.setText(String.format("Avg Waiting: %.2f", sumWait / jobs.size()));
        lblAvgTurn.setText(String.format("Avg Turnaround: %.2f", sumTurn / jobs.size()));

        double util = (maxEnd > 0) ? (sumBurst / maxEnd) * 100.0 : 0.0;
        lblUtil.setText(String.format("CPU Utilization: %.1f%%", util));

        pnlChart.drawData(timeline, maxEnd);
    }

    // --- Visualization Panel ---
    class VisualizationPanel extends JPanel {
        private List<ChartSegment> segments;
        private int totalDuration;

        public void drawData(List<ChartSegment> segments, int duration) {
            this.segments = segments;
            this.totalDuration = duration;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (segments == null || segments.isEmpty()) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int h = getHeight();
            int w = getWidth();
            int barH = 50;
            int y = (h - barH) / 2;
            double pxPerUnit = (double) (w - 60) / totalDuration;

            for (ChartSegment s : segments) {
                int x = 30 + (int) (s.in * pxPerUnit);
                int width = (int) ((s.out - s.in) * pxPerUnit);

                if (s.owner.equals("IDLE")) {
                    g2d.setColor(new Color(220, 220, 220));
                } else {
                    g2d.setColor(generateColor(s.owner));
                }

                g2d.fillRoundRect(x, y, width, barH, 5, 5);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(x, y, width, barH, 5, 5);

                g2d.setColor(s.owner.equals("IDLE") ? Color.GRAY : Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2d.getFontMetrics();

                if(width > 15) {
                    int txtX = x + (width - fm.stringWidth(s.owner)) / 2;
                    int txtY = y + (barH + fm.getAscent()) / 2 - 2;
                    g2d.drawString(s.owner, txtX, txtY);
                }

                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2d.drawString(String.valueOf(s.in), x - 3, y + barH + 18);
            }
            int finalX = 30 + (int)(totalDuration * pxPerUnit);
            g2d.drawString(String.valueOf(totalDuration), finalX - 3, y + barH + 18);
        }

        private Color generateColor(String seed) {
            int hash = seed.hashCode();
            return new Color(
                    (hash * 123 + 50) % 200,
                    (hash * 345 + 50) % 200,
                    (hash * 678 + 50) % 200
            );
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new ProcessScheduler().setVisible(true));
    }
}