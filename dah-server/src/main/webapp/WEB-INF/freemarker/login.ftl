<#include "header.ftl"/>
<#include "spring_form_macros.ftl"/>
<div id="left-col" class="grid_6">
    <img src="images/tetragotchi-world_bg_white.png"/>
</div>

<div id="right-col" class="grid_6">
    <h1>Enter The Tetragotchi Game</h1>
    <form id="loginForm" action='j_spring_security_check' method='POST'>

        <fieldset>

            <legend>Login</legend>
            
            <label>Email<br/>
            <input type='text' name='j_username' value='${email}' size='50'/></label><br/><br/>

            <label>Password<br/>
            <input type='password' name='j_password' size="50"/></label><br/><br/>

            <input name="submit_login" type="submit" value="Login" class="button"/>
            
        </fieldset>

        <p>Players admitted by invitation only. For questions, contact darwinathome@gmail.com.</p>

    </form>
</div>

<#include "footer.ftl"/>
