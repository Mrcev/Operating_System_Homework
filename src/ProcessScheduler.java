import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class ProcessScheduler extends JFrame {

    // Süreç (Job) verilerini ve sonuçlarını tutan sınıf
    static class Job implements Comparable<Job> {
        String name;
        int arrival;
        int duration; // Çalışma süresi (Burst Time)
        int level;    // Öncelik (Priority)

        // Simülasyon sonrası metrikler
        int start;
        int end;
        int turnaround;
        int wait;
        int left;     // Kalan çalışma süresi (Round Robin için)

        public Job(String name, int arrival, int duration, int level) {
            this.name = name;
            this.arrival = arrival;
            this.duration = duration;
            this.level = level;
            this.left = duration;
        }

        // Simülasyonu temiz başlatmak için kopyalama metodu
        public Job duplicate() {
            return new Job(this.name, this.arrival, this.duration, this.level);
        }

        @Override
        public int compareTo(Job other) {
            return Integer.compare(this.arrival, other.arrival);
        }
    }

    // Gantt Şeması segmentlerini temsil eden sınıf
    static class ChartSegment {
        String owner; // Hangi sürecin çalıştığı (PID veya IDLE)
        int in;      // Başlangıç zamanı
        int out;     // Bitiş zamanı

        public ChartSegment(String owner, int in, int out) {
            this.owner = owner;
            this.in = in;
            this.out = out;
        }
    }

    // YENİ EKLEME: Sadece okuma (Read-Only) sağlayan tablo modeli
    class NonEditableTableModel extends DefaultTableModel {
        NonEditableTableModel(Object[] columnNames, int rowCount) {
            super(columnNames, rowCount);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Hiçbir hücre düzenlenemez
        }
    }
    // YENİ EKLEME BİTTİ

    // GUI bileşenleri
    private JTextArea txtInput;
    private JTable tblStats;
    private NonEditableTableModel modelStats; // Tür değişti
    private VisualizationPanel pnlChart;
    private JComboBox<String> cmbStrategy;
    private JTextField txtQuantum;
    private JLabel lblAvgWait, lblAvgTurn, lblUtil;

    private List<Job> rawData = new ArrayList<>();

    public ProcessScheduler() {
        setTitle("Gokberk Ceviker OS_HOMEWORK");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        initUI();
    }

    // Arayüz bileşenlerini ayarlar
    private void initUI() {
        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnLoad = new JButton("Load File");
        JButton btnRun = new JButton("Run Simulation");

        cmbStrategy = new JComboBox<>(new String[]{"FCFS", "SJF (Non-Preemptive)", "Priority (Non-Preemptive)", "Round Robin"});
        txtQuantum = new JTextField("3", 5);
        txtQuantum.setEnabled(false);

        // Round Robin seçilince kuantum alanını aktifleştirme (UX)
        cmbStrategy.addActionListener(e -> {
            boolean isRR = ((String) cmbStrategy.getSelectedItem()).startsWith("Round");
            txtQuantum.setEnabled(isRR);
            txtQuantum.setBackground(isRR ? Color.WHITE : Color.LIGHT_GRAY);
        });

        pnlTop.add(new JLabel("Input File:"));
        pnlTop.add(btnLoad);
        pnlTop.add(new JLabel("Algorithm:"));
        pnlTop.add(cmbStrategy);
        pnlTop.add(new JLabel("Time Quantum:"));
        pnlTop.add(txtQuantum);
        pnlTop.add(btnRun);

        add(pnlTop, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        txtInput = new JTextArea(8, 40);
        txtInput.setEditable(false);
        JScrollPane scrollInput = new JScrollPane(txtInput);
        scrollInput.setBorder(BorderFactory.createTitledBorder("File Contents"));

        String[] cols = {"ID", "Arrival", "Burst", "Priority", "Finish", "Turnaround", "Waiting"};

        // NonEditableTableModel kullanımı
        modelStats = new NonEditableTableModel(cols, 0);
        tblStats = new JTable(modelStats);
        JScrollPane scrollTable = new JScrollPane(tblStats);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Results"));

        JPanel pnlStats = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        lblAvgTurn = new JLabel("Avg Turnaround: 0.0");
        lblAvgWait = new JLabel("Avg Waiting: 0.0");
        lblUtil = new JLabel("CPU Utilization: 0%");
        pnlStats.add(lblAvgTurn);
        pnlStats.add(lblAvgWait);
        pnlStats.add(lblUtil);

        JPanel pnlCenterBottom = new JPanel(new BorderLayout());
        pnlCenterBottom.add(scrollTable, BorderLayout.CENTER);
        pnlCenterBottom.add(pnlStats, BorderLayout.SOUTH);

        split.setTopComponent(scrollInput);
        split.setBottomComponent(pnlCenterBottom);
        split.setDividerLocation(150);
        add(split, BorderLayout.CENTER);

        pnlChart = new VisualizationPanel();
        pnlChart.setPreferredSize(new Dimension(1000, 150));
        pnlChart.setBorder(BorderFactory.createTitledBorder("Gantt Visualization"));
        add(pnlChart, BorderLayout.SOUTH);

        btnLoad.addActionListener(e -> browseFile());
        btnRun.addActionListener(e -> startSimulation());
    }

    // Simülasyonu başlatır ve doğru algoritmayı çağırır
    private void startSimulation() {
        if (rawData.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load a file first.");
            return;
        }

        // Orijinal veriyi korumak için derin kopyalama
        List<Job> activeJobs = new ArrayList<>();
        for (Job j : rawData) activeJobs.add(j.duplicate());

        String mode = (String) cmbStrategy.getSelectedItem();
        List<ChartSegment> timeline = new ArrayList<>();

        try {
            // Algoritma seçimine göre ilgili metodu çağır
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
            JOptionPane.showMessageDialog(this, "Invalid Time Quantum value.");
        }
    }

    // --- Algoritmalar ---

    // Round Robin (RR) Algoritması (Preemptive)
    private void calcRoundRobin(List<Job> jobs, int q, List<ChartSegment> timeline) {
        jobs.sort(Comparator.comparingInt(j -> j.arrival));
        Queue<Job> readyQ = new LinkedList<>();

        int now = 0, doneCount = 0, idx = 0;
        int total = jobs.size();

        // Başlangıçta gelen süreçleri kuyruğa ekle
        while(idx < total && jobs.get(idx).arrival <= now) {
            readyQ.add(jobs.get(idx++));
        }

        while(doneCount < total) {
            if (readyQ.isEmpty()) {
                // CPU boşta (IDLE)
                if (idx < total) {
                    int nextArr = jobs.get(idx).arrival;
                    timeline.add(new ChartSegment("IDLE", now, nextArr));
                    now = nextArr;
                    // Yeni gelenleri kuyruğa ekle
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

            // Önemli RR Mantığı: Yeni gelenleri, mevcut süreçten önce kuyruğa al
            while(idx < total && jobs.get(idx).arrival <= now) {
                readyQ.add(jobs.get(idx++));
            }

            if (current.left > 0) {
                readyQ.add(current); // Kalan süresi varsa kuyruğun sonuna ekle
            } else {
                current.end = now;
                doneCount++; // Süreç tamamlandı
            }
        }
    }

    // Öncelikli Planlama (Priority Scheduling) Algoritması (Non-Preemptive)
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
                // Seçim: En düşük öncelik değeri (en yüksek öncelik), eşitlikte FCFS
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

    // En Kısa İş İlk (SJF) Algoritması (Non-Preemptive)
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
                // Seçim: En kısa çalışma süresi, eşitlikte FCFS
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

    // İlk Gelen İlk Hizmet (FCFS) Algoritması (Non-Preemptive)
    private void calcFCFS(List<Job> jobs, List<ChartSegment> timeline) {
        jobs.sort(Comparator.comparingInt(j -> j.arrival)); // Varış zamanına göre sırala
        int now = 0;

        for (Job j : jobs) {
            if (now < j.arrival) {
                // Varıştan önce bekleme (IDLE) süresi
                timeline.add(new ChartSegment("IDLE", now, j.arrival));
                now = j.arrival;
            }
            int start = now;
            now += j.duration;
            j.end = now;
            timeline.add(new ChartSegment(j.name, start, now));
        }
    }

    // Metrikleri hesaplar ve GUI'yi günceller
    private void renderMetrics(List<Job> jobs, List<ChartSegment> timeline) {
        modelStats.setRowCount(0);
        double sumWait = 0, sumTurn = 0, sumBurst = 0;
        int maxEnd = 0;

        jobs.sort(Comparator.comparing(j -> j.name)); // Tablo için ID'ye göre sırala

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

        // CPU Kullanımı = (Toplam Burst Süresi / Toplam Simülasyon Süresi) * 100
        double util = (maxEnd > 0) ? (sumBurst / maxEnd) * 100.0 : 0.0;
        lblUtil.setText(String.format("CPU Utilization: %.2f%%", util));

        pnlChart.drawData(timeline, maxEnd);
    }

    // Dosya okuma ve verileri yükleme
    private void browseFile() {
        JFileChooser fc = new JFileChooser(new File("."));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            rawData.clear();
            txtInput.setText("");
            try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    txtInput.append(line + "\n");
                    String[] token = line.split(",");
                    rawData.add(new Job(token[0].trim(),
                            Integer.parseInt(token[1].trim()),
                            Integer.parseInt(token[2].trim()),
                            Integer.parseInt(token[3].trim())));
                }
                JOptionPane.showMessageDialog(this, "Loaded " + rawData.size() + " tasks.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Read Error: " + e.getMessage());
            }
        }
    }

    // Gantt Şeması çizim paneli
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

            int h = getHeight();
            int w = getWidth();
            int barH = 40;
            int y = (h - barH) / 2;
            double pxPerUnit = (double) (w - 40) / totalDuration; // Ölçeklendirme faktörü

            for (ChartSegment s : segments) {
                int x = 20 + (int) (s.in * pxPerUnit);
                int width = (int) ((s.out - s.in) * pxPerUnit);

                if (s.owner.equals("IDLE")) g.setColor(Color.LIGHT_GRAY);
                else g.setColor(generateColor(s.owner));

                g.fillRect(x, y, width, barH);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, width, barH);

                // Segment adı (PID)
                FontMetrics fm = g.getFontMetrics();
                int txtX = x + (width - fm.stringWidth(s.owner)) / 2;
                int txtY = y + (barH + fm.getAscent()) / 2 - 2;
                g.drawString(s.owner, txtX, txtY);

                // Başlangıç zamanı etiketi
                g.drawString(String.valueOf(s.in), x, y + barH + 15);
            }
            // Bitiş zamanı etiketi
            int finalX = 20 + (int)(totalDuration * pxPerUnit);
            g.drawString(String.valueOf(totalDuration), finalX, y + barH + 15);
        }

        // PID'ye göre rastgele renk üretir
        private Color generateColor(String seed) {
            int hash = seed.hashCode();
            return new Color((hash * 100) % 255, (hash * 200) % 255, (hash * 300) % 255);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProcessScheduler().setVisible(true));
    }
}