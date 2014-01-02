// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import java.util.ArrayList;
import java.util.List;

/**
 * The sign that appears above the body
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Speech {
    private static final int MAX_SIGN_LINES = 3;
    private static final int MAX_SIGN_WIDTH = 60;
    private static final int MIN_SIGN_WIDTH = 40;
    private String text;
    private List<String> lines;

    public Speech(String text) {
        this.text = text;
        lines = makeLines(text);
    }

    private static List<String> makeLines(String text) {
        List<String> lines = new ArrayList<String>();
        loop: while (text.length() > MAX_SIGN_WIDTH) {
            for (int split = MAX_SIGN_WIDTH; split > MIN_SIGN_WIDTH; split--) {
                if (Character.isWhitespace(text.charAt(split))) {
                    lines.add(text.substring(0, split).trim());
                    text = text.substring(split).trim();
                    continue loop;
                }
            }
            lines.add(text.substring(0, MAX_SIGN_WIDTH).trim());
            text = text.substring(MAX_SIGN_WIDTH).trim();
        }
        lines.add(text);
        if (lines.size() > MAX_SIGN_LINES) {
            lines = lines.subList(0, MAX_SIGN_LINES);
        }
        return lines;
    }

    public String getText() {
        return text;
    }

    public List<String> getLines() {
        return lines;
    }
}
