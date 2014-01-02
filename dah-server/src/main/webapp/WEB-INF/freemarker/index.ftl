<#include "header.ftl"/>

<div id="left-col" class="grid_6">
    <img src="images/tetragotchi-world_bg_white.png"/>
</div>

<div id="right-col" class="grid_6">
    <h1>Welcome to the Tetragotchi Game</h1>
    <h2>${player.email}</h2>

    <p>
        To play the game you will have to download your own personal "launcher" called <strong>tetragotchi.jnlp</strong>,
        and if your computer is configured correctly with Java, the game will start up automatically and take you to
        the planet where your Tetragotchi will live.
    </p>

    <p>
        Click for your personal <a href="${player.email}/${player.password}/tetragotchi.jnlp">tetragotchi.jnlp</a>
        launch file.
    </p>

<#if player.beingOwner>

    <p>
        To invite someone else to create a tetragotchi and join in the game, enter their email address below:
    </p>

    <div>
        <form id="registrationForm" action='' method='POST'>
            <fieldset>
                <legend></legend>
                <label>Email
                    <input type='text' name='email' value=''>
                </label>
                <input name="submit_login" type="submit" value="Invite" class="button"/>
            </fieldset>
            <#if success>
                <p class="success">
                    An email has been sent to <span class="fg-gold">${email}</span>, thanks for spreading the word.
                </p>
            </#if>
            <#if failureFormat>
                <p class="failure">An error occurred. Please make sure your email is correctly formatted and try
                    again.</p>
            </#if>
            <#if failureExists>
                <p class="failure">This email is already registered.</p>
            </#if>
        </form>
    </div>
</#if>
<#if player.parent??>
    <#else>
        <div>
            <p>
                Advance the world by
                <a href="index.html?minutes=60">1 hour</a>,
                <a href="index.html?minutes=360">6 hours</a>.
            </p>
        </div>
</#if>

    <p>
        <a href="logout.html">Log out</a>.
    </p>
</div>

<#include "footer.ftl"/>

