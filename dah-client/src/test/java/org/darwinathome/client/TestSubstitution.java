package org.darwinathome.client;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TestSubstitution {

    @Test
    public void testSub() {
        String url = "http://localhost:8080/dah/{session}/get-world.service";
        String session = "bababalaba";
        String subst = url.replace("{session}", session);
        Assert.assertEquals("no good", "http://localhost:8080/dah/"+session+"/get-world.service", subst);
    }
}
