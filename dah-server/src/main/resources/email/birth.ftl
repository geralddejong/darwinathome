Hello ${being.name},

<#if being.makerName == "">
Congratulations, your Tetragotchi was born!  You are responsible for its survival, and in order to survive
it must drink water.  On the surface of the planet you will see triangles of different colors between blue and green.
Your tetragotchi will drink any blue water that it encounters until it is full.

Before you will be able to move, you must train your Tetragotchi using Natural Selection, but you should
be able to figure out how to do that from the instructions that the game provides.
<#else>
Your Tetragotchi was assimilated by ${being.makerName}, which means that it was destroyed but then reborn as a clone
of ${being.makerName}.

To play again, you can either click on your Tetragotchi icon or on a file called tetragotchi.jnlp somewhere on your
drive.  If you can't find either of these, you can login here again: http://tetragotchi.com/tetragotchi/login.html

We hope to see you back soon.
</#if>

Contact me if you have any trouble.
darwinathome@gmail.com
