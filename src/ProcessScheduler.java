import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.List;

// Main Class
public class ProcessScheduler extends JFrame {

    // --- Inner Class: Process ---
    static class Process implements Comparable<Process> {
        String id;
        int arrivalTime;
        int burstTime;
        int priority;

        // Metrics
        int startTime;
        int finishTime;
        int turnaroundTime;
        int waitingTime;
        int remainingTime; // For RR

        public Process(String id, int arrivalTime, int burstTime, int priority) {
            this.id = id;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            this.remainingTime = burstTime;
        }

        // Deep copy for simulation restart
        public Process copy() {
            return new Process(this.id, this.arrivalTime, this.burstTime, this.priority);
        }

        @Override
        public int compareTo(Process o) {
            return Integer.compare(this.arrivalTime, o.arrivalTime);
        }
    }

    // --- Inner Class: Gantt Block (For visualization) ---
    static class GanttBlock {
        String processId;
        int startTime;
        int endTime;

        public GanttBlock(String processId, int startTime, int endTime) {
            this.processId = processId;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    // --- GUI Components ---
    private JTextArea inputArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private GanttPanel ganttPanel;
    private JComboBox<String> algoSelector;
    private JTextField quantumField;
    private JLabel avgWaitLabel, avgTurnLabel, utilLabel;

    private List<Process> loadedProcesses = new ArrayList<>();

    public ProcessScheduler() {
        setTitle("CS 305: Process Scheduling Simulator");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Top Panel: Controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadButton = new JButton("Load File");

        // Algoritma listesi
        algoSelector = new JComboBox<>(new String[]{"FCFS", "SJF (Non-Preemptive)", "Priority (Non-Preemptive)", "Round Robin"});

        // Time Quantum alanı
        quantumField = new JTextField("3", 5);

        // --- YENİ EKLENEN KISIM BAŞLANGIÇ ---
        // 1. Başlangıçta FCFS seçili olduğu için kutuyu pasif yap
        quantumField.setEnabled(false);

        // 2. Seçim değiştiğinde tetiklenecek olay (Event Listener)
        algoSelector.addActionListener(e -> {
            String selected = (String) algoSelector.getSelectedItem();
            // Eğer seçilen metin "Round" ile başlıyorsa (Round Robin ise) kutuyu aktif et
            if (selected.startsWith("Round")) {
                quantumField.setEnabled(true);
                quantumField.setBackground(Color.WHITE); // Görsel ipucu: Beyaz (Aktif)
            } else {
                quantumField.setEnabled(false);
                quantumField.setBackground(Color.LIGHT_GRAY); // Görsel ipucu: Gri (Pasif)
            }
        });
        // --- YENİ EKLENEN KISIM BİTİŞ ---ss
        JButton runButton = new JButton("Run Simulation");

        topPanel.add(new JLabel("Input File:"));
        topPanel.add(loadButton);
        topPanel.add(new JLabel("Algorithm:"));
        topPanel.add(algoSelector);
        topPanel.add(new JLabel("Time Quantum (RR):"));
        topPanel.add(quantumField);
        topPanel.add(runButton);

        add(topPanel, BorderLayout.NORTH);

        // Center Split: Input Text vs Results
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Input Preview
        inputArea = new JTextArea(8, 40);
        inputArea.setEditable(false);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("Input File Content"));

        // Results Table
        String[] columns = {"ID", "Arrival", "Burst", "Priority", "Finish", "Turnaround", "Waiting"};
        tableModel = new DefaultTableModel(columns, 0);
        resultTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Simulation Metrics"));

        // Bottom Stats Panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        avgWaitLabel = new JLabel("Avg Waiting: 0.0");
        avgTurnLabel = new JLabel("Avg Turnaround: 0.0");
        utilLabel = new JLabel("CPU Utilization: 0%");
        statsPanel.add(avgTurnLabel);
        statsPanel.add(avgWaitLabel);
        statsPanel.add(utilLabel);

        JPanel centerBottomContainer = new JPanel(new BorderLayout());
        centerBottomContainer.add(tableScroll, BorderLayout.CENTER);
        centerBottomContainer.add(statsPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(inputScroll);
        splitPane.setBottomComponent(centerBottomContainer);
        splitPane.setDividerLocation(150);

        add(splitPane, BorderLayout.CENTER);

        // Bottom: Gantt Chart
        ganttPanel = new GanttPanel();
        ganttPanel.setPreferredSize(new Dimension(1000, 150));
        ganttPanel.setBorder(BorderFactory.createTitledBorder("Gantt Chart Visualization"));
        add(ganttPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---
        loadButton.addActionListener(e -> loadFile());
        runButton.addActionListener(e -> runSimulation());
    }

    private void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        // Set default directory to current folder for ease
        fileChooser.setCurrentDirectory(new File("."));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadedProcesses.clear();
            inputArea.setText("");
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    inputArea.append(line + "\n");
                    String[] parts = line.split(",");
                    String id = parts[0].trim();
                    int arr = Integer.parseInt(parts[1].trim());
                    int burst = Integer.parseInt(parts[2].trim());
                    int prio = Integer.parseInt(parts[3].trim());
                    loadedProcesses.add(new Process(id, arr, burst, prio));
                }
                JOptionPane.showMessageDialog(this, "Loaded " + loadedProcesses.size() + " processes.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage());
            }
        }
    }

