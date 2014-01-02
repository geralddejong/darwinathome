<#include "header.ftl"/>
<#include "spring_form_macros.ftl"/>

<div id="left-col" class="grid_6">
    <img src="images/tetragotchi-world_bg_white.png"/>
</div>

<div id="right-col" class="grid_6">
    <h1>Welcome to the Tetragotchi Game!</h1>

    <#if command.parent??>
        <h2>Choose a password for ${command.email}:</h2>

        <form id="regForm" action="register.html" method="post">
        <@spring.formHiddenInput "command.email" "${command.email}" />
        <@spring.formHiddenInput "command.parent" "${command.parent}" />
            <fieldset id="pt1">

                <label>Password</label><br/>
                <@spring.formPasswordInput "command.password"/><br/><br/>
                <label>Repeat Password </label><br/>
                <@spring.formPasswordInput "command.password2"/><br/><br/>
            </fieldset>
            <fieldset id="pt2">
                <legend></legend>
                <h3>&#160;</h3>
                <input type="submit" name="submit_registration" tabindex="6" value="Finish registration &raquo;"
                       class="button" style="width:14em;"/>
            </fieldset>
        </form>
    <#else>
        <h2>I'm sorry, but this invitation to Tetragotchi has expired.</h2>
    </#if>

</div>
<#include "footer.ftl"/>
