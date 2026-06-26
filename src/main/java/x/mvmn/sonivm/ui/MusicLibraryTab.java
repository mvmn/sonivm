package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import x.mvmn.sonivm.ui.model.MusicLibraryTableModel;

public class MusicLibraryTab {
    private final JPanel panel;
    
    public MusicLibraryTab(MusicLibraryTableModel musicLibraryTableModel, SonivmUIController controller) {
        panel = new JPanel(new BorderLayout());
        
        JTable table = new JTable(musicLibraryTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Handle double-click to play track (this would be implemented in a real system)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    // In a real implementation, this would trigger playing the track
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
    }
    
    public JPanel getPanel() {
        return panel;
    }
}