    private void runSimulation() {
        if (loadedProcesses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load a file first.");
            return;
        }

        // Create a fresh working copy of processes
        List<Process> workList = new ArrayList<>();
        for (Process p : loadedProcesses) workList.add(p.copy());

        String algo = (String) algoSelector.getSelectedItem();
        List<GanttBlock> ganttData = new ArrayList<>();

        try {
            if (algo.startsWith("FCFS")) {
                runFCFS(workList, ganttData);
            } else if (algo.startsWith("SJF")) {
                runSJF(workList, ganttData);
            } else if (algo.startsWith("Priority")) {
                runPriority(workList, ganttData);
            } else if (algo.startsWith("Round")) {
                int tq = Integer.parseInt(quantumField.getText().trim());
                runRR(workList, tq, ganttData);
            }

            updateResults(workList, ganttData);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Time Quantum.");
        }
    }

    // --- ALGORITHMS ---

    private void runFCFS(List<Process> processes, List<GanttBlock> gantt) {
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime));
        int currentTime = 0;

        for (Process p : processes) {
            if (currentTime < p.arrivalTime) {
                // IDLE
                gantt.add(new GanttBlock("IDLE", currentTime, p.arrivalTime));
                currentTime = p.arrivalTime;
            }
            int start = currentTime;
            currentTime += p.burstTime;
            p.finishTime = currentTime;
            gantt.add(new GanttBlock(p.id, start, currentTime));
        }
    }

    private void runSJF(List<Process> processes, List<GanttBlock> gantt) {
        int currentTime = 0;
        int completed = 0;
        int n = processes.size();
        Set<String> completedSet = new HashSet<>();

        while (completed < n) {
            Process selected = null;
            // Find available processes
            List<Process> available = new ArrayList<>();
            for (Process p : processes) {
                if (p.arrivalTime <= currentTime && !completedSet.contains(p.id)) {
                    available.add(p);
                }
            }

            if (available.isEmpty()) {
                gantt.add(new GanttBlock("IDLE", currentTime, currentTime + 1));
                currentTime++;
            } else {
                // Sort by Burst, then Arrival (FCFS tie break)
                available.sort((p1, p2) -> {
                    if (p1.burstTime != p2.burstTime) return Integer.compare(p1.burstTime, p2.burstTime);
                    return Integer.compare(p1.arrivalTime, p2.arrivalTime);
                });
                selected = available.get(0);

                int start = currentTime;
                currentTime += selected.burstTime;
                selected.finishTime = currentTime;
                completedSet.add(selected.id);
                completed++;
                gantt.add(new GanttBlock(selected.id, start, currentTime));
            }
        }
    }

    private void runPriority(List<Process> processes, List<GanttBlock> gantt) {
        int currentTime = 0;
        int completed = 0;
        int n = processes.size();
        Set<String> completedSet = new HashSet<>();

        while (completed < n) {
            List<Process> available = new ArrayList<>();
            for (Process p : processes) {
                if (p.arrivalTime <= currentTime && !completedSet.contains(p.id)) {
                    available.add(p);
                }
            }

            if (available.isEmpty()) {
                gantt.add(new GanttBlock("IDLE", currentTime, currentTime + 1));
                currentTime++;
            } else {
                // Sort by Priority (Lower is higher), then Arrival
                available.sort((p1, p2) -> {
                    if (p1.priority != p2.priority) return Integer.compare(p1.priority, p2.priority);
                    return Integer.compare(p1.arrivalTime, p2.arrivalTime);
                });
                Process selected = available.get(0);

                int start = currentTime;
                currentTime += selected.burstTime;
                selected.finishTime = currentTime;
                completedSet.add(selected.id);
                completed++;
                gantt.add(new GanttBlock(selected.id, start, currentTime));
            }
        }
    }

    private void runRR(List<Process> processes, int quantum, List<GanttBlock> gantt) {
        processes.sort(Comparator.comparingInt(p -> p.arrivalTime)); // Initial sort for arrival

        Queue<Process> queue = new LinkedList<>();
        int currentTime = 0;
        int completed = 0;
        int n = processes.size();
        int processIndex = 0;

        // Push first process(es)
        while(processIndex < n && processes.get(processIndex).arrivalTime <= currentTime) {
            queue.add(processes.get(processIndex));
            processIndex++;
        }

        while(completed < n) {
            if (queue.isEmpty()) {
                // If queue is empty but more processes to come
                if (processIndex < n) {
                    // IDLE until next arrival
                    int nextArrival = processes.get(processIndex).arrivalTime;
                    gantt.add(new GanttBlock("IDLE", currentTime, nextArrival));
                    currentTime = nextArrival;
                    while(processIndex < n && processes.get(processIndex).arrivalTime <= currentTime) {
                        queue.add(processes.get(processIndex));
                        processIndex++;
                    }
                }
                continue;
            }

            Process current = queue.poll();
            int runTime = Math.min(current.remainingTime, quantum);

            gantt.add(new GanttBlock(current.id, currentTime, currentTime + runTime));

            currentTime += runTime;
            current.remainingTime -= runTime;

            // CRITICAL RR LOGIC: Check for new arrivals BEFORE re-adding current process
            while(processIndex < n && processes.get(processIndex).arrivalTime <= currentTime) {
                queue.add(processes.get(processIndex));
                processIndex++;
            }

            if (current.remainingTime > 0) {
                queue.add(current);
            } else {
                current.finishTime = currentTime;
                completed++;
            }
        }
    }

    // --- Metrics & UI Update ---

    private void updateResults(List<Process> processes, List<GanttBlock> gantt) {
        tableModel.setRowCount(0);
        double totalWait = 0, totalTurn = 0, totalBurst = 0;
        int maxFinish = 0;

        // Sort by ID for clean table output
        processes.sort(Comparator.comparing(p -> p.id));

        for (Process p : processes) {
            p.turnaroundTime = p.finishTime - p.arrivalTime;
            p.waitingTime = p.turnaroundTime - p.burstTime;

            totalWait += p.waitingTime;
            totalTurn += p.turnaroundTime;
            totalBurst += p.burstTime;
            if(p.finishTime > maxFinish) maxFinish = p.finishTime;

            tableModel.addRow(new Object[]{
                    p.id, p.arrivalTime, p.burstTime, p.priority,
                    p.finishTime, p.turnaroundTime, p.waitingTime
            });
        }

        avgWaitLabel.setText(String.format("Avg Waiting: %.2f", totalWait / processes.size()));
        avgTurnLabel.setText(String.format("Avg Turnaround: %.2f", totalTurn / processes.size()));

        double util = (maxFinish > 0) ? (totalBurst / maxFinish) * 100.0 : 0.0;
        utilLabel.setText(String.format("CPU Utilization: %.2f%%", util));

        ganttPanel.setGanttData(gantt, maxFinish);
    }

    // --- Custom Gantt Panel ---
    class GanttPanel extends JPanel {
        private List<GanttBlock> blocks;
        private int totalTime;

        public void setGanttData(List<GanttBlock> blocks, int totalTime) {
            this.blocks = blocks;
            this.totalTime = totalTime;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks == null || blocks.isEmpty()) return;

            int h = getHeight();
            int w = getWidth();
            int barHeight = 40;
            int y = (h - barHeight) / 2;

            // Dynamically scale width
            double scale = (double) (w - 40) / totalTime;

            for (GanttBlock b : blocks) {
                int x = 20 + (int) (b.startTime * scale);
                int bw = (int) ((b.endTime - b.startTime) * scale);

                // Color logic
                if (b.processId.equals("IDLE")) g.setColor(Color.LIGHT_GRAY);
                else g.setColor(getColorForId(b.processId));

                g.fillRect(x, y, bw, barHeight);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, bw, barHeight);

                // Text centering
                String label = b.processId;
                FontMetrics fm = g.getFontMetrics();
                int tx = x + (bw - fm.stringWidth(label)) / 2;
                int ty = y + (barHeight + fm.getAscent()) / 2 - 2;
                g.drawString(label, tx, ty);

                // Time markers
                g.drawString(String.valueOf(b.startTime), x, y + barHeight + 15);
            }
            // Final time marker
            g.drawString(String.valueOf(totalTime), 20 + (int)(totalTime * scale), y + barHeight + 15);
        }

        private Color getColorForId(String id) {
            int hash = id.hashCode();
            return new Color((hash * 123) % 255, (hash * 456) % 255, (hash * 789) % 255);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ProcessScheduler().setVisible(true);
        });
    }
}
