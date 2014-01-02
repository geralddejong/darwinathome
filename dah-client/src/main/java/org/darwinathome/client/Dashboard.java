package org.darwinathome.client;

import org.darwinathome.body.Being;
import org.darwinathome.universe.World;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This dashboard gives players some Swing controls
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Dashboard extends JPanel {
    private BeingListModel beingListModel = new BeingListModel();

    public Dashboard(final Core core) {
        super(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Tetragotchis"));
        final JList beingList = new JList(beingListModel);
        beingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        beingList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    Being being = (Being) beingList.getSelectedValue();
                    core.setBeing(being);
                }
            }
        });
        JScrollPane scroll = new JScrollPane(beingList);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroll, BorderLayout.CENTER);
        core.addTimeListener(new Core.TimeListener() {
            @Override
            public void snapshot(World world) {
                beingListModel.setBeings(world.getBeings());
            }

            @Override
            public void beingSet(Being being) {
                // todo: maybe show stats
            }

            @Override
            public void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being) {
            }
        });
    }

    private class BeingListModel extends AbstractListModel {

        private List<Being> beings = new ArrayList<Being>();

        void setBeings(Collection<Being> beings) {
            if (!this.beings.isEmpty()) {
                int size = this.beings.size();
                this.beings.clear();
                fireIntervalRemoved(this, 0, size - 1);
            }
            this.beings.addAll(beings);
            if (!this.beings.isEmpty()) {
                fireIntervalAdded(this, 0, this.beings.size() - 1);
            }
        }

        @Override
        public int getSize() {
            return beings.size();
        }

        @Override
        public Object getElementAt(int index) {
            return beings.get(index);
        }
    }
}
