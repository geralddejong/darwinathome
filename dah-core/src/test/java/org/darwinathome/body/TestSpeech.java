package org.darwinathome.body;

import org.junit.Assert;
import org.junit.Test;

/**
 * make sure that speech is splitting lines properly
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TestSpeech {


    @Test
    public void veryShort() {
        Speech speech = new Speech("harro");
        Assert.assertEquals(1, speech.getLines().size());
    }

    @Test
    public void veryLongWord() {
        Speech speech = new Speech("012345678-012345678-012345678-012345678-012345678-");
        Assert.assertEquals(1, speech.getLines().size());
    }

    @Test
    public void maxWord() {
        Speech speech = new Speech("012345678-012345678-012345678-012345678-012345678-012345678-");
        Assert.assertEquals(1, speech.getLines().size());
    }

    @Test
    public void tooLongWord() {
        Speech speech = new Speech("012345678-012345678-012345678-012345678-012345678-012345678-012345678-012345678-012345678-012345678-");
        Assert.assertEquals(2, speech.getLines().size());
        Assert.assertEquals("012345678-012345678-012345678-012345678-012345678-012345678-",speech.getLines().get(0));
        Assert.assertEquals("012345678-012345678-012345678-012345678-",speech.getLines().get(1));
    }

    @Test
    public void crazyLongWord() {
        Speech speech = new Speech(
                "012345678-012345678-012345678-012345678-012345678-012345678-" +
                        "012345678-012345678-012345678-012345678-012345678-012345678-" +
                        "012345678-012345678-012345678-012345678-012345678-012345678-" +
                        "012345678-012345678-"
        );
        Assert.assertEquals(3, speech.getLines().size());
    }

    @Test
    public void spacesWord() {
        Speech speech = new Speech("A12345678-B12345678-C12345678-D12345678-E12345678- F12345678-G12345678-H12345678-I12345678-J12345678-K12345678-L12345678-");
        Assert.assertEquals(3, speech.getLines().size());
        Assert.assertEquals("A12345678-B12345678-C12345678-D12345678-E12345678-",speech.getLines().get(0));
        Assert.assertEquals("F12345678-G12345678-H12345678-I12345678-J12345678-K12345678-",speech.getLines().get(1));
        Assert.assertEquals("L12345678-",speech.getLines().get(2));
    }

}
