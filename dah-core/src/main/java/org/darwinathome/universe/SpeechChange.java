// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.universe;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Somebody changes what their speech balloon says
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class SpeechChange {
    final long time;
    final String bodyName;
    final String speech;

    public SpeechChange(long time, String bodyName, String speech) {
        this.time = time;
        this.bodyName = bodyName;
        this.speech = speech;
    }

    public long getTime() {
        return time;
    }

    public String getBodyName() {
        return bodyName;
    }

    public String getSpeech() {
        return speech;
    }

    public static void write(DataOutputStream dataOutputStream, List<SpeechChange> list) throws IOException {
        dataOutputStream.writeInt(list.size());
        for (SpeechChange change : list) {
            dataOutputStream.writeLong(change.time);
            dataOutputStream.writeUTF(change.bodyName);
            dataOutputStream.writeUTF(change.speech);
        }
    }

    public static List<SpeechChange> read(DataInputStream dataInputStream) throws IOException {
        int size = dataInputStream.readInt();
        if (size < 0 || size > 1000) {
            throw new IOException("Size is crazy: "+size);
        }
        List<SpeechChange> list = new ArrayList<SpeechChange>(size);
        while (size-- > 0) {
            list.add(new SpeechChange(
                    dataInputStream.readLong(),
                    dataInputStream.readUTF(),
                    dataInputStream.readUTF()
            ));
        }
        return list;
    }
}
